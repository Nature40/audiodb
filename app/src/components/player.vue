<template>
<v-container>
  <div class="send-overlay" v-show="sendMessage !== undefined">
    {{sendMessage}}
  </div>
  <div style="position: absolute; left: 100px;">
    {{(currentTimeAudio / audioTimeFactor).toFixed(3)}} / {{(duration / audioTimeFactor).toFixed(3)}} 
  </div>
  <div style="position: absolute; right: 0px;">
    <audio-meta :sample="sample"/>
    <player-settings />
    <label-definitions @onDialog="labelDefinitionsDialogOpen = $event" />
    <div class="send-error" v-show="sendMessageError !== undefined">
      <v-icon color="red">warning</v-icon>
      {{sendMessageError}}
      <br>{{sendMessageErrorReason}}
    </div>  
  </div>
  <div v-show="!imageLoaded" style="position: absolute; background-color: lightgrey; padding: 100px; margin: 100px; margin-left: 400px;; border-style: solid;">    
    <h1>loading ...</h1>
    <ring-loader :loading="!imageLoaded" color="#000000" size="50px" />
  </div>
  <div v-show="imageError && imageLoaded" style="position: absolute; background-color: lightgrey; padding: 100px; margin: 100px; margin-left: 400px;; border-style: solid;">    
    <h1>Error loading visualisation</h1>
  </div>
  <v-layout text-xs-center wrap>
    <v-flex xs12 mb-5 >
      <audio id="player" :src="audioUrl" type="audio/wav" controls preload="auto">
      </audio>
      <br>
      <div style="display: inline-block; ">
              <v-slider
        v-model="currentTimeUser"
        :min="0"
        :max="duration"
        :step="0"
        style="margin-bottom: -25px; margin-top: -10px;"
        :style="{ width: canvasWidth + 'px' }"        
      ></v-slider>
      <canvas id="canvas" :class="{ 'semi_transparent': (!imageLoaded) }" :width="canvasWidth" :height="canvasHeight" style="background-color: #d1d1d1;" @mousedown="startDrag" @mousemove="dragMove" class="spectrogram" />
      <img id="image" :src="spectrumUrl" style="width: 1800px; display: none;" />
      <br>

        <multiselect 
          v-model="selectedLabelNames" 
          :options="mergedLabelNames" 
          style="max-width: 500px; display: inline-block; vertical-align: top;" 
          placeholder="Search or add label"
          tagPlaceholder="Press ENTER to add this as new label" 
          :allowEmpty="true"
          :taggable="true"
          :multiple="true"
          @tag="addLabelName"
          @open="selectLabel = true"
          @close="selectLabel = false"
          :limit="1"
          :limitText="count => `and ${count} more`"
          :class="{ 'hide': (labelEndTime === undefined) }"
        />

        <div style="position: relative; display: inline-block; vertical-align: top;">
          <v-btn small round color="primary" class="hide" >start label</v-btn>
          <v-btn @click="onLabelStart" small round color="primary" v-show="labelStartTime === undefined" title="create new label that starts at current audio position" style="position: absolute; top: 0px; left: 0px;"><v-icon>flight_takeoff</v-icon> start label</v-btn>
          <v-btn @click="onLabelEnd" :disabled="labelStartTime === undefined || currentTimeAudio === undefined || (labelStartTime * this.audioTimeFactor) === currentTimeAudio" small round color="primary" v-show="labelStartTime !== undefined && labelEndTime === undefined" title="end current label at current audio position"  style="position: absolute; top: 0px; left: 0px;"><v-icon>flight_land</v-icon> end label</v-btn>
        </div>
        <v-text-field v-model="labelComment" placeholder="comment" class="input-comment" :class="{ 'hide': (labelEndTime === undefined) }" style="vertical-align: top;"></v-text-field>        
        <v-btn @click="onLabelPlay" small round color="primary" :class="{ 'hide': (labelEndTime === undefined) }" title="play just current selection" style="vertical-align: top;"><v-icon>play_arrow</v-icon> play selection</v-btn>
        <v-btn @click="onLabelSaveAndNext" small round color="green" :class="{ 'hide': (!hasNextSelection || isReadOnly) }" title="save current label and select next label from the list" style="vertical-align: top;"><v-icon>done</v-icon> save and go to next selection</v-btn>
        <v-btn @click="onLabelSave()" small round color="green" v-show="labelEndTime !== undefined  && !isReadOnly" title="store current label" style="vertical-align: top;"><v-icon>push_pin</v-icon> save</v-btn>
        <v-btn @click="onLabelDiscard" small round color="red" :class="{ 'hide': (labelStartTime === undefined) }" title="remove current label" style="vertical-align: top;"><v-icon>power_off</v-icon> discard</v-btn>

      </div>
      <div v-if="selectedLabelEntry !== undefined && selectedLabelEntry.generated_labels !== undefined && selectedLabelEntry.generated_labels.length > 0" style="margin-bottom: 20px;">
        Generated labels: <span :class="{'button-generated-label': !isSelectedLabelName(generatorLabel.name), 'button-generated-label-selected': isSelectedLabelName(generatorLabel.name)}" v-for="(generatorLabel, i) in selectedLabelEntry.generated_labels" :key="i" @click="addLabelName(generatorLabel.name)">{{generatorLabel.name}} ({{generatorLabel.reliability.toFixed(1)}})</span>
        <br>(Click on a generated label to set it as verifed label.)
      </div>
      press <b>[SPACE]</b> key to <b>play</b> / <b>pause</b> audio
      <br>
      press <b>[ENTER]</b> key to <b :class="{active: labelStartTime === undefined}">mark start</b> / <b :class="{active: labelStartTime !== undefined && labelEndTime === undefined}">mark end</b> / <b :class="{active: labelStartTime !== undefined && labelEndTime !== undefined && !selectLabel}">save label</b>
      <br>
      press <b>[LEFT/RIGTH ARROW]</b> key to move <b>back/forward</b> in time
      <br>
      on spectrogram hold <b>[left mouse button]</b> and <b>[move mouse left/right]</b> to move in time <b></b>


      <br>
      <br>
      <table class="table-labels">
      <tr>
        <th>remove</th>
        <th>start</th>
        <th>end</th>
        <th>generated</th>
        <th>label</th>
        <th>move to</th>
        <th>comment</th>
      </tr>
      <tbody>
      <tr v-for="(label, index) in labels" :key="index" :class="{'selected-label-entry': (selectedLabelEntry === label)}">
        <td><v-btn icon title="remove label"><v-icon @click="onLabelRemove(index)">delete_forever</v-icon></v-btn></td>
        <td>{{label.start.toFixed(3)}}</td>
        <td>{{label.end.toFixed(3)}}</td>
        <td><span v-for="(generatorLabel, i) in label.generated_labels" :key="i" class="label-name">{{generatorLabel.name}}</span></td>
        <td><span v-for="(userLabel, i) in label.labels" :key="i" class="label-name">{{userLabel.name}}</span></td>
        <td>
          <v-btn icon title="move to label start">
            <v-icon @click="onSelectLabelEntry(label, index)" v-if="selectedLabelEntry !== label">redo</v-icon>
            <v-icon @click="onSelectLabelEntry(label, index)" v-if="selectedLabelEntry === label">details</v-icon>
          </v-btn>
        </td>
        <td>{{label.comment}}</td>
      </tr>
      <tr v-if="labels.length === 0">
        ---- (no labels) ----
      </tr>
      </tbody>
      </table>
    </v-flex>
  </v-layout>
   
