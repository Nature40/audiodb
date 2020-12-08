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
          <v-list-tile-title><a href="#/audio"><v-icon>arrow_forward</v-icon>audio</a></v-list-tile-title>
        </v-list-tile> 
        <v-list-tile>
          <v-list-tile-title><a href="#/export"><v-icon>arrow_forward</v-icon>export</a></v-list-tile-title>
        </v-list-tile>               
      </v-list>
    </v-menu> 
    <v-toolbar-title class="headline text-uppercase">
      Classification:  
    </v-toolbar-title> 
    <div style="margin-left: 10px;"><v-select v-model="classification_label" :items="classification_labels" label="Label" solo /></div>
    <identity-dialog></identity-dialog>
  </v-toolbar>

  <v-content>
  <v-layout align-center justify-start column fill-height>
      <div v-if="samples !== undefined && sample !== undefined">
        <v-btn @click="movePrevSample"><v-icon>fast_rewind</v-icon></v-btn>
        <b>[Recording {{sampleIndex+1}} / {{samples.length}}]</b>  {{sample === undefined ? '-' : sample.id}}
        <v-btn @click="moveNextSample"><v-icon>fast_forward</v-icon></v-btn>
      </div>
      <div v-if="labels !== undefined && label !== undefined ">
        [Arrow Left]
        <v-btn @click="movePrevLabel"><v-icon>skip_previous</v-icon></v-btn>
        <b>[Slice {{labelIndex+1}} / {{labels.length}}]</b>  {{label.start.toFixed(2) + ' - ' + label.end.toFixed(2)}}
        <v-btn @click="moveNextLabel"><v-icon>skip_next</v-icon></v-btn>
        [Arrow Right]
      </div>
      <div v-if="sample !== undefined">
        <audio ref="audio" :src="apiBase + 'samples/'+ sample.id + '/data'" type="audio/wav" preload="auto" />
        <img :src="spectrogramUrl" class="spectrogram" draggable="false"/>
      </div>
      <div class="review-label">
        {{classification_label}}
      </div>
      <div class="controls" v-if="label !== undefined">
        <div :class="{ 'reviewed-selected': storedReviewed === 'no' }"><v-btn @click="setReviewed('no')" color="red"><v-icon dark>clear</v-icon> NO</v-btn></div>
        <div :class="{ 'reviewed-selected': storedReviewed === 'unsure' }"><v-btn @click="setReviewed('unsure')" color="yellow"><v-icon dark>code</v-icon> UNSURE</v-btn></div> 
        <div :class="{ 'reviewed-selected': storedReviewed === 'yes' }"><v-btn @click="setReviewed('yes')" color="green"><v-icon dark>done</v-icon> YES</v-btn></div>
        <div><v-btn @click="replayAudio()" icon title="replay audio"><v-icon dark>replay</v-icon></v-btn></div>
        <div>[Esc]</div>
        <div>[Enter]</div>
        <div>[Space]</div>
        <div>[Tab]</div>    
      </div>
  </v-layout>
  </v-content>
</v-app>
</template>

<script>

import { mapState, mapGetters, mapActions } from 'vuex'
import axios from 'axios'
import YAML from 'yaml'

import identityDialog from './identity-dialog'

