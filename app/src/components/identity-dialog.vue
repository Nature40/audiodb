<template>
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
        <v-list-tile-title><a href="#/account"><v-icon>arrow_forward</v-icon>Account</a></v-list-tile-title>
      </v-list-tile>       
      <v-list-tile>
        <v-list-tile-title><a :href="apiBase + 'logout'"><v-icon>call_end</v-icon>Sign out</a></v-list-tile-title>
      </v-list-tile>
      </v-list>        
    </v-menu>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
name: 'identity-dialog',
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
},
mounted() {
  this.identityInit();
},
}
</script>
