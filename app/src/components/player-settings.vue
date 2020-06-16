<template>
<v-dialog v-model="dialog" width="700">
      <template v-slot:activator="{ on }">
        <v-btn v-on="on" color="grey">
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
        <v-flex shrink style="width: 60px">
          no filtering
        </v-flex>

        <v-flex shrink style="width: 200px">
          <v-slider
            v-model="user_threshold"
            :min="0"
            :max="20"
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
}),
computed: {
  ...mapState({
    apiBase: 'apiBase',
    threshold: state => state.settings.player_spectrum_threshold,
    threshold_default: state => state.settings.player_spectrum_threshold_default,
  }),
},  
methods: {
  onApply() {
    var settings = {};
    settings.player_spectrum_threshold = this.user_threshold;
    this.$store.commit('settings/set', settings);
    this.dialog = false;
  }
},
watch: {
  threshold() {
    this.user_threshold = this.threshold;
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


