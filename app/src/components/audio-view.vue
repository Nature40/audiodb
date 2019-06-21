<template>
<v-app>
  <v-toolbar app>
    <v-toolbar-title class="headline text-uppercase">
      Audio
    </v-toolbar-title> 
    &nbsp;&nbsp;&nbsp;<multiselect v-model="selectedSample" :options="samples" :loading="samplesLoading" label="name" style="max-width: 500px;" placeholder="select audio sample" :allowEmpty="false"/>    
    <div style="position: absolute; right: 2px;">
      <a :href="apiBase + 'logout'">logout</a>
    </div>
  </v-toolbar>

  <v-content>
    <player :sample="selectedSample" v-if="selectedSample !== undefined" />
    <div v-if="selectedSample === undefined" style="text-align: center;">
      <br>
      <br>
      <h1>no audio sample selected</h1>
    </div>
  </v-content>
</v-app>
</template>

<script>
import player from './player'

import { mapState } from 'vuex'
import axios from 'axios'

export default {
name: 'audio-view',
components: {
  player
},
data () {
  return {
    selectedSample: undefined,
    samples: [],
    samplesLoading: true,
  }
},
computed: {
  ...mapState({
    apiBase: 'apiBase',
  }),
},
mounted() {
  var self = this;
  axios.get(self.apiBase + 'samples')
  .then(function(response) {
      self.samples = response.data.samples;
      self.samplesLoading = false;
  })
},
}
</script>
