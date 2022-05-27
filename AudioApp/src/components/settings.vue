<template>
  <q-dialog v-model="show" style="width: 1000px;">
      <q-card style="width: 1000px;">
        <q-bar>
          <q-icon name="tune"/>
          <div>Settings</div>
          <q-space />
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>     

        <q-card-section>
          <q-badge color="grey-6">
            Sampling window 
            <q-btn
              dense
              color="blue-10"
              size="xs"
              icon-right="undo"
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
        <q-separator/>
        <q-card-section>
          <q-badge color="grey-6">
            Intensity range of interest
            <q-btn
              dense
              color="blue-10"
              size="xs"
              icon-right="undo"
              :label="default_player_spectrum_threshold + ' .. ' + default_player_fft_intensity_max"
              title="reset to default"
              @click="user_player_fft_intensity_range.min = default_player_spectrum_threshold; user_player_fft_intensity_range.max = default_player_fft_intensity_max;"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-range
            v-model="user_player_fft_intensity_range"
            :min="user_player_fft_intensity_range_min"
            :max="user_player_fft_intensity_range_max"
            :step="0.1"
            dense
            style="margin-top: 30px;"
            label-always
            snap
            :left-label-value="user_player_fft_intensity_range.min.toFixed(1)"
            :right-label-value="user_player_fft_intensity_range.max.toFixed(1)"
        	/>
        </q-card-section>
        <q-separator/>
        <q-card-section>
          <q-badge color="grey-6">
            Frequency range of interest (kHz)
            <q-btn
              dense
              color="blue-10"
              size="xs"
              icon-right="undo"
              :label="(default_player_fft_cutoff_lower_frequency / 1000).toFixed(1) + ' .. ' + (default_player_fft_cutoff_upper_frequency / 1000).toFixed(1)"
              title="reset to default"
              @click="user_player_fft_cutoff_range.min = default_player_fft_cutoff_lower_frequency; user_player_fft_cutoff_range.max = default_player_fft_cutoff_upper_frequency;"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-range
            v-model="user_player_fft_cutoff_range"
            :min="user_player_fft_cutoff_range_min"
            :max="user_player_fft_cutoff_range_max"
            :step="0.1"
            dense
            style="margin-top: 30px;"
            label-always
            snap
            :left-label-value="(user_player_fft_cutoff_range.min / 1000).toFixed(1)"
            :right-label-value="(user_player_fft_cutoff_range.max / 1000).toFixed(1)"
        	/>
        </q-card-section>
        <q-separator/>
        <q-card-section>
          <q-badge color="grey-6">
            Spectrogram time contraction factor 
            <q-btn
              dense
              color="blue-10"
              size="xs"
              icon-right="undo"
              :label="default_player_spectrum_shrink_Factor"
              title="reset to default"
              @click="user_player_spectrum_shrink_Factor = default_player_spectrum_shrink_Factor"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-slider
            v-model="user_player_spectrum_shrink_Factor"
            :min="1"
            :max="256"
            :step="1"
            label-always
            :label-value="user_player_spectrum_shrink_Factor"
            dense
            style="margin-top: 30px;"
            snap
          />
        </q-card-section>
        <q-separator/>
        <q-card-section>
          <q-badge color="grey-6">
            Player time expansion factor 
            <q-btn
              dense
              color="blue-10"
              size="xs"
              icon-right="undo"
              :label="default_player_time_expansion_factor"
              title="reset to default"
              @click="user_player_time_expansion_factor = default_player_time_expansion_factor"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-slider
            v-model="user_player_time_expansion_factor"
            :min="1"
            :max="10"
            :step="1"
            label-always
            :label-value="user_player_time_expansion_factor"
            dense
            style="margin-top: 30px;"
            markers
            snap
          />
        </q-card-section>                        
        <q-separator/>
        <q-card-section>
          <q-badge color="grey-6">
            Player Frequency orientation lines.
            <q-btn
              dense
              color="blue-10"
              size="xs"
              icon-right="undo"
              :label="default_player_static_lines_frequency === undefined ? '-' : default_player_static_lines_frequency.join(', ')"
              title="reset to default"
              @click="user_player_static_lines_frequency = default_player_static_lines_frequency"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-input square outlined v-model="user_player_static_lines_frequency" label="Comma separated list of frequencies in Hz." error-message="Error in input: One or multiple comma separated numbers are needed, or empty input." :error="!valid_lines_frequency"/>
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
      user_player_fft_window: 10, //2^10=1024
      user_player_fft_window_min: 8,
      user_player_fft_window_max: 16,
      user_player_fft_intensity_range: {min: 20, max: 33},
      user_player_fft_intensity_range_min: 10,
      user_player_fft_intensity_range_max: 40,
      user_player_spectrum_shrink_Factor: 1,
      user_player_time_expansion_factor: 1,
      user_player_static_lines_frequency: '',
      user_player_fft_cutoff_range: {min: 1000, max: 5000},
      user_player_fft_cutoff_range_min: 0,
      user_player_fft_cutoff_range_max: 192000,      
    };
  },
  computed: {
    ...mapState({
      player_fft_window: state => state.project.player_fft_window,
      default_player_fft_window: state => state.project.default_player_fft_window,
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      default_player_spectrum_threshold: state => state.project.default_player_spectrum_threshold,      
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
      default_player_fft_intensity_max: state => state.project.default_player_fft_intensity_max,
      player_spectrum_shrink_Factor: state => state.project.player_spectrum_shrink_Factor,
      default_player_spectrum_shrink_Factor: state => state.project.default_player_spectrum_shrink_Factor,
      player_time_expansion_factor: state => state.project.player_time_expansion_factor,
      default_player_time_expansion_factor: state => state.project.default_player_time_expansion_factor,
      player_static_lines_frequency: state => state.project.player_static_lines_frequency,
      default_player_static_lines_frequency: state => state.project.default_player_static_lines_frequency,   
      player_fft_cutoff_lower_frequency: state => state.project.player_fft_cutoff_lower_frequency,
      default_player_fft_cutoff_lower_frequency: state => state.project.default_player_fft_cutoff_lower_frequency,
      player_fft_cutoff_upper_frequency: state => state.project.player_fft_cutoff_upper_frequency,        
      default_player_fft_cutoff_upper_frequency: state => state.project.default_player_fft_cutoff_upper_frequency,
    }),
    valid_lines_frequency() {
      let u = this.user_player_static_lines_frequency;
      console.log(u);
      console.log(u === undefined);
      if(u === undefined || u === null ) {
        return true;
      }
      u = u + '';
      if(u.trim() === '' ) {
        return true;
      }
      let us = u.split(',').map(text => text.trim());
      us = us.map(text => Number.parseFloat(text));
      return us.every(v => Number.isFinite(v));
    },        
  },
  methods: {
    user_player_static_lines_frequency_to_text() {
      if(!this.valid_lines_frequency) {
        return undefined;
      }
      let u = this.user_player_static_lines_frequency;
      if(u === undefined || u === null) {
        return [];
      }
      u = u + '';
      u = u.trim();
      if(u === '' ) {
        return [];
      }
      let us = u.split(',').map(text => text.trim());
      us = us.map(text => Number.parseFloat(text));
      return us;
    },
    onApply() {
      var settings = {};
      settings.player_fft_window = 2**this.user_player_fft_window;
      settings.player_spectrum_threshold = this.user_player_fft_intensity_range.min;      
      settings.player_fft_intensity_max = this.user_player_fft_intensity_range.max;
      settings.player_spectrum_shrink_Factor = this.user_player_spectrum_shrink_Factor;
      settings.player_time_expansion_factor = this.user_player_time_expansion_factor;
      console.log('this.user_player_static_lines_frequency_to_text()');
      console.log(this.user_player_static_lines_frequency_to_text());
      settings.player_static_lines_frequency = this.user_player_static_lines_frequency_to_text();
      settings.player_fft_cutoff_lower_frequency = this.user_player_fft_cutoff_range.min;      
      settings.player_fft_cutoff_upper_frequency = this.user_player_fft_cutoff_range.max;
      this.$store.dispatch('project/set', settings); 
    },
    refresh() {
      this.user_player_fft_window = this.toValidExp(this.player_fft_window);
      this.user_player_fft_intensity_range.min = this.player_spectrum_threshold;      
      this.user_player_fft_intensity_range.max = this.player_fft_intensity_max;      
      this.user_player_spectrum_shrink_Factor = this.player_spectrum_shrink_Factor;
      this.user_player_time_expansion_factor = this.player_time_expansion_factor;
      this.user_player_static_lines_frequency = this.player_static_lines_frequency === undefined ? undefined : this.player_static_lines_frequency.join(', ');
      this.user_player_fft_cutoff_range.min = this.player_fft_cutoff_lower_frequency;      
      this.user_player_fft_cutoff_range.max = this.player_fft_cutoff_upper_frequency; 
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
