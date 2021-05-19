<template>
  
<div style="overflow: auto" class="fit">

<q-page class="flex flex-center" v-if="photo === undefined">
  No photo selected.
</q-page>

<q-page v-if="photo !== undefined" class="flex flex-center column justify-center">
    <div class="col-auto">
      <q-btn :disable="!hasPrev" @click="move(-1)">prev</q-btn>
      <span class="time-text">{{dateText}}</span>
      <q-btn :disable="!hasNext" @click="move(+1)">next</q-btn>
    </div>
    <div style="width: 1024px; height: 768px; position: relative;">
      <img :src="imageURL" style="max-width: 1024px; max-height: 768px;" ref="image"/>
      <canvas style="position: absolute; top: 0px; left: 0px;" ref="image_overlay"/>
    </div>
    <div>
      <q-btn :disable="!hasPrev" @click="$refs.tagsDialog.show()">tags</q-btn>
    </div>
    <div>
      <div v-for="classification in classifications" :key="JSON.stringify(classification)">{{classification}}</div>
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
      return this.api('photodb2', 'photos', this.photo, 'image.jpg');
      //return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?width=1280&height=960';
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
      console.log("draw");
      var image = this.$refs.image;
      var width = image.width;
      var height = image.height;
      var canvas = this.$refs.image_overlay;
      canvas.width = width;
      canvas.height = height;
      console.log(width + "  " + height);
      var ctx = canvas.getContext("2d");
      ctx.fillStyle = "rgba(0, 0, 0, 0)";
      ctx.fillRect(0, 0, width, height);
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
    },  
  },
  
  watch: {
    classifications() {
      this.redrawImageOverlay();
    },
  },

  async mounted() {
  },
}
</script>

<style scoped>
.time-text {
  font-family: "Lucida Console", Courier, monospace;
}

</style>
