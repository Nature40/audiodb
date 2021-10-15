<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="$refs.browser.show = true;">Browse</q-btn>
      <audio-browser ref="browser"/>
      <q-space></q-space>
      <div v-if="sample !== undefined">{{sample.id}}</div>
      <q-space></q-space>
      <q-btn @click="$refs.settings.show = true;">Settings</q-btn>
      <audio-settings ref="settings"/>
    </q-toolbar>
    <div style="margin-left: 15px; margin-right: 15px;">
    <q-slider v-model="canvasPixelPosX" :min="0" :max="spectrogramFullPixelLen - 1" @change="onSliderChange"/>
    </div>
    <div style="position: relative;" ref="canvasContainer" :style="{height: player_fft_cutoff + 'px'}">
      <canvas ref="spectrogram" style="position: absolute; top: 0px; left: 0px;" :width="canvasWidth" :height="player_fft_cutoff" :style="{width: canvasWidth + 'px', height: player_fft_cutoff + 'px'}" class="spectrogram" @mousedown="onCanvasMouseDown" @mousemove="onCanvasMouseMove"/>
      <q-linear-progress :value="spectrogramLoadedprogress" class="q-mt-md" size="25px" v-if="spectrogramLoadedprogress < 1 && spectrogramImagesErrorCount === 0" style="position: absolute; top: 0px; left: 0px;">
        <div class="absolute-full flex flex-center">
          <q-badge color="white" text-color="accent" label="loading spectrogram" />
        </div>
      </q-linear-progress>
      <q-badge v-if="spectrogramImagesErrorCount > 0" style="position: absolute; top: 0px; left: 0px;" color="white" text-color="accent" label="ERROR laoding spectrogram" />        
      <q-badge v-if="audioWaiting" color="yellow-14" text-color="accent" label="loading audio" style="position: absolute; top: 10px; right: 10px;"/>    
      <q-badge v-if="audioStalled" color="yellow-14" text-color="red" label="stalled loading audio" style="position: absolute; top: 30px; right: 10px;"/>

      <q-badge v-if="sampleLoading" color="grey-3" text-color="accent" label="loading metadata" style="position: absolute; top: 50px; left: 10px;"/>    
      <q-badge v-if="sampleError" color="grey-3" text-color="red" style="position: absolute; top: 80px; left: 10px;">
        error loading metadata
        <q-btn color="grey" @click="refreshSample">refresh</q-btn>
      </q-badge>

    </div>
    <q-toolbar class="bg-grey-3">
      <q-space></q-space>
      <q-btn @click="onAudioPlayButton" :disabled="audioPlaying">play</q-btn>
      <q-btn @click="onAudioPauseButton" :disabled="!audioPlaying">pause</q-btn>
      <q-space></q-space>
    </q-toolbar>    
  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';

import AudioBrowser from 'components/browser';
import AudioSettings from 'components/settings';

