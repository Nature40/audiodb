<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="$refs.browser.show = true;">Browse</q-btn>
      <audio-browser ref="browser"/>
      <q-space></q-space><div v-if="sample !== undefined">{{sample.id}}</div><q-space></q-space>
    </q-toolbar>
    <div style="margin-left: 15px; margin-right: 15px;">
    <q-slider v-model="canvasPixelPosX" :min="0" :max="spectrogramFullPixelLen - 1"/>
    </div>
    <div style="position: relative;" ref="canvasContainer" :style="{height: spectrogramCutoff + 'px'}">
      <canvas ref="spectrogram" style="position: absolute; top: 0px; left: 0px;" :width="canvasWidth" :height="spectrogramCutoff" :style="{width: canvasWidth + 'px', height: spectrogramCutoff + 'px'}" class="spectrogram" @mousedown="onCanvasMouseDown" @mousemove="onCanvasMouseMove"/>
      <q-linear-progress :value="spectrogramLoadedprogress" class="q-mt-md" size="25px" v-if="spectrogramLoadedprogress < 1 && spectrogramImagesErrorCount === 0" style="position: absolute; top: 0px; left: 0px;">
        <div class="absolute-full flex flex-center">
          <q-badge color="white" text-color="accent" label="loading spectrogram" />
        </div>
      </q-linear-progress>
      <q-badge v-if="spectrogramImagesErrorCount > 0" style="position: absolute; top: 0px; left: 0px;" color="white" text-color="accent" label="ERROR laoding spectrogram" />        
    </div>
  </q-page>
</template>

<script>
import { defineComponent } from 'vue';

import AudioBrowser from 'components/browser';

