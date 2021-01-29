<template>
  <div>
    <v-menu transition="scale-transition">
      <template v-slot:activator="{on}">
        <v-btn v-on="on" icon title="make a note"><v-icon>emoji_people</v-icon></v-btn>
      </template>
      <v-list>
      <v-list-tile @click="onLock">
        <v-list-tile-title><v-icon>lock</v-icon> Lock this audio sample. (e.g. I heard a human.)</v-list-tile-title>
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
},
mounted() {
  this.identityInit();
},
}
</script>