export default defineComponent({
  name: 'PageMain',

  components: {
    AudioBrowser,
    AudioSettings,
  },

  data() {
    return {
      sampleLoading: false,
      sampleError: false,
      sample: undefined,
      samplePos: undefined,
      sampleLen: undefined,
      spectrogramImages: [],
      imageNextIndex: undefined,
      spectrogramImagesLoadedCount: 0,
      spectrogramImagesErrorCount: 0,
      spectrogramId: 0,
      spectrogramImageMaxPixelLen: 2048,
      canvasPixelPosX: 0,
      canvasPixelPosXrequest: undefined,
      canvasMovePixelStartX: undefined,
      canvasWidth: 1024,
      paintSpectrogramRequested: false,
      audio: undefined,
      audioPlaying: false,
      audioWaiting: false,
      audioStalled: false,
      sampleRate: undefined,
    };
  },
  
  computed: {
    ...mapState({
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      player_fft_window: state => state.project.player_fft_window,
      player_fft_step: state => state.project.player_fft_step,
      player_fft_cutoff: state => state.project.player_fft_cutoff,
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
    }),
    spectrogramSettingsQuery() {
      return "&cutoff=" + this.player_fft_cutoff + "&step=" + this.player_fft_step + "&window=" + this.player_fft_window + "&threshold=" + this.player_spectrum_threshold + "&intensity_max=" + this.player_fft_intensity_max;
    },     
    selectedSampleId() {
      return this.$route.query.sample;
    },
    spectrogramImageMaxSampleLen() {
      return (this.spectrogramImageMaxPixelLen - 1) * this.player_fft_step + this.player_fft_window;
    },
    spectrogramFullPixelLen() {
      if(this.sampleLen === undefined) {
        return 0;
      }
      var p = Math.trunc((this.sampleLen - this.player_fft_window) / this.player_fft_step) + 1;
      return p < 0 ? 0 : p;
    },
    spectrogramFullSampleLen() {
      if(this.spectrogramFullPixelLen < 1) {
        return 0;
      }
      return (this.spectrogramFullPixelLen - 1) * this.player_fft_step + this.player_fft_window;
    },
    spectrogramImagesLen() {
      return Math.ceil(this.spectrogramFullPixelLen / this.spectrogramImageMaxPixelLen);
    },
    spectrogramLoadedprogress() {
      return this.spectrogramImagesLoadedCount / this.spectrogramImagesLen;
    },
    audioURL() {
      if(!this.sample) {
        return;
      }
      const baseURL = this.$api.defaults.baseURL;
      return baseURL + 'samples2/' + this.sample.id + '/audio';
    },    
  },

  methods: {
    paintSpectrogram() {      
      var canvasContainer = this.$refs.canvasContainer;
      this.canvasWidth = canvasContainer.clientWidth;
      //console.log(canvasContainer);
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
        return;
      }
      var imageIndexEnd = Math.trunc((this.canvasPixelPosX + (this.canvasWidth - 1)) / this.spectrogramImageMaxPixelLen);
      if(imageIndexEnd < 0) {
        return;
      }
       if(imageIndexEnd >= this.spectrogramImagesLen) {
        imageIndexEnd = this.spectrogramImagesLen - 1;
      }
      //console.log('draw');
      var next = undefined;
      for(var i = imageIndexStart; i <= imageIndexEnd; i++) {        
        var image = this.spectrogramImages[i];
        var canvasPixelX = i * this.spectrogramImageMaxPixelLen;
        var dstX = canvasPixelX - this.canvasPixelPosX;
        var dstY = 0;
        //console.log('draw image ' + i + " at " + dstX + " of images " +  this.spectrogramImagesLen);
        if(image === undefined) {
          if(next === undefined) {
            next = i;
          }
          ctx.fillStyle = 'grey';
          ctx.fillRect(dstX, dstY, this.spectrogramImageMaxPixelLen, this.player_fft_cutoff);
        } else {
          ctx.drawImage(image, dstX, dstY);
        }
      }
      if(next !== undefined) {
        this.imageNextIndex = next;
      }
      if(!this.audio.paused) {
        this.$nextTick(() => {
          console.log("next");
          this.onAudioTimeupdate();
        });
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
        image.src = baseURL + 'samples2/' + this.sample.id + '/spectrogram' + '?start_sample=' + start_sample + '&end_sample=' + end_sample + this.spectrogramSettingsQuery;
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
          var newCanvasPixelPosX = this.canvasPixelPosX - offsetX;
          this.moveToCanvasPixelPosX(newCanvasPixelPosX);
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
    onAudioPlayButton() {
      this.audio.play();
    },
    onAudioPauseButton() {
      this.audio.pause();
    },    
    onAudioPlaying() {
      this.audioPlaying = true;
      this.audioWaiting = false;
    },
    onAudioPause() {
      this.audioPlaying = false;
    },
    onAudioTimeupdate() {
      var t = this.audio.currentTime;
      var newSamplePos = t * this.sampleRate;
      if(newSamplePos < 0) {
        newSamplePos = 0;
      }
      if(newSamplePos >= this.sampleLen) {
        newSamplePos = this.sampleLen - 1;
      }
      newSamplePos = Math.trunc(newSamplePos);
      var newCanvasPixelPosX = newSamplePos / this.player_fft_step;
      if(newCanvasPixelPosX < 0) {
        newCanvasPixelPosX = 0;
      }
      if(newCanvasPixelPosX >= this.spectrogramFullPixelLen) {
        newCanvasPixelPosX = this.spectrogramFullPixelLen - 1;
      }
      newCanvasPixelPosX = Math.trunc(newCanvasPixelPosX);
      this.samplePos = newSamplePos;
      this.canvasPixelPosX = newCanvasPixelPosX;
      console.log(t + "  " + this.samplePos + "  " + this.canvasPixelPosX + "  " + this.player_fft_step);
    },
    moveToSamplePos(newSamplePos) {
      if(newSamplePos < 0) {
        newSamplePos = 0;
      }
      if(newSamplePos >= this.sampleLen) {
        newSamplePos = this.sampleLen - 1;
      }
      var t = newSamplePos / this.sampleRate;
      this.audio.currentTime = t;
    },
    moveToCanvasPixelPosX(newCanvasPixelPosX) {
      this.canvasPixelPosX = newCanvasPixelPosX;
      var newSamplePos = newCanvasPixelPosX * this.player_fft_step;
      this.moveToSamplePos(newSamplePos);
    },
    onSliderChange(newValue) {
      this.moveToCanvasPixelPosX(newValue);
    },
    onAudioWaiting() {
      this.audioWaiting = true;
    },
    onAudioStalled() {
      this.audioStalled = true;
    },
    onAudioLoadeddata() {
      this.audioStalled = false;
    },
    onAudioCanplay() {
      this.audioStalled = false;
    },
    onAudioCanplaythrough() {
      this.audioStalled = false;
    },
    async refreshSample() {
      if(this.selectedSampleId === undefined) {
        return;
      }
      console.log("querySample");
      try {
        var urlPath = 'samples2/' + this.selectedSampleId;
        var params = {samples: true, sample_rate: true,};
        this.sampleLoading = true;
        this.sampleError = false;
        var response = await this.$api.get(urlPath, {params});
        this.sampleLoading = false;
        this.sampleError = false;
        var sample = response.data?.sample;
        console.log(sample);
        this.sample = sample;
        this.sampleLen = sample.Samples;
        this.sampleRate = sample.sample_rate;
      } catch(e) {
        this.sampleLoading = false;
        this.sampleError = true;
        console.log(e);
      }
    },
  },

  watch: {
    selectedSampleId: {
      immediate: true,   
      async handler() {
        this.refreshSample();
      }
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
    spectrogramSettingsQuery() {
      this.$nextTick( () => {
        this.loadSpectrogram();
      });
    },
    audioURL() {
      this.audio.src = this.audioURL;
    },    
  },
  async mounted() {
    this.audio = new Audio();    
    this.audio.addEventListener('playing', e => this.onAudioPlaying(e));
    this.audio.addEventListener('pause', e => this.onAudioPause(e));
    this.audio.addEventListener('timeupdate', e => this.onAudioTimeupdate(e));
    this.audio.addEventListener('waiting', e => this.onAudioWaiting(e));
    this.audio.addEventListener('stalled', e => this.onAudioStalled(e));
    this.audio.addEventListener('loadeddata', e => this.onAudioLoadeddata(e));
    this.audio.addEventListener('canplay', e => this.onAudioCanplay(e));
    this.audio.addEventListener('canplaythrough', e => this.onAudioCanplaythrough(e));
   },  
})
</script>

<style scoped>
.spectrogram {
  border-style: solid;
  border-width: 1px;
  border-color: black;
}
</style>
