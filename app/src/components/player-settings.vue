<template>
<v-dialog v-model="dialog" width="700">
      <template v-slot:activator="{ on }">
        <v-btn v-on="on" color="grey" title="adjust audio player settings">
          <v-icon>settings</v-icon>
          settings
        </v-btn>
      </template>

      <v-card>
        <v-card-title class="headline grey lighten-2" primary-title>
          Settings
        </v-card-title>

<v-card-text>
      <v-layout row>
        <b>Spectrogram level of detail (noise reduction)</b>
      </v-layout>
      <v-layout row>
        <v-flex shrink style="width: 60px">
          no filtering
        </v-flex>

        <v-flex shrink style="width: 200px">
          <v-slider
            v-model="user_threshold"
            :min="0"
            :max="21"
            :step="0.1"            
          ></v-slider>
        </v-flex>

        <v-flex shrink style="width: 60px">
          strong filtering
        </v-flex>

        <v-flex shrink style="width: 60px">
          <v-text-field
            v-model="user_threshold"
            class="mt-0"
            hide-details
            single-line
            type="number"
          ></v-text-field>
        </v-flex>

        <v-flex shrink style="width: 60px">
          <v-btn @click="user_threshold = threshold_default"><v-icon>undo</v-icon> default ({{threshold_default}})</v-btn>
        </v-flex>
      </v-layout>

      <v-divider></v-divider>

      <v-layout row>
        <b>Playback rate (audio speed)</b>
        <v-checkbox v-model="user_preservesPitch" label="preserve pitch (non preserve pitch Firefox only)" style="padding-left: 30px; margin-top: 0px; padding-top: 0px;" hide-details></v-checkbox>
      </v-layout>

      <v-layout row>
        <v-flex shrink style="width: 60px">
          slow
        </v-flex>

        <v-flex shrink style="width: 200px">
          <v-slider
            v-model="user_playbackRate"
            :min="0.25"
            :max="4"
            :step="0.25"            
          ></v-slider>
        </v-flex>

        <v-flex shrink style="width: 60px">
          fast
        </v-flex>

        <v-flex shrink style="width: 60px">
          <v-text-field
            v-model="user_playbackRate"
            class="mt-0"
            hide-details
            single-line
            type="number"
          ></v-text-field>
        </v-flex>

        <v-flex shrink style="width: 60px">
          <v-btn @click="user_playbackRate = 1"><v-icon>undo</v-icon> normal speed (1)</v-btn>
        </v-flex>
      </v-layout>

      <v-divider></v-divider>

      <v-layout row>
        <b><v-checkbox v-model="user_overwriteSamplingRate" label="Overwrite sampling rate" hide-details /></b>
      </v-layout>

      <v-layout row v-show="user_overwriteSamplingRate">
        <v-flex shrink style="width: 100px">
          low bandwidth
        </v-flex>

        <v-flex shrink style="width: 200px">
          <v-slider
            v-model="user_samplingRate"
            :min="4000"
            :max="312500"
            :step="1"            
          ></v-slider>
        </v-flex>

        <v-flex shrink style="width: 100px">
          high bandwidth
        </v-flex>

        <v-flex shrink style="width: 100px">
          <v-text-field
            v-model="user_samplingRate"
            class="mt-0"
            hide-details
            single-line
            type="number"
          ></v-text-field>
        </v-flex>

      </v-layout>      

    </v-card-text>






        <v-divider></v-divider>

        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" flat @click="onApply">
            apply
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import { mapState } from 'vuex'

export default {
props: [],
data: () => ({
  dialog: false,
  user_threshold: undefined,
  user_playbackRate: undefined,
  user_preservesPitch: true,
  user_overwriteSamplingRate: false,
  user_samplingRate: undefined,
}),
computed: {
  ...mapState({
    apiBase: 'apiBase',
    threshold: state => state.settings.player_spectrum_threshold,
    threshold_default: state => state.settings.player_spectrum_threshold_default,
    playbackRate: state => state.settings.player_playbackRate,
    preservesPitch: state => state.settings.player_preservesPitch,
    overwriteSamplingRate: state => state.settings.player_overwriteSamplingRate,
    samplingRate: state => state.settings.player_samplingRate,
  }),
},  
methods: {
  onApply() {
    var settings = {};
    settings.player_spectrum_threshold = this.user_threshold;
    settings.player_playbackRate = this.user_playbackRate;
    settings.player_preservesPitch = this.user_preservesPitch;
    settings.player_overwriteSamplingRate = this.user_overwriteSamplingRate;
    settings.player_samplingRate = this.user_samplingRate;
    this.$store.commit('settings/set', settings);
    this.dialog = false;
  }
},
mounted() {

},
watch: {
  threshold: {
    immediate: true,
    handler() {
      this.user_threshold = this.threshold;
    }
  },
  playbackRate: {
    immediate: true,
    handler() {
      this.user_playbackRate = this.playbackRate;
    }
  },
  preservesPitch: {
    immediate: true,
    handler() {
      this.user_preservesPitch = this.preservesPitch;
    }
  },
  overwriteSamplingRate: {
    immediate: true,
    handler() {
      this.user_overwriteSamplingRate = this.overwriteSamplingRate;
    }
  },
  samplingRate: {
    immediate: true,
    handler() {
      this.user_samplingRate = this.samplingRate;
    }
  },
  dialog() {
    if(this.dialog) {
      this.user_threshold = this.threshold;
    }
  },
},  
  
}
</script>

<style scoped>


</style>


