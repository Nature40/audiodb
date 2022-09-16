<template>
  
<div style="overflow: auto" class="fit">

<q-page class="flex flex-center" v-if="photo === undefined">
  No image selected on the browser page.
</q-page>

<q-page v-if="photo !== undefined" class="column wrap  items-center ">
    <div class="row items-center" style="padding-top: 5px; padding-bottom: 5px;">
      <q-btn :disable="!hasPrev" @click="move(-1)" icon="chevron_left" title="Move to previous image." :style="hasPrev ? {} : {color: 'grey'}"></q-btn>
      <span class="time-text" :class="{ 'expert-classified': isExpertClassified }"><span v-if="locationText !== undefined">{{locationText}}</span><span v-else>{{locationTextPrev}}</span> | {{dateText}}</span>
      <q-btn :disable="!hasNext" @click="move(+1)" icon="chevron_right" title="Move to next image." :style="hasNext ? {} : {color: 'grey'}"></q-btn>
      <q-select v-model="processing" :options="['original', 'lighten', 'lighten strong', 'darken', 'darken strong']" label="Processing" dense options-dense style="width: 200px;" rounded standout title="Change image exposure."/>
      <!--<q-select v-model="scaling" :options="['fast', 'high quality']" label="Scaling" dense options-dense style="width: 200px;" rounded standout/>-->
      <!--<q-checkbox size="xs" v-model="hideIncorrectBoxes" val="xs" label="hide incorrect boxes" />-->
      <q-btn-toggle
        v-model="show_box_mode"
        push
        glossy
        toggle-color="primary"
        :options="[
          {label: 'All', value: 'all'},
          {label: 'No incorrect', value: 'no_incorrect'},
          {label: 'None', value: 'none'}
        ]"
        title="Show all boxes or omit invalidated boxes or hide all boxes."
      />
      <q-toggle
        v-model="showCurrentDetectionOnly"
        :disable="show_box_mode === 'none'"
        icon="anchor"
        title="Show current selected detection box only."
      />
      <a :href="imageURL" target="_blank" title="Open current image at new tab to view details."><q-icon size="md" name="image_search" /></a>
    </div>
    <div style="position: relative;" class="" ref="imageDiv">
      <img :src="imageURL" :style="{'max-width': maxImageWidth + 'px', 'max-height': maxImageHeight + 'px'}" ref="image" @load="onLoadImage" @error="onErrorImage"/>
      <!--<q-img :src="imageURL" :style="{'max-width': maxImageWidth + 'px', 'max-height': maxImageHeight + 'px'}" ref="image" @load="onLoadImage" @error="onErrorImage"/>-->
      <canvas style="position: absolute; top: 0px; left: 0px;" ref="image_overlay" @mousedown="onMouseDownImage" @mousemove="onMouseMoveImage" @mouseup="onMouseUpImage" @mouseenter="onMouseEnterImage" @mouseleave="onMouseLeaveImage" @contextmenu="onContextMenuImage" title="Create new Box: Press and hold left mouse-button and move mouse. Discard box: Press right mouse-button."/>
      <q-spinner-gears color="primary" size="4em" v-show="imageLoading" style="position: absolute; top: 0px; right: 0px;"/>
      <span v-show="!imageLoading && imageError" style="color: red;">ERROR loading image.</span>
    </div>
    <div class="row items-center" style="padding-top: 5px; padding-bottom: 5px;" v-if="detections !== undefined && detections.length > 1 && selectedDetectionIndex !== undefined && userBox === undefined">
      Selected detection
      <q-btn :disable="!hasPrevDetection" @click="movePrevDetection" icon="arrow_left" :style="hasPrevDetection ? {} : {color: 'grey'}" title="Move to previous detection within this image."></q-btn>
      <span>{{selectedDetectionIndex + 1}}</span>
      <q-btn :disable="!hasNextDetection" @click="moveNextDetection" icon="arrow_right" :style="hasNextDetection ? {} : {color: 'grey'}" title="Move to next detection within this image,"></q-btn>
      of 
      {{detections.length}}
    </div>
    <div class="row"  style="padding-bottom: 5px;">
      <q-btn-toggle
        v-model="classificationSelectMode" 
        push
        glossy
        toggle-color="primary"
        :options="[
          {label: 'From List', value: 'list', attrs: {title: 'Select classification from predefined list of classifications.'}},
          {label: 'Custom', value: 'custom', attrs: {title: 'Type custom classification. Use only if classifications from the list of classifications are not suitable.'}},
        ]"
      />
      <q-select v-if="classificationSelectMode === 'list'"
        filled
        v-model="selectedClassification" 
        use-input
        hide-selected
        fill-input
        input-debounce="0"
        label="Classification"
        :options="classification_definitions_list_filtered" 
        @filter="classificationSelectionFilterFn"
        style="min-width: 200px;"
        :options-dense="true" 
        option-label="name"
        dense
        title="Select classification from predefined list of classifications."
      >
        <template v-slot:option="scope">
          <q-item v-bind="scope.itemProps" v-on="scope.itemEvents">
            <q-item-section>
              <q-item-label><b>{{scope.opt.name}}</b> {{scope.opt.description !== '' ? ' - ' + scope.opt.description : ''}}</q-item-label>
            </q-item-section>
          </q-item>
        </template>
        <template v-slot:no-option>
          <q-item>
            <q-item-section class="text-grey">
              No results
            </q-item-section>
          </q-item>
        </template>        
      </q-select>
      <q-input 
        v-if="classificationSelectMode === 'custom'" 
        filled 
        v-model="customClassificationText" 
        label="Custom Classification" 
        stack-label 
        dense 
        style="min-width: 200px;"
        placeholder="name"
        title="Type custom classification. Use only if classifications from the list of classifications are not suitable." 
      />
      <q-btn icon="where_to_vote" title="Store selected classification." round @click="onSubmitClassification(undefined)" text-color="green" />
      <q-btn v-show="userBox === undefined" icon="close_fullscreen" title="Store as misaligned box - too large or too small box." round @click="onSubmitClassification('Misaligned box')" text-color="red" />
      <q-btn v-show="userBox === undefined" icon="minimize" title="Store as empty box - no animal shown in box." round @click="onSubmitClassification('Empty box')" text-color="red" />
      <span style="color: green;" v-show="userBox !== undefined">
        (new box and classification) <q-btn @click="userBox = undefined;" style="color: red; height: 40px;" title="Discard new box and classification.">x</q-btn>
      </span>
      <!--<span style="padding-left: 200px;"></span>
      <q-btn style="color: red;" icon="remove_done" title="Set image as empty. (no animals visible)" @click="onSubmitNoAnimals">no animals</q-btn>-->
    </div>
    <div v-if="userBox === undefined && selectedDetection !== undefined">
      <table class="blueTable">
        <thead>
          <tr>
            <th>classification</th>
            <th>conf</th>
            <th>classificator</th>
            <th>identity</th>
            <th>date</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(entry) in selectedDetection.classifications" 
          :key="JSON.stringify(entry)" 
          style="margin-right: 15px; color: #111191c4;" 
          :style="{'background-color': 'rgba(196, 196, 196, 0.57)'}">
            <td><b>{{entry.classification}}</b></td>
            <td>{{entry.conf}}</td>
            <td>{{entry.classificator}}</td>
            <td>{{entry.identity}}</td>            
            <td>{{entry.date}}</td>
          </tr>
        </tbody>
      </table>
    </div>    
