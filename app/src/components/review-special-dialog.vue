<template>
  <div>
    <v-menu transition="scale-transition">
      <template v-slot:activator="{on}">
        <v-btn v-on="on" icon title="make a note"><v-icon>emoji_people</v-icon></v-btn>
      </template>
      <v-list>
      <v-list-tile @click="onLock">
        <v-list-tile-title><v-icon color="red">lock</v-icon> Lock this audio sample. (e.g. I heard a human.)</v-list-tile-title>
      </v-list-tile>
      <v-list-tile @click="onSampleView">
        <v-list-tile-title><v-icon>arrow_right_alt</v-icon> Open full audio sample view on a new tab. (e.g. to set labels of different species)</v-list-tile-title>
      </v-list-tile>
      </v-list>        
    </v-menu>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
name: 'review-special-dialog',
components: {
},
props: ['sampleId'],
data () {
  return {
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
  onLock() {
    this.$emit('lock-audio-sample');
  },
  onSampleView() {
    //var url =  window.location.origin + window.location.pathname + '#/audio?sample=' + this.sampleId + "&interval=1 2";
    var url =  window.location.origin + window.location.pathname + '#/audio?sample=' + this.sampleId;
    console.log(window.location);
    console.log(url);
    window.open(url, '_blank');
  }
},
mounted() {
  this.identityInit();
},
}
</script>
