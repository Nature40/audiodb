<template>
  <q-dialog v-model="show" full-width @contextmenu="onContextmenu">
      <q-card v-if="show">
        <q-bar>
          <q-icon name="image"/>
          <div>Detail view</div>
          <div v-if="loading" class="text-primary">
            <q-spinner color="primary" />
            Processing ...
          </div>
          <div v-if="error" class="text-red">
            Error at processing or network connection.
          </div>          
          <q-space />
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>

        <q-toolbar class="bg-grey-3">
          <q-select
            filled
            v-model="userSelectedLabelNames"
            :options="selectableLabels"
            label="Labels"
            style="width: 250px"
            dense
            multiple
            option-label="name"
            option-value="name"
            emit-value
            clearable
          >
            <template v-slot:option="scope">
              <q-item v-bind="scope.itemProps">
                <q-item-section>
                  <q-item-label><b>{{scope.opt.name}}</b> <span style="color: grey;">- {{scope.opt.desc}}</span></q-item-label>
                </q-item-section>
              </q-item>
            </template>          
          </q-select>
          <q-btn icon="push_pin" label="Save new segment" size="xs" padding="xs" margin="xs" title="Save new time segment with currently selected labels" @click="onNewTimeSegmentSave"/>
          <q-badge color="grey-4" text-color="grey-8" style="margin-left: 50px;">
          Place mouse cursor at spectrogram segment start position, press and hold left mouse button, move mouse cursor to segment end position, release left mouse button,<br> select correct label, click right mouse button to save the new segment.          
          </q-badge>
        </q-toolbar>     

        <!--<q-card-section>-->
          <div :style="{
                        position: 'relative',
                        width: canvasWidth + 'px', 
                        height: canvasHeight + 'px'
                      }">
            <canvas 
              ref="spectrogram" 
              :width="canvasWidth" 
              :height="canvasHeight" 
              :style="{
                        position: 'absolute', 
                        top: '0px', 
                        left: '0px',
                        width: canvasWidth + 'px', 
                        height: canvasHeight + 'px'
                      }" 
              class="spectrogram"
              @mousedown="onMousedown"
              @mousemove="onMousemove"
              @mouseup="onMouseup"
              @mouseleave="onMouseleave"
            />

            <template v-if="staticLinesCanvasPosY !== undefined">
              <template v-for="staticLineCanvasPosY in staticLinesCanvasPosY" :key="staticLineCanvasPosY">
                <div v-if="staticLineCanvasPosY >= player_fft_cutoff_lower && staticLineCanvasPosY < player_fft_cutoff" style="position: absolute; pointer-events: none; left: 0px; right: 0px; height: 1px; background-color: rgba(0, 255, 255, 0.30);" :style="{bottom: (staticLineCanvasPosY - player_fft_cutoff_lower) + 'px',}"></div>
              </template>
        	  </template>

            <div v-if="mousePixelPosX !== undefined && mousePixelPosY !== undefined" style="position: absolute; pointer-events: none; left: 0px; right: 0px; height: 1px; background-color: rgba(255, 255, 255, 0.41);" :style="{bottom: mousePixelPosY + 'px',}"></div>
            <q-badge v-if="mousePixelPosX !== undefined && mousePixelPosY !== undefined" style="position: absolute; pointer-events: none;" :style="{bottom: mousePixelPosY + 'px', left: mousePixelPosX + 'px',}" color="white" text-color="accent">
              <span v-html="mouseFrequencyText"></span> kHz
              <span style="padding-left: 10px;">{{mouseTimePosText}}</span> s
            </q-badge>             
          </div>
        <!--</q-card-section>-->

        <q-separator />

      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'detail-view',

  props: ['sampleRate', 'labels'],

  setup () {
    const show = ref(false);
    return {
      show,
    };
  },
  data() {
    return {  
      sample: undefined,
      samplePos: undefined,
      canvasWidth: 100,
      canvasHeight: 300,
      image: undefined,
      loading: false, 
      error: false,
      mouseStartX: undefined, 
      mouseEndX: undefined,
      labelStartX: undefined,
      labelEndX: undefined,
      userSelectedLabelNames: undefined,
      selectableLabels: undefined,
      mousePixelPosX: undefined,
      mousePixelPosY: undefined,
    };
  },
  computed: {
    ...mapState({
      player_fft_cutoff_lower_frequency: state => state.project.player_fft_cutoff_lower_frequency,
      player_fft_cutoff_upper_frequency: state => state.project.player_fft_cutoff_upper_frequency,    
      //player_fft_step: state => state.project.player_fft_step,
      player_fft_window: state => state.project.player_fft_window,      
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
      player_static_lines_frequency: state => state.project.player_static_lines_frequency,     
    }),
    player_fft_cutoff_lower() {
      let c = Math.floor((this.player_fft_cutoff_lower_frequency *  this.player_fft_window) / this.sample.sample_rate);
      return c < 0 ? 0 : c > (this.player_fft_window / 2) - 1 ? (this.player_fft_window / 2) - 1 : c;
    },    
    player_fft_cutoff() {
      let c = Math.floor((this.player_fft_cutoff_upper_frequency *  this.player_fft_window) / this.sample.sample_rate);
      return c < 1 ? 1 : c > this.player_fft_window / 2 ? this.player_fft_window / 2 : c;
    },
    player_fft_cutoff_range() {
      return this.player_fft_cutoff - this.player_fft_cutoff_lower;
    },
    player_fft_step() {
      if(this.player_fft_window === undefined) {
        return undefined;
      }
      return this.player_fft_window / 4;
    },
    player_spectrum_shrink_Factor() {
      return 1;
    },    
    spectrogramSettingsQuery() {
      var q = "&cutoff_lower=" + this.player_fft_cutoff_lower 
      + "&cutoff=" + this.player_fft_cutoff 
      + "&step=" + this.player_fft_step
      + "&window=" + this.player_fft_window
      + "&threshold=" + this.player_spectrum_threshold 
      + "&intensity_max=" + this.player_fft_intensity_max;
      return q;
    },
    labelStartPixelX() {
      if(this.labelStartX === undefined) {
        return undefined;
      }
      return this.samplePosToPixelPos(this.labelStartX);
    },
    labelEndPixelX() {
      if(this.labelEndX === undefined) {
        return undefined;
      }
      return this.samplePosToPixelPos(this.labelEndX);
    },
    mouseFrequencyPos() {
      return this.mousePixelPosY === undefined || this.sampleRate === undefined || this.player_fft_window === undefined ? undefined : (((this.player_fft_cutoff_lower + this.mousePixelPosY) * this.sampleRate) / this.player_fft_window);
    },
    mouseFrequencyText() {
      return (this.mouseFrequencyPos < 100000 ? (this.mouseFrequencyPos < 10000 ? '&numsp;&numsp;' : '&numsp;' ) : '' ) + (this.mouseFrequencyPos / 1000).toFixed(2);
    },
    mouseSamplePos() {
      if(this.mousePixelPosX === undefined) {
        return undefined;
      }
      return this.pixelPosToSamplePos(this.mousePixelPosX);
    },    
    mouseTimePos() {
      if(this.mouseSamplePos === undefined || !this.sampleRate) {
        return undefined;
      }
      return this.mouseSamplePos / this.sampleRate;
    },
    mouseTimePosText() {
      return this.mouseTimePos === undefined ? '' : this.mouseTimePos.toFixed(3);
    },
    staticLinesCanvasPosY() {
      return this.sampleRate === undefined || this.player_fft_window === undefined || this.player_static_lines_frequency === undefined || this.player_static_lines_frequency.length === 0 ? undefined : this.player_static_lines_frequency.map(staticLineFrequency => Math.round((staticLineFrequency * this.player_fft_window) / this.sampleRate));
    },             
  },
  methods: {
    refresh() {
      this.loadSpectrogramImage();
    },
    async loadSpectrogramImage() {
      try {
        this.loading = true;
        this.error = false;
        var baseURL = this.$api.defaults.baseURL;
        var sampleLen = 2048*400;
        var start_sample = this.samplePos;
        if(start_sample < 0) {
          start_sample = 0;
        }
        if(start_sample >= (this.sample.samples - sampleLen)) {
          start_sample = this.sample.samples - sampleLen;
        }
        var end_sample = start_sample + sampleLen;
         if(end_sample < 0) {
          end_sample = 0;
        }
        if(end_sample >= (this.sample.samples - sampleLen)) {
          end_sample = this.sample.samples - sampleLen;
        }
        var image = new Image();
        image.src = baseURL + 'samples2/' + this.sample.id + '/spectrogram' + '?start_sample=' + start_sample + '&end_sample=' + end_sample + this.spectrogramSettingsQuery;
        await image.decode();        
        this.image = image;
        this.canvasWidth = this.image.width;
        this.canvasHeight = this.image.height;
        this.loading = false;
        this.$nextTick( () => {
          this.repaint();
        });                 
      } catch {
        this.loading = false;
        this.error = true;
        console.log('error');         
      }
    },
    repaint() {
      //console.log('repaint'); 
      var canvas = this.$refs.spectrogram;
      if(!canvas) {
        return;
      }
      var ctx = canvas.getContext("2d");
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      if(this.image !== undefined) {
        //console.log('drawImage'); 
        //console.log(this.image);
        ctx.drawImage(this.image, 0, 0);          
      }

      if(this.labels !== undefined) {
        for(var i = 0; i < this.labels.length; i++) {
          var label = this.labels[i];
          var labelPixelXmin = Math.trunc(Math.trunc(label.start * this.sampleRate - this.samplePos) / (this.player_fft_step * this.player_spectrum_shrink_Factor));
          var labelPixelXmax = Math.trunc(Math.trunc(label.end * this.sampleRate - this.samplePos) / (this.player_fft_step * this.player_spectrum_shrink_Factor));
          //console.log(labelPixelXmin + ' ' + labelPixelXmax + '  ' + canvasPixelXmin + ' ' + canvasPixelXmax);
          if(0 <= labelPixelXmax && this.canvasWidth >= labelPixelXmin) {
            //console.log('fill ' + labelPixelXmin + ' ' + labelPixelXmax + '  ' + canvasPixelXmin + ' ' + canvasPixelXmax);
            ctx.fillStyle = i === this.selectedLabelIndex ? 'rgba(255,0,0,0.3)' : 'rgba(0,255,0,0.3)';
            ctx.fillRect(labelPixelXmin, 0, labelPixelXmax - labelPixelXmin + 1, this.player_fft_cutoff_range);
          }
        }
      }

      if(this.mouseStartX !== undefined && this.mouseEndX !== undefined) {
        ctx.fillStyle = 'rgba(0,0,255,0.3)';
        ctx.fillRect(this.mouseStartX, 0, this.mouseEndX - this.mouseStartX + 1, this.canvasHeight);
      } 
      if(this.labelStartPixelX !== undefined && this.labelEndPixelX !== undefined) {
        ctx.fillStyle = 'rgba(255,255,0,0.3)';
        ctx.fillRect(this.labelStartPixelX, 0, this.labelEndPixelX - this.labelStartPixelX + 1, this.canvasHeight);
      }      
    },
    onMousedown(e) {
      if(e.buttons == 1) {
        //console.log('onMousedown');
        //console.log(e);
        const x = e.offsetX;
        this.mouseStartX = x;
        this.mouseEndX = x;
        this.repaint();
      }
    },
    onMousemove(e) {
      const x = e.offsetX;
      const y = e.offsetY;
      this.mousePixelPosX = x;
      this.mousePixelPosY = this.canvasHeight - 1 - y;
      if(e.buttons == 1) {
        if(this.mouseStartX !== undefined) {
          //console.log('onMousemove');
          //console.log(e);          
          this.mouseEndX = x;
        }
      } else {
        this.mouseStartX = undefined;
        this.mouseEndX = undefined;
      }
      this.repaint();
    },
    onMouseup(e) {
      if(this.mouseStartX !== undefined) {
        console.log('onMouseup');
        //console.log(e);
        const x = e.offsetX;
        this.mouseEndX = x;
        this.labelStartX = this.pixelPosToSamplePos(this.mouseStartX);
        this.labelEndX = this.pixelPosToSamplePos(this.mouseEndX);
        console.log(this.samplePos + '  ' + x + '  ' + this.pixelPosToSamplePos(x));
        this.mouseStartX = undefined;
        this.mouseEndX = undefined;
        this.repaint();
      }
    },
    onMouseleave() {
      this.mousePixelPosX = undefined;
      this.mousePixelPosY = undefined;
    },
    pixelPosToSamplePos(x) {
      if(this.samplePos === undefined || x === undefined || !this.player_fft_step || !this.player_spectrum_shrink_Factor) {
        return undefined;
      }
      return this.samplePos + x * (this.player_fft_step * this.player_spectrum_shrink_Factor);
    },
    samplePosToPixelPos(x) {
      if(this.samplePos === undefined || x === undefined || !this.player_fft_step || !this.player_spectrum_shrink_Factor) {
        return undefined;
      }
      return (x - this.samplePos) / (this.player_fft_step * this.player_spectrum_shrink_Factor);      
    },
    onContextmenu(e) {
      e.preventDefault();      
      this.onNewTimeSegmentSave();
    },
    onNewTimeSegmentSave() {
      console.log('onNewTimeSegmentSave');  
      console.log('A' + this.labelStartX);    
      let event = {};
      console.log('B' + this.labelEndX);  
      event.start = this.labelStartX;
      console.log('C');  
      event.end = this.labelEndX;
      console.log('D');  
      event.names = this.userSelectedLabelNames;
      console.log(event);
      this.$emit('save', event);
    },
  },
  watch: {
    show() {
      if(this.show) {
        this.refresh();
      }
    },
    labels() {
      this.repaint();
    },
  },
  async mounted() {
  },
});
</script>

<style scoped>
.spectrogram {
  /*border-style: solid;
  border-width: 1px;
  border-color: black;*/
  /*background-color: #e5e5f7;*/
  opacity: 1;
  background-image:  repeating-linear-gradient(45deg, #202020 25%, transparent 25%, transparent 75%, #202020 75%, #202020), repeating-linear-gradient(45deg, #202020 25%, #404040 25%, #404040 75%, #202020 75%, #202020);  background-position: 0 0, 10px 10px;
  background-size: 20px 20px;
  cursor: crosshair;
}
</style>