</q-page>

<tags-dialog ref="tagsDialog"/>

</div>
  
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

import tagsDialog from '../components/tags-dialog'

function pad(number) {
  if (number < 10) {
    return '0' + number;
  }
  return number;
}

export default {
  name: 'viewer',

  components: {
    tagsDialog,
  },

  data: () => ({
    processing: 'original',
    scaling: 'fast',
    maxImageWidth: 640,
    maxImageHeight: 480,
    imageLoading: false,
    imageError: false,
    selectedDetectionIndex: undefined,
    selectedClassification: undefined,
    classification_definitions_list_filtered: [],
    userBox: undefined,
    classificationSelectMode: 'list',
    customClassificationText: undefined,
    /*hideIncorrectBoxes: true,*/
    show_box_mode: 'no_incorrect',
    locationTextPrev: '---',
    detectionsOfPhoto: undefined,
    lastSelectedDetectionIndexModeAll: undefined,
    lastSelectedDetectionIndexModeNoIncorrect: undefined,
    showCurrentDetectionOnly: false,
  }),  

  computed: {
    ...mapState({
      photo: state => state.photo.photo,
      photoMeta: state => state.photo.meta,
      classification_definitions: state => state.meta?.data?.classification_definitions,
    }),     
    ...mapGetters({
      api: 'api',
      hasPrev: 'photo/hasPrev',
      hasNext: 'photo/hasNext',
      apiPOST: 'apiPOST',      
    }),
    classification_definitions_list_server() {      
      return this.classification_definitions === undefined ? [] : this.classification_definitions;
    },
    classification_definitions_list() {
      if(this.classificationsInImage.length === 0) {      
        return this.classification_definitions_list_server;
      }
      let cs = new Set();
      this.classification_definitions_list_server.forEach(df => {
        cs.add(df.name);
      });
      let a = this.classification_definitions_list_server.slice();
      this.classificationsInImage.forEach(c => {
        if(!cs.has(c)) {
          a.push({name: c});
        }
      });
      return a;      
    },
    imageURL() {
      //return this.api('photodb2', 'photos', this.photo, 'image.jpg');
      //return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?width=1024&height=768';
      if(this.scaling === 'high quality') {
        if(this.processing === 'lighten') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=2&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;
        } else if(this.processing === 'lighten strong') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=3&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;
        } else if(this.processing === 'darken') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=0.75&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;  
        } else if(this.processing === 'darken strong') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=0.5&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;                   
        } else {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;
        }
      } else {
        if(this.processing === 'lighten') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=2';
        } else if(this.processing === 'lighten strong') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=3';
        } else if(this.processing === 'darken') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=0.75';          
        } else if(this.processing === 'darken strong') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=0.5';           
        } else {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg');
        }
      }
    },
    locationText() {
      if(this.photoMeta === undefined || this.photoMeta.data === undefined) {
        return undefined;
      }
      return this.photoMeta.data.location;
    },
    dateText() {
      if(this.photoMeta === undefined || this.photoMeta.date === undefined) {
        return "0000-00-00 00:00";
      }
      var date = this.photoMeta.date;
      if(isFinite(date.getUTCFullYear())) {     
        return date.getUTCFullYear() +
          '-' + pad(date.getUTCMonth() + 1) +
          '-' + pad(date.getUTCDate()) +
          ' ' + pad(date.getUTCHours()) +
          ':' + pad(date.getUTCMinutes()) +
          ':' + pad(date.getUTCSeconds());
      } else {
        return '0000' +
          '-' + '00' +
          '-' + '00' +
          ' ' + '00' +
          ':' + '00' +
          ':' + '00';
      }
    },
    detections() {
      if(this.photoMeta === undefined || this.photoMeta.data === undefined) {
        return undefined;
      }
      if(this.show_box_mode === 'no_incorrect') {
        return this.photoMeta.data.detections.filter(detection => {
          let classifications = detection.classifications;
          if(classifications === undefined || classifications.length < 1) {
            return true;
          } else {
            let classification = classifications[classifications.length - 1].classification;
            console.log(classification);
            return classification !== 'incorrect box' && classification !== 'Empty box' && classification !== 'Misaligned box';
          }          
        });
      } else if(this.show_box_mode === 'all') {        
        return this.photoMeta.data.detections;
      } else {
        return [];
      }
    },
    isExpertClassified() {
      if(this.photoMeta === undefined || this.photoMeta.data === undefined) {
        return false;
      }
      let hasExpertClassification = this.photoMeta.data.detections.some(detection => {
        let classifications = detection.classifications;
        if(!classifications) {
            return false;
        }
        console.log(JSON.stringify(classifications));
        let hasExpert = classifications.some(classification => {
          if(!classification.classificator) {
            return false;
          }
          return classification.classificator === 'Expert';
        });
        return hasExpert;
      });
      return hasExpertClassification;
    },
    selectedDetection() {
      if(this.detections === undefined || this.selectedDetectionIndex === undefined || this.selectedDetectionIndex >= this.detections.length || this.selectedDetectionIndex < 0) {
        return undefined;
      }
      return this.detections[this.selectedDetectionIndex];
    },
    presetClassification() {
      if(this.selectedDetection === undefined) {
        return undefined;
      }
      let classifications = this.selectedDetection.classifications;
      if(classifications === undefined || classifications.length < 1) {
        return undefined;
      }
      return classifications[classifications.length - 1].classification;
    },
    classificationsInImage() {
      if(this.detections === undefined) {
        return [];
      }
      let cs = new Set();
      this.detections.forEach(d => {
        if(d.classifications !== undefined) {
          d.classifications.forEach(c => {
            if(c.classification !== undefined) {
              cs.add(c.classification);
            }
          });
        }
      });
      return [...cs];
    },
    hasPrevDetection() {
      return this.detections !== undefined && this.detections.length > 0 && this.selectedDetectionIndex !== undefined && this.selectedDetectionIndex > 0;
    },
    hasNextDetection() {
      return this.detections !== undefined && this.detections.length > 0 && this.selectedDetectionIndex !== undefined && this.selectedDetectionIndex < this.detections.length - 1;
    },           
  },

  methods: {
    ...mapActions({
      move: 'photo/move',
      photoMetaRefresh: 'photo/meta/refresh',
    }),
    redrawImageOverlay() {
      this.$nextTick(() => {
        //console.log("draw");      
        var image = this.$refs.image;
        if(image === undefined) {
          return;
        }
        var width = image.width;
        var height = image.height;
        var canvas = this.$refs.image_overlay;
        canvas.width = width;
        canvas.height = height;
        //console.log(width + "  " + height);
        var ctx = canvas.getContext("2d");
        ctx.fillStyle = "rgba(0, 0, 0, 0)";
        ctx.fillRect(0, 0, width, height);
        if(!this.imageLoading) {
          ctx.fillStyle = "rgba(255, 0, 0, 0.5)";
          ctx.lineWidth = 3;
          ctx.strokeStyle = "rgba(255, 0, 0, 0.5)";
          if(this.detections !== undefined && !this.showCurrentDetectionOnly) {
            this.detections.forEach((detection, index) => {
              if(detection.bbox !== undefined) {
                //console.log(detection.bbox);
                let xmin = detection.bbox[0] * width;
                let ymin = detection.bbox[1] * height;
                let boxwidth = detection.bbox[2] * width;
                let boxheight = detection.bbox[3] * height;
                if(this.userBox !== undefined || index !== this.selectedDetectionIndex) {
                ctx.strokeRect(xmin, ymin, boxwidth, boxheight);
                }
                //ctx.strokeRect(xmin, ymin, boxwidth, boxheight);
              }
            });
          }
          if(this.userBox === undefined && this.selectedDetection !== undefined) {
            let detection = this.selectedDetection;
            if(detection.bbox !== undefined) {
              //console.log(detection.bbox);
              let xmin = detection.bbox[0] * width;
              let ymin = detection.bbox[1] * height;
              let boxwidth = detection.bbox[2] * width;
              let boxheight = detection.bbox[3] * height;
              ctx.strokeStyle = "rgba(0, 255, 0, 0.5)";
              ctx.strokeRect(xmin, ymin, boxwidth, boxheight);
            }
          }
          if(this.userBox !== undefined) {
            let xmin = this.userBox[0] * width;
            let ymin = this.userBox[1] * height;
            let boxwidth = this.userBox[2] * width;
            let boxheight = this.userBox[3] * height;
            if(boxwidth > -5 && boxwidth < 5) {
              boxwidth = 5;
            }
            if(boxheight > -5 && boxheight < 5) {
              boxheight = 5;
            }
            ctx.setLineDash([6]);
            ctx.strokeStyle = "rgba(0, 255, 255, 0.5)";
            console.log("draw at: " + xmin + " " + ymin + "               " + width + "   " + height);
            ctx.strokeRect(xmin, ymin, boxwidth, boxheight);
          }
        }
      });
    },
    onLoadImage() {
      this.imageLoading = false;
      this.imageError = false;
      console.log("loaded");
      this.redrawImageOverlay();
      this.updateMaxImageDimensions();
    },
    onErrorImage() {
      this.imageLoading = false;
      this.imageError = true;
      console.log("error");
      this.redrawImageOverlay();
      this.updateMaxImageDimensions();
    },
    updateMaxImageDimensions() {
      //this.maxImageWidth = this.$refs.imageDiv.clientWidth - 20;
      //this.maxImageHeight = this.$refs.imageDiv.clientWidth - 20;
      this.maxImageWidth = document.body.clientWidth - 400;
      this.maxImageHeight = document.body.clientHeight - 250;
      if(this.maxImageWidth < 320) {
        this.maxImageWidth = 320;
      }
      if(this.maxImageHeight < 240) {
        this.maxImageHeight = 240;
      }
    },
    classificationSelectionFilterFn(val, update) {
      update(() => {
          if (val === '') {
            this.classification_definitions_list_filtered = this.classification_definitions_list;
          } else {
            const needle = val.toLowerCase();
            this.classification_definitions_list_filtered = this.classification_definitions_list.filter(v => v.name.toLowerCase().indexOf(needle) > -1);
          }
        }, ref => {
          if (val !== '' && ref.options.length > 0) {
            ref.setOptionIndex(-1)
            ref.moveOptionSelection(1, true)
          }
      });
    },
    async onSubmitClassification(optionalClassification) {
      if(this.photo !== undefined && (optionalClassification !== undefined || (this.classificationSelectMode === 'list' && this.selectedClassification !== undefined) || (this.classificationSelectMode === 'custom' && this.customClassificationText !== undefined && this.customClassificationText !== null && this.customClassificationText !== ''))) {
        var action = {action: 'set_classification'};
        if(optionalClassification !== undefined) {
          action.classification = optionalClassification;
        } else if(this.classificationSelectMode === 'list') {
          action.classification = this.selectedClassification.name;
        } else if(this.classificationSelectMode === 'custom') {
          action.classification = this.customClassificationText;
        }
        if(this.userBox !== undefined) {
          action.bbox = this.userBox;
        } else if(this.selectedDetection !== undefined && this.selectedDetection.bbox !== undefined) {
          action.bbox = this.selectedDetection.bbox;
        }
        var content = {actions: [action]}; 
        try {
          var response = await this.apiPOST(['photodb2', 'photos', this.photo], content);
          if(this.userBox !== undefined && this.detections !== undefined && this.detections.length > 0) {
            this.selectedDetectionIndex = this.detections.length - 1; // Move at least near to the new detection, the end of old detections list.
          }
          this.userBox = undefined;
          if(this.hasNextDetection) {
            if(this.show_box_mode === 'no_incorrect' && (action.classification === 'Misaligned box' || action.classification === 'Empty box')) {
              // nothing
            } else {
              this.moveNextDetection();
            }
          }
        } finally {
          this.photoMetaRefresh();
        }
      }
    },
    async onSubmitNoAnimals() {
      if(this.photo !== undefined) {
        try {
          var action = {action: 'set_classification'};
          action.classification = 'Empty box';
          action.bbox = [0, 0, 1, 1];
          var content = {actions: [action]};
          var response = await this.apiPOST(['photodb2', 'photos', this.photo], content);
          this.userBox = undefined;
          if(this.hasNextDetection) {
            this.moveNextDetection();
          }/* else if(this.hasNext){
            this.move(+1);
          }*/
        } finally {
          this.photoMetaRefresh();
        }
      }
    },
    movePrevDetection() {
      if(this.hasPrevDetection) {
        this.selectedDetectionIndex--;
        this.userBox = undefined;
      }
    },
    moveNextDetection() {
      if(this.hasNextDetection) {
        this.selectedDetectionIndex++;
        this.userBox = undefined;
      }
    },
    onMouseDownImage(e) {
      console.log('onMouseDownImage');
      if(e.buttons === 1)  { // left mouse button
        let image = this.$refs.image;
        if(image === undefined) {
          return;
        }
        let width = image.width;
        let height = image.height;
        let x = e.offsetX / width;
        let y = e.offsetY / height;
        console.log("click at: " + e.offsetX + " " + e.offsetY + "   " + x + " " + y + "               " + width + "   " + height);
        this.userBox = [x, y, 0, 0];
        this.redrawImageOverlay();
      }
      console.log(e);
    },
    onContextMenuImage(e) {
      e.preventDefault();
      this.userBox = undefined;
      this.redrawImageOverlay();
      console.log(e);
       return false;
    },
    onMouseMoveImage(e) {
      if(e.buttons === 1)  { // left mouse button
        if(this.userBox !== undefined) {
          console.log('onMouseMoveImage');
          let image = this.$refs.image;
          if(image === undefined) {
            return;
          }
          let width = image.width;
          let height = image.height;
          let px = this.userBox[0];
          let py = this.userBox[1];
          let mx = e.offsetX / width;
          //let my = (height - e.offsetY) / height;
          let my = e.offsetY / height;
          this.userBox = [px, py, (mx - px), (my - py)];
          this.redrawImageOverlay();
        }
      }
    },
    onMouseUpImage() {
      if(this.userBox !== undefined) {
      console.log('onMouseUpImage');
        if(this.userBox[2] === 0 || this.userBox[3] === 0) {
          this.userBox = undefined;
          this.redrawImageOverlay();
        }
      }
    },
    onMouseEnterImage() {
      console.log('onMouseEnterImage');
    },
    onMouseLeaveImage() {
      console.log('onMouseLeaveImage');
    },          
  },
  
  watch: {
    /*detections() {
      console.log('detections');
      console.log(this.detections);
      if(this.detectionsOfPhoto !== this.photo) {
        console.log('detections new');
        console.log(this.detections);
        this.detectionsOfPhoto = this.photo;
        if(this.detections !== undefined && this.detections.length > 0) {
          console.log('detections set 0');
          this.selectedDetectionIndex = 0;
        } else {
          this.selectedDetectionIndex = undefined;
        }
      }
      this.redrawImageOverlay();
    },*/
    detections() {
      this.$nextTick(() => {
        if(this.detections !== undefined) {
          console.log('detections');
          console.log(this.detections);
          console.log(this.detectionsOfPhoto);
          console.log(this.photo);
          if(this.detectionsOfPhoto === this.photo) {
            if(this.selectedDetectionIndex === undefined) {
              this.selectedDetectionIndex = 0;
            } 
            if(this.detections.length === 0) {
              this.selectedDetectionIndex = undefined;
            } else if(this.selectedDetectionIndex >= this.detections.length) {
              this.selectedDetectionIndex = this.detections.length - 1;
            }
          } else {
            this.detectionsOfPhoto = this.photo;
            this.selectedDetectionIndex = this.detections.length === 0 ? undefined : 0;
          }
        }
        this.redrawImageOverlay();
      });
    },
    selectedDetection() {
      this.userBox = undefined;
      this.redrawImageOverlay();
      if(this.presetClassification === undefined) {
        this.selectedClassification = undefined;
      }
      //let dfs = this.classification_definitions_list_filtered;
      let dfs = this.classification_definitions_list;
      let df = dfs.find(df => {
        return df.name === this.presetClassification;
      });
      this.selectedClassification = df;
    },
    imageURL() {
      this.lastSelectedDetectionIndexModeAll = undefined;
      this.lastSelectedDetectionIndexModeNoIncorrect = undefined;
      this.imageLoading = true;
      this.redrawImageOverlay();
    },
    userBox() {
      this.redrawImageOverlay();
    },
    photo() {
      //this.selectedDetectionIndex = undefined;
      this.redrawImageOverlay();
    },
    photoMeta() {
      this.redrawImageOverlay();
    },
    locationText() {
      if(this.locationText !== undefined) {
        this.locationTextPrev = '='.repeat(this.locationText.length);
      }
    },
    show_box_mode(newMode, oldMode) {
      const currentSelectedDetectionIndex = this.selectedDetectionIndex;

      if(newMode === 'all') {
        if(this.lastSelectedDetectionIndexModeAll !== undefined) {
          this.selectedDetectionIndex = this.lastSelectedDetectionIndexModeAll;
        }
      } else if(newMode === 'no_incorrect') {
        if(this.lastSelectedDetectionIndexModeAll !== undefined) {
          this.selectedDetectionIndex = this.lastSelectedDetectionIndexModeNoIncorrect;
        }
      } else {
        // nothing
      }

      if(oldMode === 'all') {
        this.lastSelectedDetectionIndexModeAll = currentSelectedDetectionIndex;
      } else if(oldMode === 'no_incorrect') {
        this.lastSelectedDetectionIndexModeNoIncorrect = currentSelectedDetectionIndex;
      } else {
        // nothing
      }      
    },
    showCurrentDetectionOnly() {
      this.redrawImageOverlay();
    },
  },

  async mounted() {
    this.redrawImageOverlay();
  },

  activated() {
    this.redrawImageOverlay();
  },

  deactivated() {
    this.redrawImageOverlay();
  },
}
</script>

