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
        <v-list-tile>
          <v-list-tile-title><a href="#/classification"><v-icon>arrow_forward</v-icon>classification</a></v-list-tile-title>
        </v-list-tile>           
      </v-list>
    </v-menu>     
    <v-toolbar-title class="headline text-uppercase">
      Audio
    </v-toolbar-title>
    <audio-browser :selected-sample="selectedSample" @select-sample="selectedSample = $event"/> 
    <v-btn icon v-show="samplesPrevious(selectedSample) !== undefined" title="select previous audio sample" @click="selectedSample = samplesPrevious(selectedSample)"><v-icon>skip_previous</v-icon></v-btn>
    <span v-if="selectedSample !== undefined" style="font-size: 1.5em; background-color: #0000000a; padding: 2px;" title="currently selected audio sample"><b><v-icon>place</v-icon> {{selectedSample.location}} </b> <span><v-icon>date_range</v-icon> {{toDate(selectedSample.datetime)}} </span> <span style="color: grey;"><v-icon>access_time</v-icon> {{toTime(selectedSample.datetime)}}</span></span>
    <v-btn icon v-show="samplesNext(selectedSample) !== undefined" title="select next audio sample" @click="selectedSample = samplesNext(selectedSample)"><v-icon>skip_next</v-icon></v-btn>
    <identity-dialog></identity-dialog>
  </v-toolbar>

  <v-content>
    <player :sample="selectedSample" v-if="selectedSample !== undefined" />
    <div v-if="selectedSample === undefined" style="text-align: center;">
      <br>
      <br>
      <h1>No audio sample selected.</h1>
      <br>
      Click the <i>Browse-button</i> to select an audio sample.
      <br>
      <br>
      <br>
      <br>
      <br>
      <br>
      <br>
      <br>
      <br>      
      <i>
        <div style="text-align: left;">
        Web application compatibility is tested on up-to-date <b>Mozilla Firefox</b> and <b>Google Chrome</b>.
        <br>
        <br>
        Limitations on <b>Google Chrome</b>:
        <ul>
          <li>Audio pitch is always constant for different playback rates. (Settings - Playback rate - unchecked box preserve pitch has no effect)</li>
        </ul>
        </div>      
      </i>

    </div>
  </v-content>
</v-app>
</template>

<script>
import player from './player'
import identityDialog from './identity-dialog'
import audioBrowser from './audio-browser'

import { mapState, mapGetters, mapActions } from 'vuex'

const yearFormat = new Intl.DateTimeFormat('en', { year: 'numeric' });
const monthFormat = new Intl.DateTimeFormat('en', { month: '2-digit' });
const dayFormat = new Intl.DateTimeFormat('en', { day: '2-digit' });
const hourFormat = new Intl.DateTimeFormat('en', { hour: '2-digit', hour12: false });
//const minuteFormat = new Intl.DateTimeFormat('en', { minute: '2-digit' }); // no leading zero
//const secondFormat = new Intl.DateTimeFormat('en', { second: '2-digit' }); // no leading zero



export default {
name: 'audio-view',
components: {
  player,
  identityDialog,
  audioBrowser
},
data () {
  return {
    selectedSample: undefined,
  }
},
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    identity: state => state.identity.data,
  }),
  ...mapGetters({
      samplesPrevious: 'samples/previous',
      samplesNext: 'samples/next',
  })    
},
methods: {
  ...mapActions({
    identityInit: 'identity/init',
  }),
  toDate(date) {
    const year = yearFormat.format(date);
    const month = monthFormat.format(date);
    const day = dayFormat.format(date);
    return `${year}-${month}-${day}`;
  },
  toTime(date) {
    const hour = hourFormat.format(date);
    const minute = date.getMinutes().toString().padStart(2,'0');
    const second = date.getSeconds().toString().padStart(2,'0');
    return `${hour}:${minute}:${second}`;
  },  
},
mounted() {
  this.identityInit();
},
}
</script>
