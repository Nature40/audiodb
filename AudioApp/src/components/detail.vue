<template>
  <q-dialog v-model="show" full-width>
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

        <!--<q-card-section>-->
          <canvas ref="spectrogram" :width="canvasWidth" :height="canvasHeight" :style="{width: canvasWidth + 'px', height: canvasHeight + 'px'}" class="spectrogram"/>
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
      player_spectrum_shrink_Factor: state => state.project.player_spectrum_shrink_Factor,      
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
    spectrogramSettingsQuery() {
      var fft_window = this.player_fft_window;
      //var fft_window = 4096;
      var q = "&cutoff_lower=" + this.player_fft_cutoff_lower 
      + "&cutoff=" + this.player_fft_cutoff 
      + "&step=" + (fft_window / 4)
      + "&window=" + fft_window
      + "&threshold=" + this.player_spectrum_threshold 
      + "&intensity_max=" + this.player_fft_intensity_max;
      return q;
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
      console.log('repaint'); 
      var canvas = this.$refs.spectrogram;
      var ctx = canvas.getContext("2d");
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      if(this.image !== undefined) {
        console.log('drawImage'); 
        console.log(this.image);
        ctx.drawImage(this.image, 0, 0);          
      }      
    },
  },
  watch: {
    show() {
      if(this.show) {
        this.refresh();
      }
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