export default defineComponent({
  name: 'PageMain',

  components: {
    AudioBrowser,
  },

  data() {
    return {
      sample: undefined,
      sampleLen: undefined,
      spectrogramImages: [],
      imageNextIndex: undefined,
      spectrogramImagesLoadedCount: 0,
      spectrogramImagesErrorCount: 0,
      spectrogramId: 0,
      spectrogramImageMaxPixelLen: 2048,
      spectrogramWindow: 1024,
      spectrogramStep: 1024,
      spectrogramCutoff: 512,
      canvasPixelPosX: 0,
      canvasMovePixelStartX: undefined,
      canvasWidth: 1024,
      paintSpectrogramRequested: false,
    };
  },
  
  computed: {
    selectedSampleId() {
      return this.$route.query.sample;
    },
    spectrogramImageMaxSampleLen() {
      return (this.spectrogramImageMaxPixelLen - 1) * this.spectrogramStep + this.spectrogramWindow;
    },
    spectrogramFullPixelLen() {
      if(this.sampleLen === undefined) {
        return 0;
      }
      var p = Math.trunc((this.sampleLen - this.spectrogramWindow) / this.spectrogramStep) + 1;
      return p < 0 ? 0 : p;
    },
    spectrogramFullSampleLen() {
      if(this.spectrogramFullPixelLen < 1) {
        return 0;
      }
      return (this.spectrogramFullPixelLen - 1) * this.spectrogramStep + this.spectrogramWindow;
    },
    spectrogramImagesLen() {
      return Math.ceil(this.spectrogramFullPixelLen / this.spectrogramImageMaxPixelLen);
    },
    spectrogramLoadedprogress() {
      return this.spectrogramImagesLoadedCount / this.spectrogramImagesLen;
    },    
  },

  methods: {
    paintSpectrogram() {      
      var canvasContainer = this.$refs.canvasContainer;
      this.canvasWidth = canvasContainer.clientWidth;
      console.log(canvasContainer);
      var canvas = this.$refs.spectrogram;
      var ctx = canvas.getContext("2d");
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      if(this.sampleLen === undefined) {
        return;
      }

      var imageIndexStart = Math.trunc(this.canvasPixelPosX / this.spectrogramImageMaxPixelLen);
      if(imageIndexStart < 0) {
        imageIndexStart = 0;
      }
      if(imageIndexStart >= this.spectrogramImagesLen) {
        return
      }
      var imageIndexEnd = Math.trunc((this.canvasPixelPosX + (this.canvasWidth - 1)) / this.spectrogramImageMaxPixelLen);
      if(imageIndexEnd < 0) {
        return;
      }
       if(imageIndexEnd >= this.spectrogramImagesLen) {
        imageIndexEnd = this.spectrogramImagesLen - 1;
      }
      console.log('draw');
      var next = undefined;
      for(var i = imageIndexStart; i <= imageIndexEnd; i++) {        
        var image = this.spectrogramImages[i];
        var canvasPixelX = i * this.spectrogramImageMaxPixelLen;
        var dstX = canvasPixelX - this.canvasPixelPosX;
        var dstY = 0;
        console.log('draw image ' + i + " at " + dstX + " of images " +  this.spectrogramImagesLen);
        if(image === undefined) {
          if(next === undefined) {
            next = i;
          }
          ctx.fillStyle = 'grey';
          ctx.fillRect(dstX, dstY, this.spectrogramImageMaxPixelLen, this.spectrogramCutoff);
        } else {
          ctx.drawImage(image, dstX, dstY);
        }
      }
      if(next !== undefined) {
        this.imageNextIndex = next;
      }
    },
    loadSpectrogram() {
      this.spectrogramId++;
      var id = this.spectrogramId;
      this.spectrogramImages = new Array(this.spectrogramImagesLen);
      this.spectrogramImagesLoadedCount = 0;
      this.spectrogramImagesErrorCount = 0;
      if(this.spectrogramImagesLen > 0) {   
        this.loadSpectrogramImage(id, 0);
      }
      this.paintSpectrogramRequested = true;
    },
    loadSpectrogramImage(id, imageIndex) {
      if(this.spectrogramImages[imageIndex] === undefined) {
        var image = new Image();
        image.onload = async () => {
          if(id != this.spectrogramId) {
            return;
          }
          this.paintSpectrogramRequested = true;
          this.spectrogramImagesLoadedCount++;
          //console.log('loaded');
          var imageBitmap = await createImageBitmap(image);
          this.spectrogramImages[imageIndex] = imageBitmap;
          var next = this.imageNextIndex;
          this.imageNextIndex = undefined;
          if(next == undefined) {
            next = imageIndex + 1;
          }
          if(next < this.spectrogramImagesLen) {            
            this.loadSpectrogramImage(id, next);
          } else if(this.spectrogramImagesLoadedCount < this.spectrogramImagesLen && this.spectrogramImagesLen > 0) {
            this.loadSpectrogramImage(id, 0);
          }
        }
        image.onerror = () => {
          this.spectrogramImagesLoadedCount++;
          this.spectrogramImagesErrorCount++;
        }
        var baseURL = this.$api.defaults.baseURL;
        var start_sample = imageIndex * this.spectrogramImageMaxSampleLen;
        var end_sample = start_sample + (this.spectrogramImageMaxSampleLen - 1);
        if(this.end_sample >= this.spectrogramFullSampleLen) {
          this.end_sample = this.spectrogramFullSampleLen - 1;
        }
        image.src = baseURL + 'samples2/' + this.sample.id + '/spectrogram' + '?start_sample=' + start_sample + '&end_sample=' + end_sample + "&cutoff=" + this.spectrogramCutoff + "&step=" + this.spectrogramStep;
      } else {
        var next = this.imageNextIndex;
        this.imageNextIndex = undefined;
        if(next == undefined) {
          next = imageIndex + 1;
        }
        if(next < this.spectrogramImagesLen) {            
          this.loadSpectrogramImage(id, next);
        } else if(this.spectrogramImagesLoadedCount < this.spectrogramImagesLen && this.spectrogramImagesLen > 0) {
          this.loadSpectrogramImage(id, 0);
        }
      }
    },
    onCanvasMouseDown(e) {      
      this.canvasMovePixelStartX = e.pageX;
      //console.log('onCanvasMouseDown ' + this.canvasMovePixelStartX);
    },
    onCanvasMouseMove(e) {
      if(this.canvasMovePixelStartX === undefined) {
        if(e.buttons == 1) { // left mouse button
          this.canvasMovePixelStartX = e.pageX;
        }
      } else {
        if(e.buttons == 1) { // left mouse button
          var offsetX = e.pageX - this.canvasMovePixelStartX;
          //console.log('offsetX ' + offsetX);
          this.canvasPixelPosX -= offsetX;
          this.canvasMovePixelStartX = e.pageX;
        } else {
          this.canvasMovePixelStartX = undefined;
        }
      }
    },
    paintSpectrogramRequestedAnimationFrame() {
      this.paintSpectrogram();
      this.paintSpectrogramRequested = false;
    }, 
  },

  watch: {
    selectedSampleId: {
      immediate: true,   
      async handler() {
        if(this.selectedSampleId === undefined) {
          return;
        }
        console.log("querySample");
        try {
          var urlPath = 'samples2/' + this.selectedSampleId;
          var response = await this.$api.get(urlPath, { params: {samples: true,} });
          var sample = response.data?.sample;
          console.log(sample);
          this.sample = sample;
          this.sampleLen = sample.Samples;
        } catch(e) {
          console.log(e);
        }
      },  
    },
    async sample() {
      this.$nextTick( () => {
        this.canvasPixelPosX = 0;
        this.loadSpectrogram();
      });        
    },
    canvasPixelPosX() {
      this.paintSpectrogramRequested = true;
    },
    paintSpectrogramRequested() {
      if(this.paintSpectrogramRequested) {
        requestAnimationFrame(() => {
          this.paintSpectrogramRequestedAnimationFrame();
        });
        
      }
    }, 
  }
})
</script>

<style scoped>
.spectrogram {
  border-style: solid;
  border-width: 1px;
  border-color: black;
}
</style>
