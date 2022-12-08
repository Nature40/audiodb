<template>
  <q-dialog v-model="show" maximized @contextmenu="onContextmenu">
      <q-card v-if="show" id="card">
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
          <q-btn @click="dialoghelpShow = true;" icon="help_outline" dense flat style="margin-left: 10px;" title="Get help."></q-btn>
          <q-dialog
            v-model="dialoghelpShow"
            :maximized="dialoghelpMaximizedToggle"
            transition-show="slide-down"
            transition-hide="slide-up"
          >
            <q-card class="bg-grey-3 text-black">
              <q-bar>
                <q-icon name="help_outline" />
                <div>Help</div>
                <q-space />
                <q-btn dense flat icon="window" @click="dialoghelpMaximizedToggle = false" v-show="dialoghelpMaximizedToggle">
                  <q-tooltip v-if="dialoghelpMaximizedToggle">Minimize</q-tooltip>
                </q-btn>
                <q-btn dense flat icon="crop_square" @click="dialoghelpMaximizedToggle = true" v-show="!dialoghelpMaximizedToggle">
                  <q-tooltip v-if="!dialoghelpMaximizedToggle">Maximize</q-tooltip>
                </q-btn>
                <q-btn dense flat icon="close" v-close-popup>
                  <q-tooltip>Close</q-tooltip>
                </q-btn>
              </q-bar>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Detail labeling</div>
                <ol>
                  <li><b>Place</b> mouse cursor at spectrogram segment start position.</li>
                  <li><b>Press and hold</b> left mouse button.</li> 
                  <li><b>Move</b> mouse cursor to segment end position by holding left mouse button.</li>
                  <li><b>Release</b> left mouse button.</li>
                  <li><b>Select</b> correct label if not selected already.</li>
                  <li><b>Click</b> right mouse button to save the new segment.</li>
                </ol>
              </q-card-section>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Move in time</div>
                <ol>
                  <li><b>Place</b> mouse cursor at spectrogram.</li>
                  <li><b>Press and hold</b> shift key.</li> 
                  <li><b>Press and hold</b> left mouse button and hold shift key.</li>
                  <li>(Shift key can be released now.)</li> 
                  <li><b>Move</b> mouse cursor left or right by holding left mouse button.</li>
                  <li><b>Release</b> left mouse button to start loading moved spectrogram.</li>
                </ol>
              </q-card-section>
              
              <q-card-section class="q-pt-none">
                <div class="text-h6">Zoom in/out</div>
                <i>Low overlap, e.g. 0%, leads to a small low detail spectrogram.
                <br>High overlap, e.g. 90%, leads to a large high detail spectrogram.</i>
                <ol>
                  <li><b>Click</b> 'overlap'-button on the top right.</li>
                  <li><b>Move</b> slider for low/high FFT window overlap.</li> 
                  <li><b>Click</b> outside of the box to apply changes.</li>
                </ol>
              </q-card-section>                 
            </q-card>
          </q-dialog>          
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
          <q-btn icon="push_pin" label="Save new segment" size="xs" padding="xs" margin="xs" title="Save new time segment with currently selected labels" @click="onNewTimeSegmentSave" :disabled="labelStartX === undefined || labelEndX === undefined"/>

          <q-badge color="grey-4" text-color="grey-8" style="margin-left: 50px;">
          Place mouse cursor at spectrogram segment start position, press and hold left mouse button, 
          <br>move mouse cursor to segment end position, release left mouse button,
          <br> select correct label, click right mouse button to save the new segment.          
          </q-badge>
          <q-badge color="grey-4" text-color="grey-8" style="margin-left: 50px;" v-if="Number.isFinite(mouseStartX) && Number.isFinite(mouseEndX)">
            Segment  
            {{(pixelPosToSamplePos(mouseStartX) / sample.sample_rate).toFixed(3)}}
            ..
            {{(pixelPosToSamplePos(mouseEndX) / sample.sample_rate).toFixed(3)}}
            (
            <b>{{((pixelPosToSamplePos(mouseEndX) - pixelPosToSamplePos(mouseStartX) + 1)/ sample.sample_rate).toFixed(3)}}</b>
            )
          </q-badge>           
          <q-badge color="grey-4" text-color="grey-8" style="margin-left: 50px;" v-else-if="Number.isFinite(labelStartX) && Number.isFinite(labelEndX)">
            Segment  
            {{(labelStartX / sample.sample_rate).toFixed(3)}}
            ..
            {{(labelEndX / sample.sample_rate).toFixed(3)}}
            (
            <b>{{((labelEndX - labelStartX + 1)/ sample.sample_rate).toFixed(3)}}</b>
            )
          </q-badge>         
          <q-space />

          <q-btn @click="onMovePrevSamples" icon="arrow_left" padding="xs" :loading="loading" title="Move backward in time."></q-btn>
          <span>{{(start_sample / sample.sample_rate).toFixed(3)}} - {{( (end_sample + 1) / sample.sample_rate).toFixed(3)}}</span>
          <q-btn @click="onMoveNextSamples" icon="arrow_right" padding="xs" :loading="loading" title="Move forward in time."></q-btn>
          <q-checkbox
            v-model="hideLabels"
            label="Hide labels"
            color="red"
            title="If checked, don't show marked intervals of existing labels on the spectrogram to better view the spectrogram details."
          />
          <q-btn icon="architecture" round padding="xs" style="margin-left: 20px" title="Change spectrogram overlap ratio.">
            <q-menu @hide="onOverlapMenuHide">
              <q-list style="min-width: 100px">
                <q-item style="min-width: 100px; padding: 20px;">
                  <q-item-section>
                    <q-item-label>Spectrogram overlap</q-item-label>
                    <q-item-label caption style="padding-bottom: 20px;"> 
                      <q-slider 
                        style="min-width: 200px;"
                        v-model="userWindowOverlapPercent"
                        :min="0"
                        :max="95"
                        :step="1"
                        label
                        :label-value="userWindowOverlapPercent + '%'"
                        label-always
                        switch-label-side
                        @change="userWindowOverlapPercentChanged = true"
                      />
                    </q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
             
            </q-menu>
          </q-btn>          
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
      imageStartSample: undefined,
      loading: false, 
      error: false,
      mouseModus: undefined,
      mouseMoveTimeStartX: undefined,
      mouseMoveTimeEndX: undefined,
      mouseStartX: undefined, 
      mouseEndX: undefined,
      labelStartX: undefined,
      labelEndX: undefined,
      userSelectedLabelNames: undefined,
      selectableLabels: undefined,
      mousePixelPosX: undefined,
      mousePixelPosY: undefined,
      spectrogramImageMaxPixelLenUnaligned: 2048,
      spectrogramImageMaxSampleLenUnaligned: 134217728,
      windowOverlapFactor: 4,
      userWindowOverlapPercent: 75,
      userWindowOverlapPercentChanged: false,
      hideLabels: false,
      dialoghelpShow: false,
      dialoghelpMaximizedToggle: false,      
    };
  },
  computed: {
    ...mapState({
      player_fft_cutoff_lower_frequency: state => state.project.player_fft_cutoff_lower_frequency,
      player_fft_cutoff_upper_frequency: state => state.project.player_fft_cutoff_upper_frequency,    
      player_fft_window: state => state.project.player_fft_window,      
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
      player_static_lines_frequency: state => state.project.player_static_lines_frequency,    
      
      detail_fft_window_overlap_percent: state => state.project.detail_fft_window_overlap_percent, 
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
      const step = Math.round(this.player_fft_window / this.windowOverlapFactor);
      return (step < 1) ? 1 : (step > this.player_fft_window ? this.player_fft_window : step);
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
    subsetMaxPixelCount() {
      const mOne = this.spectrogramImageMaxSampleLenUnaligned - this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) - this.player_fft_window;
      if(mOne < 0) {
        return 0;
      }
      const pixelCountFloat = mOne / (this.player_fft_step * this.player_spectrum_shrink_Factor);
      const pixelCount = Math.floor(pixelCountFloat) + 1;
      return pixelCount < this.spectrogramImageMaxPixelLenUnaligned ? pixelCount :  this.spectrogramImageMaxPixelLenUnaligned;
    },
    subsetMaxSampleLen() {
      return (this.subsetMaxPixelCount - 1) * this.player_fft_step + this.player_fft_window;
    },
    start_sample() {
      if(this.samplePos === undefined)  {
        return 0;
      }
      let pos = this.samplePos;
      if(pos >= (this.sample.samples - this.subsetMaxSampleLen)) {
        pos = this.sample.samples - this.subsetMaxSampleLen;
      }
      if(pos < 0) {
        pos = 0;
      }
      return pos;
    },
    end_sample() {
      let pos = this.start_sample + (this.subsetMaxSampleLen - 1);
      if(pos > (this.sample.samples - 1)) {
        pos = this.sample.samples - 1;
      }
      if(pos < 0) {
        pos = 0;
      }        
      return pos;
    },
    moveSampleStep() {
      const pixels = Math.floor(this.subsetMaxPixelCount / 2);
      return pixels * this.player_fft_step * this.player_spectrum_shrink_Factor;
    },            
  },
  methods: {
    refresh() {
      this.loadSpectrogramImage();
    },
    async loadSpectrogramImage() {
      if(!this.sample) {
        return;
      }
      try {
        this.loading = true;
        this.error = false;
        var baseURL = this.$api.defaults.baseURL;
        /*var sampleLen = 2048*400;
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
        }*/
        var image = new Image();
        const imageUrl = baseURL + 'samples2/' + this.sample.id + '/spectrogram' + '?start_sample=' + this.start_sample + '&end_sample=' + this.end_sample + this.spectrogramSettingsQuery;
        image.src = imageUrl;
        await image.decode();   
        this.image = image;
        this.imageStartSample = this.start_sample;        
        this.canvasWidth = this.image.width;
        this.canvasHeight = this.image.height;
        this.loading = false;
        this.$nextTick( () => {
          this.repaint();
        });                 
      } catch(e) {
        this.loading = false;
        this.error = true;
        console.log('error ');         
        console.log(e); 
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
      let sampleStart = this.start_sample;
      let pixelDelta = 0;
      if(this.image !== undefined) {
        //console.log('drawImage'); 
        //console.log(this.image);
        if(this.mouseModus === 'move_time') {
          const moveTimeSampleStartX = this.pixelPosToSamplePos(this.mouseMoveTimeStartX);
          const moveTimeSampleEndX = this.pixelPosToSamplePos(this.mouseMoveTimeEndX);
          const diff = moveTimeSampleEndX - moveTimeSampleStartX;
          sampleStart = this.start_sample - diff;
          console.log('move_time repaint');
        }
        pixelDelta = this.samplePosToPixelPos(this.imageStartSample) - this.samplePosToPixelPos(sampleStart);
        ctx.drawImage(this.image, pixelDelta, 0);          
      }

      if(!this.hideLabels && this.labels !== undefined) {
        for(var i = 0; i < this.labels.length; i++) {
          var label = this.labels[i];
          var labelPixelXmin = Math.trunc(Math.trunc(label.start * this.sampleRate - sampleStart) / (this.player_fft_step * this.player_spectrum_shrink_Factor));
          var labelPixelXmax = Math.trunc(Math.trunc(label.end * this.sampleRate - sampleStart) / (this.player_fft_step * this.player_spectrum_shrink_Factor));
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
        const delta = this.mouseModus === 'move_time' ? pixelDelta : 0;
        ctx.fillRect(this.labelStartPixelX + delta, 0, this.labelEndPixelX - this.labelStartPixelX + 1, this.canvasHeight);
      }      
    },
    onMousedown(e) {
      if(e.buttons == 1) {
        if(e.shiftKey) {
          console.log('shift');
          this.mouseModus = 'move_time';
          const x = e.offsetX;
          this.mouseMoveTimeStartX = x;
          this.mouseMoveTimeEndX = x;
        } else {
          this.mouseModus = 'set_label';
          //console.log('onMousedown');
          //console.log(e);
          const x = e.offsetX;
          this.mouseStartX = x;
          this.mouseEndX = x;
          this.repaint();
        }
      }
    },
    onMousemove(e) {
      const x = e.offsetX;
      const y = e.offsetY;
      this.mousePixelPosX = x;
      this.mousePixelPosY = this.canvasHeight - 1 - y;
      if(this.mouseModus === 'set_label' && e.buttons == 1) {
        if(this.mouseStartX !== undefined) {        
          this.mouseEndX = x;
        }
      } else {
        this.mouseStartX = undefined;
        this.mouseEndX = undefined;
      }
      if(this.mouseModus === 'move_time' && e.buttons == 1) {
        if(this.mouseMoveTimeStartX !== undefined) {        
          this.mouseMoveTimeEndX = x;
        }
      } else {
        this.mouseMoveTimeStartX = undefined;
        this.mouseMoveTimeEndX = undefined;
      }
      this.repaint();
    },
    onMouseup(e) {
      if(this.mouseModus === 'set_label' && this.mouseStartX !== undefined) {
        console.log('onMouseup');
        //console.log(e);
        const x = e.offsetX;
        this.mouseEndX = x;
        this.labelStartX = this.pixelPosToSamplePos(this.mouseStartX);
        this.labelEndX = this.pixelPosToSamplePos(this.mouseEndX);
        console.log(this.start_sample + '  ' + x + '  ' + this.pixelPosToSamplePos(x));
        this.mouseStartX = undefined;
        this.mouseEndX = undefined;
        this.repaint();
      }
      if(this.mouseModus === 'move_time' && this.mouseMoveTimeStartX !== undefined) {
        const x = e.offsetX;
        this.mouseMoveTimeEndX = x;
        const moveTimeSampleStartX = this.pixelPosToSamplePos(this.mouseMoveTimeStartX);
        const moveTimeSampleEndX = this.pixelPosToSamplePos(this.mouseMoveTimeEndX);
        const diff = moveTimeSampleEndX - moveTimeSampleStartX;
        if(diff !== 0) {
          this.samplePos = this.start_sample - diff;
          this.mouseModus = undefined;
          this.refresh();
          this.repaint();
        }
        this.mouseMoveTimeStartX = undefined;
        this.mouseMoveTimeEndX = undefined;
      }
      this.mouseModus = undefined;
    },
    onMouseleave() {
      this.mousePixelPosX = undefined;
      this.mousePixelPosY = undefined;
    },
    pixelPosToSamplePos(x) {
      if(this.start_sample === undefined || x === undefined || !this.player_fft_step || !this.player_spectrum_shrink_Factor) {
        return undefined;
      }
      return this.start_sample + x * (this.player_fft_step * this.player_spectrum_shrink_Factor);
    },
    samplePosToPixelPos(x) {
      if(this.start_sample === undefined || x === undefined || !this.player_fft_step || !this.player_spectrum_shrink_Factor) {
        return undefined;
      }
      return (x - this.start_sample) / (this.player_fft_step * this.player_spectrum_shrink_Factor);      
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
      this.mouseStartX = undefined;
      this.mouseEndX = undefined;
      this.labelStartX = undefined;
      this.labelEndX = undefined;
    },
    onOverlapMenuHide() {
      if(this.userWindowOverlapPercentChanged) {
        //console.log('hidden');
        this.userWindowOverlapPercentChanged = false;
        this.windowOverlapFactor = (100 / (100 - this.userWindowOverlapPercent));
        this.refresh();
      }
    },
    onMovePrevSamples() {
      this.samplePos = this.start_sample - this.moveSampleStep;
      this.refresh();
      this.repaint();
    },
    onMoveNextSamples() {
      this.samplePos = this.start_sample + this.moveSampleStep;
      this.refresh();
      this.repaint();
    },
  },
  watch: {
    show() {
      if(this.show) {
        this.$nextTick( () => {
          const card = document.getElementById('card');
          if(card !== undefined && card.clientWidth) {
            this.spectrogramImageMaxPixelLenUnaligned = card.clientWidth;
          }
          this.refresh();
        });
      }
    },
    labels() {
      this.repaint();
    },
    hideLabels() {
      this.repaint();
    },
    detail_fft_window_overlap_percent: {
      immediate: true,
      handler() {
        this.userWindowOverlapPercentChanged = true;
        this.userWindowOverlapPercent = this.detail_fft_window_overlap_percent;
        this.onOverlapMenuHide();
      },
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
