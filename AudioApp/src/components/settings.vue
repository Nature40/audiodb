<template>
  <q-dialog v-model="show" style="width: 500px;">
      <q-card>
        <q-bar>
          <div>Settings</div>
          <q-space />
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>     

        <q-card-section>
          <q-badge color="blue-5">
            Spectrogram low signal threshold (noise reduction) 
            <q-btn
              round
              dense
              color="primary"
              size="xs"
              icon="undo"
              :label="default_player_spectrum_threshold"
              title="reset to default"
              @click="user_player_spectrum_threshold = default_player_spectrum_threshold"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-slider
            v-model="user_player_spectrum_threshold"
            :min="0.0"
            :max="21.0"
            :step="0.1"
            label-always
            dense
            style="margin-top: 30px;"
          />
        </q-card-section>

        <q-card-section>
          <q-badge color="blue-5">
            Sampling window 
            <q-btn
              round
              dense
              color="primary"
              size="xs"
              icon="undo"
              :label="default_player_fft_window"
              title="reset to default"
              @click="user_player_fft_window = toValidExp(default_player_fft_window)"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-slider
            v-model="user_player_fft_window"
            :min="user_player_fft_window_min"
            :max="user_player_fft_window_max"
            :step="1"
            label-always
            :label-value="2**user_player_fft_window"
            dense
            style="margin-top: 30px;"
            markers
            snap
          />
        </q-card-section>

        <q-card-section>
          <q-badge color="blue-5">
            Intensity maximum 
            <q-btn
              round
              dense
              color="primary"
              size="xs"
              icon="undo"
              :label="default_player_fft_intensity_max"
              title="reset to default"
              @click="user_player_fft_intensity_max = toValidExp(default_player_fft_intensity_max)"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-slider
            v-model="user_player_fft_intensity_max"
            :min="user_player_fft_intensity_max_min"
            :max="user_player_fft_intensity_max_max"
            :step="1"
            label-always
            :label-value="user_player_fft_intensity_max"
            dense
            style="margin-top: 30px;"
            markers
            snap
          />
        </q-card-section>                

        <q-separator />

        <q-card-actions align="right">
          <q-btn v-close-popup flat color="primary" label="Apply" @click="onApply" />
        </q-card-actions>
      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'audio-settings',
  setup () {
    const show = ref(false);
    return {
      show,
    };
  },
  data() {
    return {  
      user_player_spectrum_threshold: 10,
      user_player_fft_window: 10, //2^10=1024
      user_player_fft_window_min: 8,
      user_player_fft_window_max: 16,
      user_player_fft_intensity_max: 23,
      user_player_fft_intensity_max_min: 20,
      user_player_fft_intensity_max_max: 30,
    };
  },
  computed: {
    ...mapState({
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      default_player_spectrum_threshold: state => state.project.default_player_spectrum_threshold,
      player_fft_window: state => state.project.player_fft_window,
      default_player_fft_window: state => state.project.default_player_fft_window,
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
      default_player_fft_intensity_max: state => state.project.default_player_fft_intensity_max,
    }),    
  },
  methods: {
    onApply() {
      var settings = {};
      settings.player_spectrum_threshold = this.user_player_spectrum_threshold;
      settings.player_fft_window = 2**this.user_player_fft_window;
      settings.player_fft_intensity_max = this.user_player_fft_intensity_max;
      console.log(settings.player_fft_window);
      this.$store.dispatch('project/set', settings); 
    },
    refresh() {
      this.user_player_spectrum_threshold = this.player_spectrum_threshold;
      this.user_player_fft_window = this.toValidExp(this.player_fft_window);
      this.user_player_fft_intensity_max = this.player_fft_intensity_max;
    },
    toValidExp(window) {
      var exp = Math.trunc(Math.log2(window));
      return exp < this.user_player_fft_window_min ? this.user_player_fft_window_min : (exp > this.user_player_fft_window_max ? this.user_player_fft_window_max : exp);
    }
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
.selected-sample {
  background-color: rgba(0, 0, 0, 0.021);
  font-weight: bold;
}
</style>
