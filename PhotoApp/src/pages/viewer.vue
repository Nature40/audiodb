<template>
  
<div style="overflow: auto" class="fit">

<q-page class="flex flex-center" v-if="photo === undefined">
  No photo selected.
</q-page>

<q-page v-if="photo !== undefined" class="column wrap  items-center ">
    <div class="col-auto">
      <q-btn :disable="!hasPrev" @click="move(-1)">prev</q-btn>
      <span class="time-text">{{dateText}}</span>
      <q-btn :disable="!hasNext" @click="move(+1)">next</q-btn>      
    </div>
    <div style="position: relative;" class="" ref="imageDiv">
      <img :src="imageURL" :style="{'max-width': maxImageWidth + 'px', 'max-height': maxImageHeight + 'px'}" ref="image" @load="onLoadImage"/>
      <canvas style="position: absolute; top: 0px; left: 0px;" ref="image_overlay"/>
      <q-spinner-gears color="primary" size="4em" v-show="imageLoading" style="position: absolute; top: 0px; right: 0px;"/>
    </div>
    <div class="row" style="padding-bottom: 10px;">
      <q-select v-model="processing" :options="['original', 'lighten', 'lighten strong']" label="Processing" dense options-dense style="width: 200px;" rounded standout/>
      <q-select v-model="scaling" :options="['fast', 'high quality']" label="Scaling" dense options-dense style="width: 200px;" rounded standout/>
    </div>
    <!--<div>
      <q-btn :disable="!hasPrev" @click="$refs.tagsDialog.show()">tags</q-btn>
    </div>-->
    <div>
      <table class="blueTable">
        <thead>
          <tr>
            <th>Classification</th>
            <th>Classificator</th>
            <th>Identity</th>
            <th>Reliability</th>            
            <th>Date</th>
            <th>Bbox</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in classifications" :key="JSON.stringify(entry)" style="margin-right: 15px; color: #111191c4;">
            <td><b>{{entry.classification}}</b></td>
            <td>{{entry.classificator}}</td>
            <td>{{entry.expert_name}}</td>
            <td>{{entry.conf === undefined ? entry.uncertainty : entry.conf}}</td>            
            <td>{{entry.date}}</td>
            <td>{{entry.bbox}}</td>
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
  }),  

  computed: {
    ...mapState({
      photo: state => state.photo.photo,
      photoMeta: state => state.photo.meta,
    }),     
    ...mapGetters({
      api: 'api',
      hasPrev: 'photo/hasPrev',
      hasNext: 'photo/hasNext',
    }),
    imageURL() {
      //return this.api('photodb2', 'photos', this.photo, 'image.jpg');
      //return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?width=1024&height=768';
      if(this.scaling === 'high quality') {
        if(this.processing === 'lighten') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=2&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;
        } else if(this.processing === 'lighten strong') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=3&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;
        } else {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?&width=' + this.maxImageWidth + '&height=' + this.maxImageHeight;
        }
      } else {
        if(this.processing === 'lighten') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=2';
        } else if(this.processing === 'lighten strong') {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?gamma=3';
        } else {
          return this.api('photodb2', 'photos', this.photo, 'image.jpg');
        }
      }
    },
    dateText() {
      var date = this.photoMeta.date;
      if(date === undefined) {
        return "0000-00-00 00:00";
      }
      return date.getUTCFullYear() +
        '-' + pad(date.getUTCMonth() + 1) +
        '-' + pad(date.getUTCDate()) +
        ' ' + pad(date.getUTCHours()) +
        ':' + pad(date.getUTCMinutes());
    },
    classifications() {
      if(this.photoMeta === undefined || this.photoMeta.data === undefined || this.photoMeta.data.classifications === undefined) {
        return [];
      }
      return this.photoMeta.data.classifications;
    },    
  },

  methods: {
    ...mapActions({
      move: 'photo/move',
    }),
    redrawImageOverlay() {
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
        this.classifications.forEach(classification => {
          if(classification.bbox !== undefined) {
            console.log(classification.bbox);
            var xmin = classification.bbox[0] * width;
            var ymin = classification.bbox[1] * height;
            var boxwidth = classification.bbox[2] * width;
            var boxheight = classification.bbox[3] * height;
            //ctx.fillRect(xmin, ymin, boxwidth, boxheight);
            ctx.strokeRect(xmin, ymin, boxwidth, boxheight);
          }
        });
      }
    },
    onLoadImage() {
      this.imageLoading = false;
      console.log("loaded");
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
    }  
  },
  
  watch: {
    classifications() {
      this.redrawImageOverlay();
    },
    imageURL() {
      this.imageLoading = true;
      this.redrawImageOverlay();
    }
  },

  async mounted() {
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

</style>
