<template>
<v-app>
  <v-toolbar app>
    <v-menu offset-y>
      <template v-slot:activator="{ on }">
        <v-toolbar-side-icon  v-on="on"></v-toolbar-side-icon>        
      </template>
      <v-list>
        <v-list-tile>
          <v-list-tile-title><b>Navigate</b></v-list-tile-title>
        </v-list-tile>
        <v-list-tile>
          <v-list-tile-title><a href="#/export"><v-icon>arrow_forward</v-icon>export</a></v-list-tile-title>
        </v-list-tile>       
      </v-list>
    </v-menu>     
    <v-toolbar-title class="headline text-uppercase">
      Audio
    </v-toolbar-title>
    <audio-browser v-if="samples !== undefined && samples !== []" :samples="samples" @select-sample="selectedSample = $event"/> 
    &nbsp;&nbsp;&nbsp;<multiselect v-model="selectedSample" :options="samples" :loading="samplesLoading" label="id" style="max-width: 1000px;" placeholder="select audio sample" :allowEmpty="false"/>    
    <audio-meta v-if="selectedSample !== undefined" :sample="selectedSample"/>
    <identity-dialog></identity-dialog>
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
import identityDialog from './identity-dialog'
import audioMeta from './audio-meta'
import audioBrowser from './audio-browser'

import { mapState, mapActions } from 'vuex'
import axios from 'axios'

export default {
name: 'audio-view',
components: {
  player,
  identityDialog,
  audioMeta,
  audioBrowser
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
    apiBase: state => state.apiBase,
    identity: state => state.identity.data,
  }),
},
methods: {
  ...mapActions({
    identityInit: 'identity/init',
  }),
},
mounted() {
  axios.get(this.apiBase + 'samples')
  .then(response => {
      this.samples = response.data.samples;
      this.samplesLoading = false;
      this.samples.forEach(sample => sample.datetime = new Date(sample.timestamp * 1000));
  });
  this.identityInit();
},
}
</script>