export default {
name: 'classification-view',
components: {
  identityDialog,
},
data () {
  return {
    sampleIndex: undefined, 
    sampleMeta: undefined,
    labels: undefined,
    labelIndex: undefined,   
    canvasHeight: 512,
    classification_labels: ['Amsel', 'Amsel-Warnruf', 'Buchfink', 'Gartenbaumlaeufer', 'Kernbeisser', 'Misteldrossel', 'Moenchsgrasmuecke', 'Ringeltaube', 'Rotkehlchen', 'Singdrossel', 'Sommergoldhaehnchen', 'Tannenmeise', 'Waldbaumlaeufer', 'Zaunkoenig', 'Zilpzalp'],
    classification_label: 'Amsel',
    animationFrameCallback: undefined,
    animationFrameID: undefined,
  }
},
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    samples: state => state.samples.data,
    samplesLoading: state => state.samples.loading,
    samplesError: state => state.samples.error, 
    threshold: state => state.settings.player_spectrum_threshold,   
  }),
  ...mapGetters({
      samplesIsError: 'samples/isError',
  }),
  sample() {
    if(this.sampleIndex === undefined || this.samples === undefined || this.sampleIndex >= this.samples.length) {
      return undefined;
    }
    return this.samples[this.sampleIndex];
  },
  label() {
    if(this.labelIndex === undefined || this.labels === undefined || this.labelIndex >= this.labels.length) {
      return undefined;
    }
    return this.labels[this.labelIndex];
  },
  spectrogramUrl() {
    if(this.sample === undefined || this.label === undefined) {
      return undefined;
    }
    return this.apiBase + 'samples/' + this.sample.id + '/spectrum' + '?cutoff=' + this.canvasHeight + "&threshold=" + this.threshold + "&start=" + this.label.start + "&end=" + this.label.end;
  },
  storedReviewed() {
    if(this.label === undefined) {
      return undefined;
    }
    var r = this.label.reviewed_labels.find(r => r.name === this.classification_label);
    return r === undefined ? undefined : r.reviewed;
  },  
},
watch: {
  samples() {
    this.sampleIndex = 0;
    this.labelIndex = 0;
  },
  sample() {
    this.sampleMeta = undefined;
    if(this.samples === undefined) {
      return;
    }
    axios.get(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'meta')
    .then(response => {
      var data = response.data;
      var parsed = YAML.parse(data);
      console.log(parsed);
      this.sampleMeta = parsed.meta;
    })
    .catch(() => {
      this.sampleMeta = undefined;
    });
  },
  sampleMeta() {
    this.labels = undefined;
    if(this.samples === undefined || this.sampleMeta === undefined) {
      return;
    }
    axios.get(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels')
    .then(response => {
      var r = response.data.labels;
      if(r.length === 0) {
        this.labels = undefined;
        this.labelIndex = undefined;
      } else {
        this.labels = r;
        this.labelIndex = 0;
      }

      
    })
    .catch(() => {
      this.labels = undefined;
    });
  },
  labels() {
    //this.labelIndex = 0;
  },
  label() {
    if(this.label !== undefined) {
      this.replayAudio();
    }
  },
},
methods: {
  ...mapActions({
    samplesQuery: 'samples/query',
  }),
  refresh() {
    this.samplesQuery();
  },
  movePrevLabel() {
    if(this.labels !== undefined && this.labelIndex !== undefined) {
      var prev = this.labelIndex - 1;
      if(prev >= 0) {
        this.labelIndex = prev;
        //this.replayAudio();
      } else {
        this.movePrevSample();
      }
    }
  },
  moveNextLabel() {
    if(this.labels !== undefined && this.labelIndex !== undefined) {
      var next = this.labelIndex + 1;
      if(next < this.labels.length) {
        this.labelIndex = next;
        //this.replayAudio();
      } else {
        this.moveNextSample();
      }
    }
  },
  moveNextSample() {
    if(this.samples !== undefined && this.sampleIndex !== undefined) {
      var next = this.sampleIndex + 1;
      if(next < this.samples.length) {
        this.sampleIndex = next;
        this.labelIndex === undefined;
      }
    }
  },
  movePrevSample() {
    if(this.samples !== undefined && this.sampleIndex !== undefined) {
      var prev = this.sampleIndex - 1;
      if(prev >= 0) {
        this.sampleIndex = prev;
        this.labelIndex === undefined;
      }
    }
  },
  replayAudio() {
    requestAnimationFrame(() => {
    if(this.$refs.audio === undefined) {
      console.log("no audio");
    }
    console.log(this.$refs.audio.src);
    this.$refs.audio.pause();
    if(this.label !== undefined) {
      this.$refs.audio.currentTime = this.label.start;
      this.$refs.audio.play();
      this.requestAnimationFrame();
    } else {
      console.log("label undefined");
    }
    });
  },
  requestAnimationFrame() {
    if(this.animationFrameID === undefined) {
      this.animationFrameID = requestAnimationFrame(this.animationFrameCallback);
    }    
  },
  animationFrame() {
    this.animationFrameID = undefined;
    if(this.label !== undefined && !this.$refs.audio.paused) {
      if(this.$refs.audio.currentTime < this.label.end) {
        //console.log(this.$refs.audio.currentTime);
        this.requestAnimationFrame();
      } else {
        this.$refs.audio.pause();
      }
    }
  },
  onKeyDown(e) {
    console.log('|' + e.key + '|');
    switch(e.key) {
      case 'Tab':
        e.preventDefault();
        this.replayAudio();
        break;
      case 'Escape':
        e.preventDefault();
        this.setReviewed('no');
        break;   
      case 'Enter':
        e.preventDefault();
        this.setReviewed('unsure');
        break;      
      case ' ':
        e.preventDefault();
        this.setReviewed('yes');
        break;
      case 'ArrowLeft':
        e.preventDefault();
        this.movePrevLabel();
        break;   
      case 'ArrowRight':
        e.preventDefault();
        this.moveNextLabel();
        break;                            
    }
  },
  setReviewed(reviewed) {
    if(this.label !== undefined && this.classification_label != undefined) {
      var reviewed_label = {name: this.classification_label, reviewed: reviewed};
      axios.post(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'labels', {actions: [{action: "set_reviewed_label", start: this.label.start, end: this.label.end, reviewed_label: reviewed_label}]})
        .then((response) => {
          console.log(response);
          this.labels = response.data.labels;
          this.moveNextLabel();
        })
        .catch(() => {
        });
    }
  },  
},
mounted() {
  this.animationFrameCallback = this.animationFrame.bind(this);
  document.addEventListener('keydown', this.onKeyDown.bind(this));
  this.refresh();   
},
}
</script>

<style scoped>

.spectrogram {
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bd9;
  border-radius: 6px;
  box-shadow: 0px 0px 15px #443c319c;
  height: 512px;
}

.controls {
  display: grid;
  grid-template-columns: auto auto auto auto;
  justify-items: center;
  align-items: center;
}

.review-label {
  font-weight: bold;
  font-size: 1.5rem;
}

.reviewed-selected {
  border-style: solid;
  border-width: 1px;
  border-color: #0000;
  border-radius: 6px;
  -webkit-box-shadow: 0px 0px 15px #443c319c;
  box-shadow: 0px 0px 15px #443c319c;
  background-color: #00000012;
}

</style>
