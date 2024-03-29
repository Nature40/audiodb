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
          <v-list-tile-title><a href="#/review"><v-icon>arrow_forward</v-icon>review lists</a></v-list-tile-title>
        </v-list-tile>
        <v-list-tile>
        <v-list-tile-title><a href="#/labeling"><v-icon>arrow_forward</v-icon>labeling lists</a></v-list-tile-title>
        </v-list-tile>        
        <v-list-tile>
          <v-list-tile-title><a href="../photo"><v-icon>arrow_forward</v-icon>switch to PhotoDB</a></v-list-tile-title>
        </v-list-tile>                   
      </v-list>
    </v-menu>     
    <v-toolbar-title class="headline text-uppercase">
      Audio
    </v-toolbar-title>
    <span v-if="!isReviewedOnly">
    <audio-browser :selected-sample="selectedSample" @select-sample="selectedSample = $event; updateRoute();"/> 
    <v-btn icon v-show="samplesPrevious(selectedSample) !== undefined" title="select previous audio sample" @click="selectedSample = samplesPrevious(selectedSample); updateRoute();"><v-icon>skip_previous</v-icon></v-btn>
    <span v-if="selectedSample !== undefined" style="font-size: 1.5em; background-color: #0000000a; padding: 2px;" title="currently selected audio sample"><b><v-icon>place</v-icon> {{selectedSample.location}} </b> <span><v-icon>date_range</v-icon> {{toDate(selectedSample.datetime)}} </span> <span style="color: grey;"><v-icon>access_time</v-icon> {{toTime(selectedSample.datetime)}}</span></span>
    <v-btn icon v-show="samplesNext(selectedSample) !== undefined" title="select next audio sample" @click="selectedSample = samplesNext(selectedSample); updateRoute();"><v-icon>skip_next</v-icon></v-btn>
    </span>
    <span v-if="isReadOnly" style="color: #e11111; padding-left: 10px;">readOnly</span>
    <span v-if="isReviewedOnly" style="color: #e11111; padding-left: 10px;">reviewedOnly</span>
    <div style="display: flex; position: absolute; right: 2px;" >
      <identity-dialog />
    </div>
  </v-toolbar>

  <v-content v-if="!isReviewedOnly">
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
        Web application compatibility is tested on up-to-date <b>Mozilla Firefox</b>.
        <br>
        <br>
        Limitations on <b>Google Chrome</b>:
        <ul>
          <li>Audio pitch is always constant for different playback rates. (Settings - Playback rate - unchecked box preserve pitch has no effect)</li>
        </ul>
        </div>      
      </i>
      <br>
      <br>
      <a href="../photo"><v-icon>arrow_forward</v-icon>switch to PhotoDB</a>
    </div>
  </v-content>

  <v-content v-if="isReviewedOnly">
    <br>
    <br>
    <h1 style="color: red;">Your account is allowed to access reviewed audio samples only.</h1>
    <br>
    So, you can not selected audio semples here.
    <br>
    Navigate to the '<b>review</b>'-page by the button on the top left of this page to select reviewed audio samples.
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
props: ['routerSample'],
data () {
  return {
    selectedSample: undefined,
  }
},
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    identity: state => state.identity.data,
    samples: state => state.samples.data,
  }),
  ...mapGetters({
    samplesPrevious: 'samples/previous',
    samplesNext: 'samples/next',
    findById: 'samples/findById',
    isReadOnly: 'identity/isReadOnly',       
    isReviewedOnly: 'identity/isReviewedOnly', 
  }),    
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
  moveToRoute() {
    if(this.routerSample !== undefined && this.samples.length > 0 && (this.selectedSample === undefined || this.selectedSample.id !== this.routerSample)) {
      console.log("move to route " + this.routerSample);
      this.selectedSample = this.findById(this.routerSample);
      this.updateRoute();
    }
  },
  updateRoute() {
    this.$nextTick(() => {
      if(this.selectedSample === undefined) {
        this.$router.push({ path: 'audio' })
      } else {
        this.$router.push({ path: 'audio', query: { sample: this.selectedSample.id } })      
      }
    });
  },  
},
watch: {
  routerSample: {
    immediate: true,
    handler() {
      this.moveToRoute();
    },
  },
  samples() {
    this.moveToRoute();
  },
},
mounted() {
  this.identityInit();
},
}
</script>
