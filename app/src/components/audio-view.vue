<template>
<v-app>
  <v-toolbar app>
    <v-toolbar-title class="headline text-uppercase">
      Audio
    </v-toolbar-title> 
    &nbsp;&nbsp;&nbsp;<multiselect v-model="selectedSample" :options="samples" :loading="samplesLoading" label="name" style="max-width: 500px;" placeholder="select audio sample" :allowEmpty="false"/>    
    <div style="position: absolute; right: 2px;" v-if="identity !== undefined">
      <v-menu>
        <template v-slot:activator="{on}">
          <v-btn v-on="on" icon outline><v-icon>perm_identity</v-icon></v-btn>
        </template>
        <v-list>
        <v-list-tile>
          <v-list-tile-title>Signed in as <b>{{identity.user}}</b></v-list-tile-title>
        </v-list-tile>
        <v-list-tile>
          <v-list-tile-title><a :href="apiBase + 'logout'"><v-icon>call_end</v-icon>Sign out</a></v-list-tile-title>
        </v-list-tile>
        </v-list>        
      </v-menu>
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

import { mapState, mapActions } from 'vuex'
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
  var self = this;
  axios.get(self.apiBase + 'samples')
  .then(function(response) {
      self.samples = response.data.samples;
      self.samplesLoading = false;
  });
  this.identityInit();
},
}
</script>
