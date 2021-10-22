<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="$refs.browser.show = true;" icon="menu_book" title="Browse"  padding="xs"></q-btn>
      <audio-browser ref="browser"/>
      <q-space></q-space>
      <div v-if="sample !== undefined">
        <q-btn icon="navigate_before" padding="xs"/>
        <span class="text-weight-bold" v-if="sample.location"><q-icon name="home"/>{{sample.location}}</span>
        <span class="text-weight-regular text-grey-9" style="padding-left: 10px;" v-if="sample.date"><q-icon name="calendar_today"/>{{sample.date}}</span>
        <span class="text-weight-light text-grey-7" style="padding-left: 5px;" v-if="sample.time"><q-icon name="query_builder"/>{{sample.time}}</span>
        <span class="text-weight-thin text-grey-6" style="padding-left: 10px;" v-if="sample.device"><q-icon name="memory"/>{{sample.device}}</span>
        <span class="text-weight-bold" v-if="(!sample.location || !sample.device) && sample.date === undefined"><q-icon name="fingerprint"/>{{sample.id}}</span>
        <span class="text-weight-thin text-grey-6" style="padding-left: 10px;" v-if="sampleRate"><q-icon name="leaderboard"/>{{Math.trunc(sampleRate/1000)}}<sup style="font-size: 0.8em">.{{sampleRatemhz}}</sup> kHz</span>
        <span class="text-weight-thin text-grey-6" style="padding-left: 10px;" v-if="duration !== undefined"><q-icon name="alarm"/><span v-if="durationHH !== '00'">{{durationHH}}:</span><span class="text-grey-8">{{durationMM}}</span><span class="text-grey-6">:{{durationSS}}</span><sup class="text-grey-5" style="font-size: 0.7em" v-if="durationMS !== '000'">.{{durationMS}}</sup></span>
        <q-btn icon="navigate_next" padding="xs"/>
      </div>
      <div :style="{visibility: sample === undefined ? 'visible' : 'hidden',}">
        <q-badge color="yellow-14" text-color="accent" label="no audio sample selected"/> 
      </div>
      <q-space></q-space>
      <q-btn @click="$refs.settings.show = true;" icon="tune" title="Settings" padding="xs"></q-btn>
      <audio-settings ref="settings"/>
    </q-toolbar>
    <div :class="sampleVisibility" style="margin-left: 15px; margin-right: 15px;">
    <q-slider v-model="canvasPixelPosX" :min="0" :max="spectrogramFullPixelLen - 1" @change="onSliderChange"/>
    </div>
    <div :class="sampleVisibility" style="position: relative;" ref="canvasContainer" :style="{height: player_fft_cutoff + 'px'}">
      <canvas ref="spectrogram" style="position: absolute; top: 0px; left: 0px;" :width="canvasWidth" :height="player_fft_cutoff" :style="{width: canvasWidth + 'px', height: player_fft_cutoff + 'px'}" class="spectrogram" @mousedown="onCanvasMouseDown" @mousemove="onCanvasMouseMove" @mouseleave="onCanvasMouseleave"/>
      <q-linear-progress :value="spectrogramLoadedprogress" class="q-mt-md" size="25px" v-if="spectrogramLoadedprogress < 1 && spectrogramImagesErrorCount === 0" style="position: absolute; top: 0px; left: 0px; pointer-events: none;">
        <div class="absolute-full flex flex-center">
          <q-badge color="white" text-color="accent" label="loading spectrogram" />
        </div>
      </q-linear-progress>
      <q-badge v-if="spectrogramImagesErrorCount > 0" style="position: absolute; top: 0px; left: 0px;" color="white" text-color="accent">
        ERROR laoding spectrogram
        <q-btn color="grey" @click="spectrogramImagesErrorCount = 0; loadSpectrogramImage(spectrogramId, 0);">retry</q-btn>
      </q-badge>                
      <q-badge v-if="audioWaiting" color="yellow-14" text-color="accent" label="loading audio" style="position: absolute; top: 10px; right: 10px;"/>    
      <q-badge v-if="audioStalled" color="yellow-14" text-color="red" label="stalled loading audio" style="position: absolute; top: 30px; right: 10px;"/>

      <q-badge v-if="sampleLoading" color="grey-3" text-color="accent" label="loading metadata" style="position: absolute; top: 50px; left: 10px;"/>    
      <q-badge v-if="sampleError" color="grey-3" text-color="red" style="position: absolute; top: 80px; left: 10px;">
        error loading metadata
        <q-btn color="grey" @click="refreshSample">refresh</q-btn>
      </q-badge>
      
      <div v-if="mouseFreuqencyPos !== undefined" style="position: absolute; pointer-events: none; left: 0px; right: 0px; height: 1px; background-color: rgba(255, 255, 255, 0.41);" :style="{bottom: canvasMousePixelPosY + 'px',}"></div>
      <q-badge v-if="mouseFreuqencyPos !== undefined" style="position: absolute; pointer-events: none;" :style="{bottom: canvasMousePixelPosY + 'px', left: canvasMousePixelPosX + 'px',}" color="white" text-color="accent">
        <span v-html="mouseFreuqencyText"></span> kHz
        <span style="padding-left: 10px;">{{mouseTimePosText}}</span> s
      </q-badge> 
    </div>
    <q-toolbar class="bg-grey-3" :class="sampleVisibility">
      <q-space></q-space>
      <q-btn @click="if(audioPlaying) {onAudioPauseButton();} else {onAudioPlayButton();}" :icon="audioPlaying?'pause':'play_arrow'" title="Play" padding="xs"></q-btn>
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
      spectrogramImageMaxPixelLenUnaligned: 2048,
      spectrogramImageMaxSampleLenUnaligned: 134217728,
      canvasPixelPosX: 0,
      canvasPixelPosXrequest: undefined,
      canvasMovePixelStartX: undefined,
      canvasWidth: 1024,
      canvasMousePixelPosX: undefined,
      canvasMousePixelPosY: undefined,
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
      player_spectrum_shrink_Factor: state => state.project.player_spectrum_shrink_Factor,
      player_time_expansion_factor: state => state.project.player_time_expansion_factor,
    }),
    spectrogramSettingsQuery() {
      var q = "&cutoff=" + this.player_fft_cutoff + "&step=" + this.player_fft_step + "&window=" + this.player_fft_window + "&threshold=" + this.player_spectrum_threshold + "&intensity_max=" + this.player_fft_intensity_max;
      if(this.player_spectrum_shrink_Factor !== undefined && this.player_spectrum_shrink_Factor > 1) {
        q += "&shrink_factor=" + this.player_spectrum_shrink_Factor;
      }
      return q;
    },     
    selectedSampleId() {
      return this.$route.query.sample;
    },
    /*spectrogramImageMaxSampleLen() {
      return (this.spectrogramImageMaxPixelLen - 1) * (this.player_fft_step * this.player_spectrum_shrink_Factor) + this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) + this.player_fft_window;
    },*/
    spectrogramImageMaxPixelLen() {
      const p = Math.trunc((this.spectrogramImageMaxSampleLenUnaligned - this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) - this.player_fft_window) / (this.player_fft_step * this.player_spectrum_shrink_Factor)) + 1;
      return p < this.spectrogramImageMaxPixelLenUnaligned ? p : this.spectrogramImageMaxPixelLenUnaligned;      
    },
    spectrogramImageMaxSampleLen() {
      return (this.spectrogramImageMaxPixelLen - 1) * (this.player_fft_step * this.player_spectrum_shrink_Factor) + this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) + this.player_fft_window;
    },
    spectrogramFullPixelLen() {
      if(this.sampleLen === undefined) {
        return 0;
      }
      var p = Math.trunc((this.sampleLen - this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) - this.player_fft_window) / (this.player_fft_step * this.player_spectrum_shrink_Factor)) + 1;
      return p < 0 ? 0 : p;
    },
    spectrogramFullSampleLen() {
      if(this.spectrogramFullPixelLen < 1) {
        return 0;
      }
      return (this.spectrogramFullPixelLen - 1) * (this.player_fft_step * this.player_spectrum_shrink_Factor) + this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) + this.player_fft_window;
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
      var url = baseURL + 'samples2/' + this.sample.id + '/audio';
      if(this.sampleRate !== this.playerSampleRate) {
        url += '?overwrite_sampling_rate=' + this.playerSampleRate;
      }
      return url;
    },
    mouseFreuqencyPos() {
      return this.canvasMousePixelPosY === undefined || this.sampleRate === undefined || this.player_fft_window === undefined ? undefined : ((this.canvasMousePixelPosY * this.sampleRate) / this.player_fft_window);
    },
    mouseFreuqencyText() {
      return (this.mouseFreuqencyPos < 100000 ? (this.mouseFreuqencyPos < 10000 ? '&numsp;&numsp;' : '&numsp;' ) : '' ) + (this.mouseFreuqencyPos / 1000).toFixed(2);
    },
    mouseTimePos() {
      if(this.canvasMousePixelPosX === undefined || !this.player_fft_step || !this.player_spectrum_shrink_Factor || !this.sampleRate || this.canvasPixelPosX === undefined || this.player_time_expansion_factor === undefined) {
        return undefined;
      }
      return ((this.canvasPixelPosX + this.canvasMousePixelPosX) * (this.player_fft_step * this.player_spectrum_shrink_Factor)) / this.sampleRate;
    },
    mouseTimePosText() {
      return this.mouseTimePos === undefined ? '' : this.mouseTimePos.toFixed(3);
    },
    duration() {
      if(!this.sampleLen || !this.sampleRate) {
        return undefined;
      }
      return this.sampleLen / this.sampleRate;
    },
    durationHH() {
      if(this.duration === undefined) {
        return '--';
      }
      var h = Math.trunc(this.duration / 3600);
      var h10 = Math.trunc(h/10);
      var h1 = h%10; 
      return '' + h10 + '' + h1;
    },
    durationMM() {
      if(this.duration === undefined) {
        return '--';
      }
      var m = Math.trunc(this.duration / 60) % 60;
      var m10 = Math.trunc(m/10);      
      var m1 = m%10;
      return '' + m10 + '' + m1;
    },
    durationSS() {
      if(this.duration === undefined) {
        return '--';
      }
      var s = Math.trunc(this.duration % 60);
      var s10 = Math.trunc(s/10);
      var s1 = s%10;
      return '' + s10 + '' + s1;
      return (s < 10 ? '0' : '') + s.toFixed(3);
    },
    durationMS() {
      if(this.duration === undefined) {
        return '---';
      }
      var s = Math.trunc(this.duration * 1000) % 1000;
      var s100 = Math.trunc(s/100);
      var s10 = Math.trunc(s/10) % 10;
      var s1 = s % 10;
      return '' + s100 + '' + s10 + '' + s1;
    },
    sampleRatemhz() {
      if(this.sampleRate === undefined) {
        return '---';
      }
      var s = Math.trunc(this.sampleRate * 1000) % 1000;
      var s100 = Math.trunc(s/100);
      var s10 = Math.trunc(s/10) % 10;
      var s1 = s % 10;
      return '' + s100 + '' + s10 + '' + s1;
    },
    sampleVisibility() {
      return {
        'sample-hidden': this.sample === undefined,
      };
    },
    playerSampleRate() {
      return this.sampleRate === undefined ? undefined : Math.trunc(this.sampleRate / this.player_time_expansion_factor);
    }
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
      console.log('loadSpectrogram ' + id + "    " + this.spectrogramImagesLen + "  " + this.spectrogramFullPixelLen + "  " + this.sampleLen);
      this.spectrogramImages = new Array(this.spectrogramImagesLen);
      this.spectrogramImagesLoadedCount = 0;
      this.spectrogramImagesErrorCount = 0;
      if(this.spectrogramImagesLen > 0) {   
        this.loadSpectrogramImage(id, 0);
      }
      this.paintSpectrogramRequested = true;
    },
    async loadSpectrogramImage(id, imageIndex) {
      console.log('loadSpectrogramImage ' + id + '  ' + imageIndex);
      if(this.spectrogramImages[imageIndex] === undefined) {
        try {
          var baseURL = this.$api.defaults.baseURL;
          var start_sample = imageIndex * this.spectrogramImageMaxSampleLen;
          var end_sample = start_sample + (this.spectrogramImageMaxSampleLen - 1);
          if(end_sample >= this.spectrogramFullSampleLen) {
            end_sample = this.spectrogramFullSampleLen - 1;

            var p = Math.trunc((this.sampleLen - this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) - this.player_fft_window) / (this.player_fft_step * this.player_spectrum_shrink_Factor)) + 1;



          }
          var image = new Image();
          image.src = baseURL + 'samples2/' + this.sample.id + '/spectrogram' + '?start_sample=' + start_sample + '&end_sample=' + end_sample + this.spectrogramSettingsQuery;
          await image.decode();
          if(id != this.spectrogramId) {
            return;
          }
          var imageBitmap = await createImageBitmap(image);
          if(id != this.spectrogramId) {
            return;
          }
          this.spectrogramImages[imageIndex] = imageBitmap;
          this.paintSpectrogramRequested = true;
          this.spectrogramImagesLoadedCount++;          
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
        } catch {
          this.spectrogramImagesLoadedCount++;
          this.spectrogramImagesErrorCount++;
          /*var next = this.imageNextIndex;
          this.imageNextIndex = undefined;
          if(next == undefined) {
            next = imageIndex + 1;
          }
          if(next < this.spectrogramImagesLen) {            
            this.loadSpectrogramImage(id, next);
          } else if(this.spectrogramImagesLoadedCount < this.spectrogramImagesLen && this.spectrogramImagesLen > 0) {
            this.loadSpectrogramImage(id, 0);
          }*/          
        }
        /*var image = new Image();
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
        image.src = baseURL + 'samples2/' + this.sample.id + '/spectrogram' + '?start_sample=' + start_sample + '&end_sample=' + end_sample + this.spectrogramSettingsQuery;*/
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
          const mouseSpeedup = 8;
          console.log('offsetX ' + (e.pageX - this.canvasMovePixelStartX));
          var deltaX = (e.pageX - this.canvasMovePixelStartX);
          var offsetX = deltaX;
          //if(Math.abs(deltaX) > 20) {
            var offsetX = deltaX * mouseSpeedup;
          //}
          //console.log('offsetX ' + offsetX);
          var newCanvasPixelPosX = this.canvasPixelPosX - offsetX;
          this.moveToCanvasPixelPosX(newCanvasPixelPosX);
          this.canvasMovePixelStartX = e.pageX;
        } else {
          this.canvasMovePixelStartX = undefined;
        }
      }
      var rect = this.$refs.spectrogram.getBoundingClientRect();
      this.canvasMousePixelPosX = e.clientX - rect.left;
      this.canvasMousePixelPosY = (this.player_fft_cutoff - 1) - (e.clientY - rect.top);
    },
    onCanvasMouseleave(e) {
      this.canvasMousePixelPosX = undefined;
      this.canvasMousePixelPosY = undefined;
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
      const tPlayer = this.audio.currentTime;
      var newSamplePos = tPlayer * this.playerSampleRate;
      if(newSamplePos < 0) {
        newSamplePos = 0;
      }
      if(newSamplePos >= this.sampleLen) {
        newSamplePos = this.sampleLen - 1;
      }
      newSamplePos = Math.trunc(newSamplePos);
      var newCanvasPixelPosX = newSamplePos / (this.player_fft_step * this.player_spectrum_shrink_Factor);
      if(newCanvasPixelPosX < 0) {
        newCanvasPixelPosX = 0;
      }
      if(newCanvasPixelPosX >= this.spectrogramFullPixelLen) {
        newCanvasPixelPosX = this.spectrogramFullPixelLen - 1;
      }
      newCanvasPixelPosX = Math.trunc(newCanvasPixelPosX);
      this.samplePos = newSamplePos;
      this.canvasPixelPosX = newCanvasPixelPosX;
      console.log(tPlayer + "  " + this.samplePos + "  " + this.canvasPixelPosX + "  " + this.player_fft_step + "  " + this.player_spectrum_shrink_Factor);
    },
    moveToSamplePos(newSamplePos) {
      if(newSamplePos < 0) {
        newSamplePos = 0;
      }
      if(newSamplePos >= this.sampleLen) {
        newSamplePos = this.sampleLen - 1;
      }
      var tPlayer = newSamplePos / this.playerSampleRate;
      this.audio.currentTime = tPlayer;
    },
    moveToCanvasPixelPosX(newCanvasPixelPosX) {
      this.canvasPixelPosX = newCanvasPixelPosX;
      var newSamplePos = newCanvasPixelPosX * (this.player_fft_step * this.player_spectrum_shrink_Factor);
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
        var params = {samples: true, sample_rate: true};
        this.sampleLoading = true;
        this.sampleError = false;
        var response = await this.$api.get(urlPath, {params});
        this.sampleLoading = false;
        this.sampleError = false;
        var sample = response.data?.sample;
        console.log(sample);
        this.sample = sample;
        this.sampleLen = sample.samples;
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

.sample-hidden {
  visibility: hidden;
}
</style>