</v-container>
</template>

<script>

import axios from 'axios'
import YAML from 'yaml'
import { mapState, mapGetters, mapActions } from 'vuex'

import audioMeta from './audio-meta'
import playerSettings from './player-settings'
import labelDefinitions from './label-definitions'

var refPlayer = undefined;

export default {
props: ['sample'],
components: {
  audioMeta,
  playerSettings,
  labelDefinitions,
},
data: () => ({
  currentTimeAudio: 0,      
  currentTimeUser: 0,
  duration: 0,
  frequencyData: [],
  sampleRate: 10000,
  canvasWidth: 1024,
  canvasHeight: 512,
  imageLoaded: false,
  imageError: false,
  dragStartX: undefined,
  secondsPerColumn: undefined,
  labelStartTime: undefined,
  labelEndTime: undefined,
  labelComment: "",
  selectedLabelNames: [],
  customLabelNames: [],
  labels: [],
  selectLabel: false,
  labelDefinitionsDialogOpen: false,
  sendMessage: undefined,
  sendMessageError: undefined,
  sendMessageErrorReason: undefined,
  audio: undefined,
  renderThis: undefined,
  timerID: undefined,
  animationFrameID: undefined,
  playSection: false,
  selectedLabelEntry: undefined,
  selectedLabelEntryIndex: -1,
  meta: undefined,
}),
computed: {
  ...mapState({
    apiBase: 'apiBase',
    label_definitions: state => state.label_definitions === undefined ? undefined : state.label_definitions.data,
    threshold: state => state.settings.player_spectrum_threshold,
    playbackRate: state => state.settings.player_playbackRate,
    preservesPitch: state => state.settings.player_preservesPitch,
    overwriteSamplingRate: state => state.settings.player_overwriteSamplingRate,
    samplingRate: state => state.settings.player_samplingRate,
  }),
  ...mapGetters({
    isReadOnly: 'identity/isReadOnly',    
  }),  
  mergedLabelNames() {
    var names = this.customLabelNames.slice();
    if(this.label_definitions !== undefined) {
      this.label_definitions.forEach(d => names.push(d.name));
    }
    return names;
  },
  shortcutsBlocked() {
    return this.labelDefinitionsDialogOpen;
  },
  spectrumUrl() {
    return this.apiBase + 'samples/' + this.sample.id + '/spectrum' + '?cutoff=' + this.canvasHeight + "&threshold=" + this.threshold;
  },
  audioUrl() {
    if(this.overwriteSamplingRate && this.samplingRate !== undefined) {
      return this.apiBase + 'samples/' + this.sample.id + '/data' + '?overwrite_sampling_rate=' + this.samplingRate;
    } else {
      return this.apiBase + 'samples/' + this.sample.id + '/data';      
    }
  },
  hasNextSelection() {
    if(this.selectedLabelEntryIndex < 0) {
      return false;
    }
    return this.labels !== undefined && this.selectedLabelEntryIndex + 1 < this.labels.length;
  },
  audioTimeFactor() {
    if(!this.overwriteSamplingRate || this.samplingRate === undefined || this.meta === undefined || this.meta.SampleRate === undefined) {
      return 1;
    }
    console.log(this.meta.SampleRate);
    return this.meta.SampleRate / this.samplingRate;
  }
},  
methods: {
  ...mapActions({
    label_definitions_init: 'label_definitions/init',
    label_definitions_refresh: 'label_definitions/refresh',
  }),  
  startDrag(e) {        
    this.dragStartX = e.pageX;
  },
  dragMove(e) {
    this.requestRender();
    if(this.dragStartX !== undefined) {
      if(e.buttons == 1) { // left mouse button
        var offsetX = e.pageX - this.dragStartX;
        this.currentTimeUser -= offsetX * this.secondsPerColumn;
        this.dragStartX = e.pageX;
      } else {
        this.dragStartX = undefined;
      }
    }
  },
  onLabelStart() {
    this.labelStartTime = (this.currentTimeAudio / this.audioTimeFactor);
  },
  onLabelEnd() {    
    this.labelEndTime = (this.currentTimeAudio / this.audioTimeFactor);
    this.audio.pause();
  },
  onLabelDiscard() {
    this.labelStartTime = undefined;
    this.labelComment = "";
    this.labelEndTime = undefined;
    this.selectedLabelEntry = undefined;
    this.selectedLabelEntryIndex = -1;
  },  
  onLabelSave(funcSuccess) {
    var label = this.selectedLabelEntry === undefined ? {start: this.labelStartTime, end: this.labelEndTime, labels: [], generated_labels: []} : this.selectedLabelEntry;
    var userLabels = this.selectedLabelNames.map(v => {
      var e = label.labels.find(a => a === v);
      if( e === undefined) {
        e = {name: v};
      }
      return e;
    });
    label.labels = userLabels; 
    label.comment = this.labelComment;
    if(this.selectedLabelEntry === undefined) {
      this.postAddLabel(label, funcSuccess);
    } else {
      this.postReplaceLabel(label, funcSuccess);
    }    
  },
  onLabelSaveAndNext() {
    var index = this.selectedLabelEntryIndex + 1;
      this.onLabelSave(() => {
        if(this.labels !== undefined && index < this.labels.length)
        var label = this.labels[index];
        this.onSelectLabelEntry(label, index);
      });
  },
  onLabelPlay() {
    this.audio.pause();
    this.currentTimeUser = (this.labelStartTime * this.audioTimeFactor);
    this.audio.play();
    this.playSection = true;
  },
  addLabelName(labelText) {
    if(!this.mergedLabelNames.includes(labelText)) {
      this.customLabelNames.push(labelText);
    }
    if(!this.selectedLabelNames.includes(labelText)) {
      this.selectedLabelNames.push(labelText);
    }
  },
  postAddLabel(label, funcSuccess) {
    this.sendMessage = "send: add label";
    this.sendMessageError = undefined;
    axios.post(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels', {actions: [{action: "add_label", label: label}]})
    .then((response) => {
      this.sendMessage = undefined;
      this.sendMessageError = undefined;
      this.labelStartTime = undefined;
      this.labelEndTime = undefined;
      this.labelComment = "";
      this.labels = response.data.labels;
      this.selectedLabelEntry = undefined;
      this.selectedLabelEntryIndex = -1;
      if(funcSuccess !== undefined) {
        funcSuccess();
      }
    })
    .catch(() => {
      this.sendMessage = undefined;
      this.sendMessageError = "could not send: add label. You may tray again.";
    });
  },
  postReplaceLabel(label, funcSuccess) {
    this.sendMessage = "send: add label";
    this.sendMessageError = undefined;
    axios.post(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels', {actions: [{action: "replace_label", label: label}]})
    .then((response) => {
      this.sendMessage = undefined;
      this.sendMessageError = undefined;
      this.labelStartTime = undefined;
      this.labelEndTime = undefined;
      this.labelComment = "";
      this.labels = response.data.labels;
      this.selectedLabelEntry = undefined;
      this.selectedLabelEntryIndex = -1;
      if(funcSuccess !== undefined) {
        funcSuccess();
      }      
    })
    .catch(() => {
      this.sendMessage = undefined;
      this.sendMessageError = "could not send: replace label. You may tray again.";
    });
  },
  refreshLabels() {
    console.log("refreshLabels");
    axios.get(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels')
    .then(response => {
      this.labels = response.data.labels;
    })
    .catch(() => {
      this.labels = [];
    });
  },
  refeshMeta() {
    this.meta = undefined;
    axios.get(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'meta')
    .then(response => {
      var data = response.data;
      var parsed = YAML.parse(data);
      this.meta = parsed.meta;
    })
    .catch(() => {
      this.meta = undefined;
    });
  },
  onLabelRemove(index) {
    //this.labels.splice(index, 1);
    this.postRemoveLabel(this.labels[index]);
  },
  postRemoveLabel(label) {
    this.sendMessage = "send: remove label";
    this.sendMessageError = undefined;
    axios.post(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels', {actions: [{action: "remove_label", label: label}]})
    .then((response) => {
      this.sendMessage = undefined;
      this.sendMessageError = undefined;
      this.labelStartTime = undefined;
      this.labelEndTime = undefined;
      this.labelComment = "";
      this.labels = response.data.labels;
    })
    .catch((error) => {
      this.sendMessage = undefined;
      console.log(error.response);
      this.sendMessageError = "could not send: remove label. You may tray again.";
      this.sendMessageErrorReason = error === undefined ? "unkown reason" : error.response === undefined ? "unkown reason" : error.response.data === undefined ? "unkown reason" : error.response.data; 
    });
  },
  onWindowResize() {
    this.canvasWidth = document.body.clientWidth - 200;
  },
  render() {
    //console.log(this.audio.paused);
    this.timerID = undefined;
    this.animationFrameID = undefined;
    if(this.audio.paused) {
      this.timerID = setTimeout(this.renderThis, 1000);
    } else {
      this.animationFrameID = requestAnimationFrame(this.renderThis);
    }

    this.imageLoaded = this.image.complete;
    var currentTime = this.audio.currentTime;
    if(isNaN(currentTime)) {
      return;
    }
    if(this.playSection && this.labelEndTime !== undefined) {
      if((this.labelEndTime * this.audioTimeFactor) <= currentTime) {
        this.audio.pause();
        this.playSection = false;
        currentTime = (this.labelEndTime * this.audioTimeFactor);
        this.audio.currentTime = currentTime;
      }
    }
    var duration = this.audio.duration;
    if(isNaN(duration)) {
      this.duration = 0;
      return;
    }
    //console.log("render currentTime: " + currentTime);
    this.currentTimeAudio = currentTime;
    this.currentTimeUser = currentTime;
    this.duration = duration;
    var canvasNowColumn = this.canvasWidth / 2;
    var columnsPerSecond = this.image.naturalWidth / duration;
    this.secondsPerColumn = duration / this.image.naturalWidth;
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    //ctx.drawImage(image, canvasNowColumn - (currentTime*collumnsPerSecond), 0);
    //ctx.drawImage(image, (currentTime*collumnsPerSecond) - canvasNowColumn, 0 , 1000, 256, 0, 0, 1000, 256);
    var sx = (currentTime*columnsPerSecond) - canvasNowColumn;
    var sy = 0;
    var sWidth = this.canvasWidth;
    var sHeight = this.canvasHeight;
    var dx = 0;
    var dy = 0;
    var dWidth = sWidth;
    var dHeight = sHeight;
    this.ctx.drawImage(this.image, sx, sy, sWidth, sHeight, dx, dy, dWidth, dHeight);

    if(this.labelStartTime !== undefined) {
      var labelXStart = canvasNowColumn - (currentTime - (this.labelStartTime * this.audioTimeFactor))*columnsPerSecond;
      var endTime = this.labelEndTime === undefined ? currentTime : (this.labelEndTime * this.audioTimeFactor);
      var labelXEnd = canvasNowColumn - (currentTime - endTime)*columnsPerSecond; 
      this.ctx.fillStyle = "rgba(255,255,0,0.5)";
      this.ctx.fillRect(labelXStart, 0, labelXEnd - labelXStart, this.canvasHeight);
    }

    this.ctx.lineWidth = 5;
    this.ctx.strokeStyle = 'rgba(0,255,0,0.9)';
    this.ctx.beginPath();
    for (let i = 0; i < this.labels.length; i++) {
      let label = this.labels[i];
      let xStart = canvasNowColumn - (currentTime - (label.start * this.audioTimeFactor)) * columnsPerSecond;
      let xEnd = canvasNowColumn - (currentTime - (label.end * this.audioTimeFactor)) * columnsPerSecond;
      //console.log("range " + xStart + "  " + xEnd + JSON.stringify(label) +" OK "); 
      this.ctx.moveTo(xStart, this.canvasHeight - 3);
      this.ctx.lineTo(xEnd, this.canvasHeight - 3);
    }
    this.ctx.stroke();

    this.ctx.fillStyle = 'rgba(255,255,255,1)';
    this.ctx.beginPath();
    for (let i = 0; i < this.labels.length; i++) {
      let label = this.labels[i];
      let x = canvasNowColumn - (currentTime - (label.end * this.audioTimeFactor)) * columnsPerSecond;
      let y = this.canvasHeight - 10;
      //console.log("range " + xStart + "  " + xEnd + JSON.stringify(label) +" OK "); 
      this.ctx.moveTo(x, y);
      this.ctx.lineTo(x - 7, y + 5);
      this.ctx.lineTo(x, y + 9);
      this.ctx.lineTo(x, y);
    }
    this.ctx.closePath();
    this.ctx.fill();

    this.ctx.fillStyle = 'rgba(255,255,255,1)';
    this.ctx.beginPath();
    for (let i = 0; i < this.labels.length; i++) {
      let label = this.labels[i];
      let x = canvasNowColumn - (currentTime - (label.start * this.audioTimeFactor)) * columnsPerSecond;
      let y = this.canvasHeight - 10;
      //console.log("range " + xStart + "  " + xEnd + JSON.stringify(label) +" OK "); 
      this.ctx.moveTo(x, y);
      this.ctx.lineTo(x + 7, y + 5);
      this.ctx.lineTo(x, y + 9);
      this.ctx.lineTo(x, y);
    }
    this.ctx.closePath();
    this.ctx.fill();
    

    this.ctx.beginPath();     
    this.ctx.moveTo(canvasNowColumn, 0); 
    this.ctx.lineTo(canvasNowColumn, this.canvasHeight); 
    this.ctx.lineWidth = 1;
    this.ctx.strokeStyle = 'rgba(255,0,0,0.9)';
    this.ctx.stroke();
    
    if(!this.imageLoaded) {
      this.ctx.font = '50px sans-serif';    
      this.ctx.fillText('Refreshing spectrogram ...', 100, 100);
    }
  },
  requestRender() {
    if(this.timerID !== undefined) {
      clearTimeout(this.timerID);
      this.timerID = undefined;
    }
    if(this.animationFrameID === undefined) {
      this.animationFrameID = requestAnimationFrame(this.renderThis);
    }
  },
  onSelectLabelEntry(label, index) {
    this.currentTimeUser = label.start * this.audioTimeFactor;
    if(this.selectedLabelEntry === label) {
      this.selectedLabelEntry = undefined;
      this.selectedLabelEntryIndex = -1;
    } else {
      this.selectedLabelEntry = label;
      this.selectedLabelEntryIndex = index;
      this.labelStartTime = this.selectedLabelEntry.start;
      this.labelEndTime = this.selectedLabelEntry.end;
      this.labelComment = this.selectedLabelEntry.comment;
      this.selectedLabelNames = [];
      this.selectedLabelEntry.labels.forEach(userLabel => this.addLabelName(userLabel.name));
    }
  },
  isSelectedLabelName(name) {
    return this.selectedLabelNames.includes(name);
  }
},
watch: {
  currentTimeUser() {
    if(this.currentTimeUser !== this.currentTimeAudio) {
      //console.log("user change " + this.currentTimeUser + "   " + this.duration);
      this.audio.currentTime = this.currentTimeUser;
      //console.log("set currentTimeUser " + this.currentTimeUser);
    }
  },
  sample: {
    immediate: true,
    handler() {
      this.sendMessage = undefined;
      this.sendMessageError = undefined;
      this.labelStartTime = undefined;
      this.labelEndTime = undefined;
      this.labelComment = "";
      this.selectedLabelEntry = undefined;
      this.selectedLabelEntryIndex = -1;
      this.refreshLabels();
      this.refeshMeta();
    }   
  },
  playbackRate() {
    this.audio.playbackRate = this.playbackRate;
    this.audio.defaultPlaybackRate = this.playbackRate;    
  },
  preservesPitch() {
    this.audio.preservesPitch = this.preservesPitch;
    this.audio.mozPreservesPitch = this.preservesPitch;
    this.audio.webkitPreservesPitch  = this.preservesPitch;
  },
},
created() {
  window.addEventListener("resize", this.onWindowResize.bind(this));
},
destroyed() {
  window.removeEventListener("resize", this.onWindowResize.bind(this));
},
mounted() {
  this.onWindowResize();
  this.refreshLabels();
  this.label_definitions_init();
  refPlayer = this;
  this.audio = document.getElementById('player');

  this.canvas = document.getElementById('canvas');
  this.ctx = this.canvas.getContext('2d');
  this.image = document.getElementById('image');
  this.image.onerror = () => {
    this.imageLoaded = true;
    this.imageError = true;
  }
  this.image.onload = () => {
    this.imageLoaded = true;
    this.imageError = false;
  }
  this.audio.addEventListener("progress", () => {
    console.log("progress");
    this.requestRender(); 
    }, true
  );
  this.audio.addEventListener("play", () => {
    console.log("play");
    this.requestRender();
    }, true
  );
  this.audio.addEventListener("playing", () => {
    console.log("playing");
    this.requestRender(); 
    }, true
  );
  this.audio.addEventListener("timeupdate", () => {
    //console.log("timeupdate");
    this.requestRender(); 
    }, true
  );

  this.renderThis = this.render.bind(this);
  this.timerID = setTimeout(this.renderThis, 500);

  var someKeysAreDown = false;
  
  window.onkeyup = () => {
    someKeysAreDown = false;
  };

  window.onkeydown = (e) => {
    if(someKeysAreDown) {
      return;
    }
    someKeysAreDown = true;
    if(refPlayer.shortcutsBlocked) {
      return;
    }
    //console.log(e.key);
    if(e.key === ' ') {
      if(this.audio.paused) {
        this.audio.play();
      } else {
        this.audio.pause();
      }
      if (e.target == document.body) {
        e.preventDefault();
      }          
    } else if (e.key === 'Enter' && refPlayer !== undefined) {
      if(refPlayer.labelStartTime === undefined) {
        refPlayer.onLabelStart();
      } else if(refPlayer.labelStartTime !== undefined && refPlayer.labelEndTime === undefined) {
        if((refPlayer.labelStartTime * this.audioTimeFactor) !== refPlayer.currentTimeAudio) {
          refPlayer.onLabelEnd();
        }
      } else if(refPlayer.labelStartTime !== undefined && refPlayer.labelEndTime !== undefined && !refPlayer.selectLabel) {
          refPlayer.onLabelSave();
      }
    } else if (e.key === 'ArrowLeft' && refPlayer !== undefined) {
      this.currentTimeUser -= 1;
    } else if (e.key === 'ArrowRight' && refPlayer !== undefined) {
      this.currentTimeUser += 1;
    }
  };
},    
}
</script>

<style scoped>

.hide {
  visibility: hidden;
}

.semi_transparent {
  opacity: 0.5;
  filter: blur(10px);
}

.table-labels {
  border-style: solid;
  border-width: 2px;
  border-color: #59544d7d;
  border-radius: 6px;
}

.table-labels tr td {
  padding-left: 5px;
  padding-right: 5px;
  padding-top: 2px;
  padding-bottom: 2px;
}

.table-labels tr:has(th) {
  background-color: black;
}

.table-labels th {
  text-align: center;
  background-color: #b4b4b4;
  padding-top: 1px;
  padding-left: 5px;
  padding-right: 5px;
  padding-bottom: 1px;  
}

.table-labels td:nth-child(n+1):nth-child(-n+3) {
  font-family: monospace, sans-serif;
  text-align: right;
}

.table-labels tbody tr:nth-child(odd) td {
  background-color: #ececec;
}

.table-labels tbody tr:nth-child(even) td {
  background-color: #dbdbdb;
}

.table-labels tbody tr.selected-label-entry td {
  color: #168000;
  background-color: #39682461;
}


.label-name {
  background-color: #c8b69887;
  padding: 3px;
  margin-right: 5px;
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bd9;
  border-radius: 6px;
  box-shadow: 5px 5px 7px #443c3145;
}

.active {
  background-color: #669eb030;
}

.spectrogram {
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bd9;
  border-radius: 6px;
  box-shadow: 0px 0px 15px #443c319c;
}

audio {
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bb5;
  border-radius: 6px;
}

.input-comment {
  width: 100px;
  display: inline-block;
  margin-right: 10px;
}

.send-overlay {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 1;
  background-color: #5e5e5e30;
}

.send-error {
  color: #ee2727e0;
  background-color: #73717138;
  border-color: #ff0e008c;
  border-style: solid;
  padding: 5px;
}

.button-generated-label {
  background-color: #cecece;
  padding: 1px 10px 1px 10px;
  margin: 1px 10px 1px 10px;
  border-style: solid;
  border-width: 2px;
  border-color: #9b9d9b; 
}

.button-generated-label-selected {
  background-color: #cecece;
  padding: 1px 10px 1px 10px;
  margin: 1px 10px 1px 10px;
  border-style: solid;
  border-width: 2px;
  border-color: #3bb93b;
}


</style>

<style>

html {
  overflow: auto;
}

</style>
