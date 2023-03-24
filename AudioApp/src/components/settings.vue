<template>
  <q-dialog v-model="show" style="width: 1000px;" :maximized="dialogMaximizedToggle">
      <q-card style="width: 1000px;">
        <q-bar>
          <q-icon name="tune"/>
          <div>Settings</div>
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
                <div class="text-h6">Change settings</div>                
                <ol>
                  <li>Change individual settings</li>
                  <li>To apply the changes, Click 'apply'-button.</li> 
                  <li>To discard changes, click 'close'-button or press 'Esc'-key or click outside of the settings-box.</li>
                  <li>To revert to default value of an individual settings, click the reset-button at that setting.</li>
                  <li>Changes are applied at the current session only. Page refresh discards all settings changes.</li>
                </ol>
              </q-card-section>
              
              <q-card-section class="q-pt-none">
                <div class="text-h6">Sampling window (spectrogram)</div>                
                <i>Window size of fast Fourier transform (FFT).</i>
              </q-card-section>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Intensity range of interest (spectrogram)</div>
                <i>Minimum and maximum of frequency intensity.</i>
                <br>Intensity values lower than minimum are mapped to black.
                <br>Intensity values higher than maximum are mapped to white.
              </q-card-section>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Frequency range of interest (spectrogram)</div>
                <i>Minimum and maximum of frequencies included in spectrogram.</i>
              </q-card-section>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Time contraction factor (spectrogram)</div>
                <i>Spectrograms of long audio can be shrinked.</i>
                <br>Value of 1 is the regular spectrogram.
                <br>For e.g. value of 20, in one pixel column 20 pixels of regular spectrogram are combined.
              </q-card-section>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Frequency orientation lines (spectrogram)</div>
                <i>Draw lines at specified frequencies on the spectrogram for orientation.</i>
              </q-card-section>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Time expansion factor (audio playback)</div>
                <i>Slowdown audio playback.</i>
                <br>Value of 1 is actual playback speed.
                <br>Value of e.g. 10 may be suitable for ultrasonic bat recordings.
              </q-card-section>
              
              <q-card-section class="q-pt-none">
                <div class="text-h6">Mouse move factor (speedup on moving in time)</div>
                <i>On the spectrogram move in time faster than actual mouse move.</i>
                <br>Value of 0.5 is half of the mouse move speed. (more sprecise positioning)
                <br>Value of 1 is same as the actual mouse move speed. (spectrogram and mouse are in sync)
                <br>Value of 20 is twenty times faster move than actual mouse move speed. (rough positioning)
              </q-card-section>
            </q-card>
          </q-dialog>
          <q-btn dense flat icon="window" @click="dialogMaximizedToggle = false" v-show="dialogMaximizedToggle">
            <q-tooltip v-if="dialoghelpMaximizedToggle">Minimize</q-tooltip>
          </q-btn>
          <q-btn dense flat icon="crop_square" @click="dialogMaximizedToggle = true" v-show="!dialogMaximizedToggle">
            <q-tooltip v-if="!dialoghelpMaximizedToggle">Maximize</q-tooltip>
          </q-btn>          
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>     

        <q-card-section style="background-color: #0000000d;">
            <b>Profile</b>
            <q-select 
              v-model="selectedProfileId" 
              dense 
              options-dense 
              hide-bottom-space 
              filled 
              :options="profileIds" 
              style="min-width: 100px;" 
              title="Select a profile to load the settings values from that profile."
            />
        </q-card-section>
        <q-separator/>
        <q-separator/>
        <q-separator/>

        <q-card-section>
          <q-badge color="grey-6">
            Sampling window (spectrogram)
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
            Intensity range of interest (spectrogram)
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
            Frequency range of interest kHz (spectrogram)
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
            Time contraction factor (spectrogram) 
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
            Frequency orientation lines (spectrogram)
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
        <q-separator/>
        <q-card-section>
          <q-badge color="grey-6">
            Time expansion factor (audio playback)
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
            Mouse move factor (speedup on moving in time by mouse)
            <q-btn
              dense
              color="blue-10"
              size="xs"
              icon-right="undo"
              :label="default_player_mouse_move_factor"
              title="reset to default"
              @click="user_player_mouse_move_factor = default_player_mouse_move_factor"
              style="margin-left: 10px;"
            />
          </q-badge>
          <q-slider
            v-model="user_player_mouse_move_factor"
            :min=".5"
            :max="20"
            :step="0.5"
            label-always
            :label-value="user_player_mouse_move_factor"
            dense
            style="margin-top: 30px;"
            markers
            snap
          />
        </q-card-section>                  

        <q-separator />
        <q-separator />
        <q-separator />

        <q-card-actions align="right" style="background-color: #0000000d;">
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
      user_player_mouse_move_factor: 1,
      dialoghelpShow: false,
      dialoghelpMaximizedToggle: false,
      dialogMaximizedToggle: false, 
      selectedProfileId: undefined,         
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
      player_mouse_move_factor: state => state.project.player_mouse_move_factor,
      default_player_mouse_move_factor: state => state.project.default_player_mouse_move_factor,      
      profileIds: state => state.project.profileIds,
      profileId: state => state.project.profileId,
    }),
    valid_lines_frequency() {
      let u = this.user_player_static_lines_frequency;
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
      settings.player_mouse_move_factor = this.user_player_mouse_move_factor;
      this.$store.dispatch('project/set', settings); 
    },
    refresh() {
      this.user_player_fft_window = this.toValidExp(this.player_fft_window);
      this.user_player_fft_cutoff_range.min = this.player_fft_cutoff_lower_frequency;      
      this.user_player_fft_cutoff_range.max = this.player_fft_cutoff_upper_frequency;         
      this.user_player_fft_intensity_range.min = this.player_spectrum_threshold;      
      this.user_player_fft_intensity_range.max = this.player_fft_intensity_max;         
      this.user_player_spectrum_shrink_Factor = this.player_spectrum_shrink_Factor;
      this.user_player_time_expansion_factor = this.player_time_expansion_factor;
      this.user_player_static_lines_frequency = this.player_static_lines_frequency === undefined ? undefined : this.player_static_lines_frequency.join(', ');
      this.user_player_mouse_move_factor = this.player_mouse_move_factor;
    },
    toValidExp(window) {
      var exp = Math.trunc(Math.log2(window));
      return exp < this.user_player_fft_window_min ? this.user_player_fft_window_min : (exp > this.user_player_fft_window_max ? this.user_player_fft_window_max : exp);
    },
    setProfile(id) {
      this.$store.dispatch('project/setProfile', id);
    }
  },
  watch: {
    show() {
      if(this.show) {
        this.refresh();
      }
    },
    profileIds() {
      if(this.profileIds && this.profileIds.length !== 0) {
        this.selectedProfileId = this.profileIds[0];
      }
    },
    profileId: {
      handler() {
        if(this.profileId !== this.selectedProfileId) {
          this.selectedProfileId = this.profileId;
        }
      },
      immediate: true,
    },
    selectedProfileId() {
      if(this.profileId !== this.selectedProfileId) {
        this.setProfile(this.selectedProfileId);
      }
    },
    player_fft_window() {this.refresh();},
    player_fft_cutoff_lower_frequency() {this.refresh();},
    player_fft_cutoff_upper_frequency() {this.refresh();},
    player_spectrum_threshold() {this.refresh();},
    player_fft_intensity_max() {this.refresh();},
    player_spectrum_shrink_Factor() {this.refresh();},
    player_time_expansion_factor() {this.refresh();},
    player_static_lines_frequency() {this.refresh();},
    player_mouse_move_factor() {this.refresh();},
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