<style scoped>
.time-text {
  font-family: "Lucida Console", Courier, monospace;
}

table.blueTable {
  border: 0px solid #1C6EA4;
  background-color: #EEEEEE;
  text-align: center;
  border-collapse: collapse;
}
table.blueTable td, table.blueTable th {
  border: 1px solid #BBBBBB;
  padding: 0px 8px;
}
table.blueTable tbody td {
  font-size: 13px;
  color: #333333;
}
table.blueTable td:nth-child(even) {
  background: #ffffffb0;
}
table.blueTable thead {
  background: linear-gradient(to bottom, #1976d2 0%, #11579d 66%, #134f8a 100%);
  border-bottom: 2px solid #444444;
}
table.blueTable thead th {
  font-size: 15px;
  font-weight: bold;
  color: #FFFFFFD6;
  text-align: center;
  border-left: 2px solid #D0E4F5;
}
table.blueTable thead th:first-child {
  border-left: none;
}

table.blueTable tfoot td {
  font-size: 14px;
}
table.blueTable tfoot .links {
  text-align: right;
}
table.blueTable tfoot .links a{
  display: inline-block;
  background: #1C6EA4;
  color: #FFFFFF;
  padding: 2px 8px;
  border-radius: 5px;
}

.expert-classified {
  color: #7AC124;;
}

</style>
