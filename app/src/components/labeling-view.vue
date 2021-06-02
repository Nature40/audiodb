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
      <span style="color: red;">(! Work in progress !)</span> Labeling list:  
    </v-toolbar-title> 
    <div style="margin-left: 10px;" v-if="labeling_lists_message === undefined">
      <v-select v-model="selected_labeling_list" :items="labeling_lists" label="Labeling list" solo item-text="id">
      </v-select>
    </div>
    <div v-if="labeling_list !== undefined && labeledCount < labeling_list.entries.length">some entries left</div>
    <div v-if="labeling_list !== undefined && labeledCount === labeling_list.entries.length && !isReviewedOnly">all entries done in this list</div>
    <div style="margin-left: 10px;" v-if="labeling_lists_message !== undefined">{{labeling_lists_message}}</div>
    <span v-if="isReadOnly" style="color: #e11111; padding-left: 10px;">readOnly</span>
    <span v-if="isReviewedOnly" style="color: #e11111; padding-left: 10px;">labeledOnly</span>
    <div style="display: flex; position: absolute; right: 2px;" >
      <identity-dialog />
    </div>
  </v-toolbar>

  <v-content>

  <v-layout align-center justify-start column fill-height v-if="selected_labeling_list !== undefined">

      <div style="display: grid; justify-items: center; align-items: center;">
        <span :class="{hidden: labeling_list_pos_pre_start}" style="grid-row-start: 1; grid-column-start: 1;">
          [Arrow Left]
          <v-btn @click="movePrevLabelingListEntry"><v-icon>fast_rewind</v-icon></v-btn>
        </span>
        <span :class="{hidden: !labeling_list_pos_pre_start}" style="grid-row-start: 1; grid-column-start: 2;">
          <b>Start of list reached</b>
        </span>
        <span :class="{hidden: (labeling_list_pos_pre_start || labeling_list_pos_past_end)}" style="grid-row-start: 1; grid-column-start: 2;">
          <b>Position {{labeling_list_pos === undefined ? 0 : (labeling_list_pos + 1)}} of {{labeling_list === undefined ? 0 : labeling_list.entries.length}}</b>
        </span>        
        <span :class="{hidden: !labeling_list_pos_past_end}" style="grid-row-start: 1; grid-column-start: 2;">
          <b>End of list reached</b>
        </span>
        <span :class="{hidden: labeling_list_pos_past_end}" style="grid-row-start: 1; grid-column-start: 3;">
          <v-btn @click="moveNextLabelingListEntry"><v-icon>fast_forward</v-icon></v-btn>
          [Arrow Right]
        </span>
        <span style="grid-row-start: 1; grid-column-start: 4; margin-left: 50px; margin-top: 0px;">
          <v-switch
                v-model="skip_labeled_entries"
                label="show unlabeled entries only"
                color="success"
                hide-details
                height="1"
                style="margin-top: 0px;"
                v-if="!isReviewedOnly"
          ></v-switch> 
        </span>         
        <span style="grid-row-start: 1; grid-column-start: 5; margin-left: 50px; background: #f8fff4; color: #499d2a;">
          Labeled {{labeledCount}} <span v-if="!isReviewedOnly">of {{labeling_list === undefined ? NaN : labeling_list.entries.length}}</span>
        </span>        
      </div>

      <div>
        <span style="font-size: 1.5em; background-color: #0000000a; padding: 2px;" title="currently selected audio sample">
          <span v-if="sampleMeta !== undefined">
            <b><v-icon>place</v-icon> {{sampleMeta.location}} </b> 
            <span><v-icon>date_range</v-icon> {{toDate(sampleMeta.datetime)}} </span> 
            <span style="color: grey;"><v-icon>access_time</v-icon> {{toTime(sampleMeta.datetime)}}</span>
          </span>
          <!--<span v-if="labeling_list_entry_sample_id !== undefined">-->
            <span v-else-if="labeling_list_entry_sample_id !== undefined">
            <!--<b :class="{hidden: sampleMeta !== undefined}">{{labeling_list_entry_sample_id}}</b>-->
            Loading sample ...
          </span>
        </span>
        <span v-if="labeling_list_entry_sample_id !== undefined" style="padding-left: 100px; font-size: 1.2em;">
          {{labeling_list_entry.label_start.toFixed(2) + ' - ' + labeling_list_entry.label_end.toFixed(2)}}
        </span>
      </div>  

      <div v-if="labeling_list_entry_sample_id !== undefined && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)" style="position: relative;">
        <audio ref="audio" :src="apiBase + 'samples/'+ labeling_list_entry_sample_id + '/data'" type="audio/wav" preload="auto" />
        <div class="audio-position" :style="audioPositionStyle"></div>        
        <img ref="spectrogram" :src="spectrogramUrl" class="spectrogram" draggable="false" v-if="spectrogramUrl !== undefined" style="z-index: 0;"/>
      </div>

      <div v-if="labeling_list_entry_sample_id !== undefined && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)" class="labeling-label">
        {{labeling_list_entry.label_name}}
      </div>

      <div class="controls" v-if="labeling_list_entry_sample_id !== undefined && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)">
        <div style="min-height: 75px;">
          <multiselect 
            v-model="selectedLabelDefinitions" 
            :options="label_definitions"
            track-by="name"
            label="name"
            multiple
            :closeOnSelect="false"
            placeholder="Type to search"
            hide-selected
            ref="labelSelect"
            style="min-width: 600px;"
            @close="setLabeling()"
            @remove="setLabeling()" 
          >
          <template slot="option" slot-scope="props">
            <div><b>{{props.option.name}}</b><i style="color: grey; padding-left: 50px;">{{props.option.desc}}</i></div>
          </template>
          </multiselect>
        </div>
        <div><v-btn @click="replayAudio()" icon title="replay audio"><v-icon dark>replay</v-icon></v-btn></div>
        <div><review-special-dialog @lock-audio-sample="onLockAudioSample" v-if="!isReadOnly"/></div>
        <div>[Esc]</div>
        <div>[Tab]</div>
        <div></div>
        <div class="sending" :class="{hidden: !postLabelingSending}">{{postLabelingMessage}}</div>    
        <div class="sending-error" :class="{hidden: !postLabelingError}">{{postLabelingMessage}}</div> 
      </div>

      <!--<div>
        {{label}}
      </div>-->

      <div class="generated-labels" v-if="generated_labels !== undefined && generated_labels.length > 0 && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)">
        <div class="generated-labels-header">Model</div>
        <div class="generated-labels-header">Version</div>
        <div class="generated-labels-header">Reliability</div>    

        <template v-for="g in generated_labels">
          <div class="generated-labels-cell" :key="JSON.stringify(g)+1">{{g.generator}}</div>
          <div class="generated-labels-cell" :key="JSON.stringify(g)+2">{{g.model_version}}</div>
          <div class="generated-labels-cell generated-labels-cell-reliability" :key="JSON.stringify(g)+4">{{Math.round(g.reliability * 100)}}</div>
        </template>      
      </div>

      <div v-if="userLabelsStored" style="color: green;">
        Current labeling is stored.
      </div>
      <div v-if="!userLabelsStored && !postLabelingSending" style="color: red;">
        Current changed labeling will be stored at select close (a.o. [Esc] key) or at prev/next sample move ([Left]/[Right] arrow key).
      </div>
      <div v-if="!userLabelsStored && postLabelingSending" style="color: red;">
        Storing Current changed labeling...
      </div>

      <div v-if="sampleMeta !== undefined && sampleMeta.sample_locked !== undefined" style="margin-top: 100px;">
        <h2>This audio sample has been locked. Possibly because a human was audible.</h2>
      </div>
  </v-layout>

  <v-layout align-center justify-start column fill-height v-if="selected_labeling_list === undefined">
    no labeling list selected
  </v-layout>

  </v-content>
</v-app>
</template>

<script>

import { mapState, mapGetters, mapActions } from 'vuex'
import axios from 'axios'
import YAML from 'yaml'

import identityDialog from './identity-dialog'
import reviewSpecialDialog from './review-special-dialog'

function equals_tolerant(a, b) {
	return (a - 0.001) < b && b < (a + 0.001);
}

const yearFormat = new Intl.DateTimeFormat('en', { year: 'numeric' });
const monthFormat = new Intl.DateTimeFormat('en', { month: '2-digit' });
const dayFormat = new Intl.DateTimeFormat('en', { day: '2-digit' });
const hourFormat = new Intl.DateTimeFormat('en', { hour: '2-digit', hour12: false });

export default {
name: 'labeling-view',
components: {
  identityDialog,
  reviewSpecialDialog,
},
data () {
  return {
    sampleMeta: undefined,
    labels: undefined,
    canvasHeight: 512,
    animationFrameCallback: undefined,
    animationFrameID: undefined,
    labeling_lists: [],
    labeling_lists_message: 'init',
    selected_labeling_list: undefined,
    labeling_list: undefined,
    labeling_list_message: 'init',
    labeling_list_pos: undefined,
    skip_labeled_entries: true,
    postLabelingMessage: 'init',
    postLabelingSending: false,
    postLabelingError: false,
    audioCurrentTime: undefined,
    audioColumnsPerSecond: undefined,
    selectedLabelDefinitions: undefined,
  }
},
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    threshold: state => state.settings.player_spectrum_threshold,
    label_definitions: state => state.label_definitions === undefined ? undefined : state.label_definitions.data,   
  }),
  ...mapGetters({
    isReadOnly: 'identity/isReadOnly',   
    isReviewedOnly: 'identity/isReviewedOnly',  
  }),
  label() {
    if(this.labels === undefined || this.labeling_list_entry === undefined) {
      return undefined;
    }
    return this.labels.find(label => equals_tolerant(label.start, this.labeling_list_entry.label_start) && equals_tolerant(label.end, this.labeling_list_entry.label_end));
  },
  generated_labels() {
    if(this.label === undefined || this.labeling_list_entry === undefined) {
      return undefined;
    }
    return this.label.generated_labels.filter(generated_label => this.labeling_list_entry.label_name === generated_label.name);
  },
  spectrogramUrl() {
    if(this.labeling_list_entry_sample_id === undefined || this.labeling_list_entry === undefined) {
      return undefined;
    }
    return this.apiBase + 'samples/' + this.labeling_list_entry_sample_id + '/spectrum' + '?cutoff=' + this.canvasHeight + "&threshold=" + this.threshold + "&start=" + this.labeling_list_entry.label_start + "&end=" + this.labeling_list_entry.label_end;
  },
  labeling_list_entry() {
    if(this.labeling_list !== undefined && this.labeling_list_pos !== undefined && this.labeling_list_pos > -1 && this.labeling_list_pos < this.labeling_list.entries.length) {
      return this.labeling_list.entries[this.labeling_list_pos];
    } else {
      return undefined;
    }
  },
  labeling_list_entry_sample_id() {
    if(this.labeling_list_entry === undefined) {
      return undefined;
    }
    return this.labeling_list_entry.sample_id;
  },
  labeling_list_pos_past_end() {
    return this.labeling_list === undefined || this.labeling_list_pos === undefined || this.labeling_list_pos >= this.labeling_list.entries.length;
  },
  labeling_list_pos_pre_start() {
    return this.labeling_list === undefined || this.labeling_list_pos === undefined || this.labeling_list_pos < 0;
  },
  labeledCount() {
    if(this.labeling_list === undefined) {
      return NaN;
    }
    return this.labeling_list.entries.reduce((acc, entry) => entry.classified ? acc + 1 : acc, 0);
  },
  audioCurrentTimePos() {
    if(this.audioCurrentTime === undefined || this.label === undefined) {
      return undefined;
    }
    return this.audioCurrentTime - this.label.start;
  },
  audioSpectrogramCurrentTimePos() {
    if(this.audioCurrentTimePos === undefined || this.audioColumnsPerSecond === undefined) {
      return undefined;
    }
    return this.audioCurrentTimePos * this.audioColumnsPerSecond;
  },
  audioPositionStyle() {
    /*return {
      left: '300px',
    };*/
    if(this.audioSpectrogramCurrentTimePos === undefined) {
       return {
         visibility: 'hidden',
       };
    }
    return {
      left: this.audioSpectrogramCurrentTimePos + 'px',
    };
  },
  userLabels() {
    return this.label != undefined && this.label.labels !== undefined ? this.label.labels.map(e => e.name) : [];
  },
  selectedUserLabels() {
    if(this.selectedLabelDefinitions === undefined || this.selectedLabelDefinitions === null || this.selectedLabelDefinitions.length === 0) {
      return [];
    }
    return this.selectedLabelDefinitions.map(d => d.name);
  },
  userLabelsStored() {
    return JSON.stringify(this.userLabels) === JSON.stringify(this.selectedUserLabels);
  }
},
watch: {
  isLabeledOnly: {
    immediate: true,
    handler() {
      if(this.isReviewedOnly) {
        this.skip_labeled_entries = false;
      }
    },
  },
  userLabels() {
    this.selectedLabelDefinitions = this.userLabels.map(u => {
      var dfn = this.label_definitions.find(d => d.name === u);
      console.log("find" + u); 
      if(dfn !== undefined) {
        return dfn;
      } else {
        return {name: u, desc: 'unknown'};
      }
    });
  },
  label() {
    /*if(this.label !== undefined) {
      this.replayAudio();
    }*/
  },
  async selected_labeling_list() {
    if(this.selected_labeling_list !== undefined) {
      this.labeling_list_message = 'loading labeling_list...';
      this.labeling_list_pos = undefined;
      try {
        var response = await axios.get(this.apiBase + 'labeling_lists' + '/' + this.selected_labeling_list);
        this.labeling_list = response.data.labeling_list;
        this.labeling_list_message = undefined;
        console.log(this.labeling_list);
        if(this.labeling_list.entries.length > 0) {
          this.labeling_list_pos = -1;
          this.moveNextLabelingListEntry();
        }
      } catch(e) {
        console.log(e);
        this.labeling_list_message = 'error loading labeling_list ' + this.selected_labeling_list;
      }      
    } else {
      this.labeling_list = undefined;
      this.labeling_list_message = "no labeling list selected";
      this.labeling_list_pos = undefined;
    }
  },
  labeling_list_entry_sample_id() {
    this.refresh_sample();
  }
},
methods: {
  ...mapActions({
    label_definitions_init: 'label_definitions/init',
    label_definitions_refresh: 'label_definitions/refresh',
  }),
  async refresh() {
    this.label_definitions_init();
    this.labeling_lists_message = 'loading labeling_lists...';
    try {
      var response = await axios.get(this.apiBase + 'labeling_lists');
      this.labeling_lists = response.data.labeling_lists;
      this.labeling_lists_message = undefined;
    } catch {
      this.labeling_lists_message = 'error loading labeling_lists';
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
    this.audioCurrentTime = undefined;
    this.audioColumnsPerSecond = undefined; 
    if(this.label !== undefined && !this.$refs.audio.paused) {
      if(this.$refs.audio.currentTime < this.label.end) {
        //console.log(this.$refs.audio.currentTime);
        this.audioCurrentTime = this.$refs.audio.currentTime;
        if(this.$refs.spectrogram !== undefined && this.$refs.spectrogram.naturalWidth > 0) {
          this.audioColumnsPerSecond = this.$refs.spectrogram.naturalWidth / (this.label.end - this.label.start);
        }
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
      case 'ArrowLeft':
        e.preventDefault();
        this.movePrevLabelingListEntry();
        break;   
      case 'ArrowRight':
        e.preventDefault();
        this.moveNextLabelingListEntry();
        break;
    }
  },
  onKeyUp(e) {
    console.log('|' + e.key + '|');
    switch(e.key) {
      case 'Escape':
        console.log("ESCAPE");
        e.preventDefault();
        console.log(this.$refs.labelSelect);
        if(this.$refs.labelSelect.isOpen) {
          this.$refs.labelSelect.isOpen = false;
        } else {
          this.$refs.labelSelect.toggle();
        }       
        //this.$refs.labelSelect.focus();
        //this.$refs.labelSelect.toggle();
        /*this.$nextTick(() => {
          this.$refs.labelSelect.toggle();
        });*/
        break;                                     
    }
  },
  setLabeling() {
    this.$nextTick(() => {
      this.sendLabeling();
    });
  },
  async sendLabeling() {
    if(this.selected_labeling_list != undefined && this.labeling_list_entry !== undefined) {
      try {
        this.postLabelingSending = true;
        this.postLabelingError = false;
        this.postLabelingMessage = 'Sending labeling to server ...';
        var content = {actions: [{
          action: "set_labeling", 
          sample_id: this.labeling_list_entry_sample_id, 
          label_start: this.labeling_list_entry.label_start, 
          label_end: this.labeling_list_entry.label_end, 
          label_names: this.selectedUserLabels
        }]}; 
        var response = await axios.post(this.apiBase + 'labeling_lists' + '/' + this.selected_labeling_list, content);
        this.labeling_list = response.data.labeling_list;
        if(response.data.sample_labels.sample_id === this.labeling_list_entry_sample_id) {
          this.labels = response.data.sample_labels.labels
        } else {
          console.log("error wrong sample id: " + this.labeling_list_entry_sample_id);
        }
        this.labeling_list_message = undefined;
      } catch(e) {
        this.postLabelingError = true;
        console.log(e);
        this.postLabelingMessage = 'Error Sending labeling to server.';
      } finally {
        this.postLabelingSending = false;
      }
    }
  },
  movePrevLabelingListEntry() {
    if(this.labeling_list !== undefined) {
      if(this.skip_labeled_entries) {
        while(this.labeling_list_pos > -1) {
          this.labeling_list_pos--;
          if(this.labeling_list_pos > -1 && !this.labeling_list.entries[this.labeling_list_pos].classified) {
            break;
          }
        }
      } else {
        if(this.labeling_list_pos > -1) {
          this.labeling_list_pos--;
        }
      }
    }
  }, 
  moveNextLabelingListEntry() {
    if(this.labeling_list !== undefined) {
      if(this.skip_labeled_entries) {
        while(this.labeling_list_pos < this.labeling_list.entries.length) {
          this.labeling_list_pos++;
          if(this.labeling_list_pos < this.labeling_list.entries.length && !this.labeling_list.entries[this.labeling_list_pos].classified) {
            break;
          }
        }
      } else {
        if(this.labeling_list_pos < this.labeling_list.entries.length) {
          this.labeling_list_pos++;
        }
      }
    }    
  },
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
  async onLockAudioSample() {
    if(this.labeling_list_entry_sample_id === undefined) {
      alert("no lock sent")
      return;
    }
    console.log("send locked");

    try {
      this.postLabelingSending = true;
      this.postLabelingError = false;
      this.postLabelingMessage = 'Sending locked status to server ...';
      var content = {actions: [{action: "set_locked"}]}; 
      var response = await axios.post(this.apiBase + 'samples' + '/' + this.labeling_list_entry_sample_id, content);
      var r = response.data;
      console.log(r);
    } catch(e) {
      this.postLabelingError = true;
      console.log(e);
      this.postLabelingMessage = 'Error sending locked status to server.';
    } finally {
      this.postLabelingSending = false;
      this.refresh_sample();
    }
  },
  refresh_sample() {
    this.sampleMeta = undefined;
    if(this.labeling_list_entry_sample_id === undefined) {
      return;
    }
    axios.get(this.apiBase + 'samples' + '/' + this.labeling_list_entry_sample_id + '/' + 'meta')
    .then(response => {
      var data = response.data;
      var parsed = YAML.parse(data);
      console.log(parsed);
      this.sampleMeta = parsed.meta;
      this.sampleMeta.datetime = new Date(this.sampleMeta.timestamp * 1000);
    })
    .catch(() => {
      this.sampleMeta = undefined;
    });
    
    this.labels = undefined;
    axios.get(this.apiBase + 'samples' + '/' + this.labeling_list_entry_sample_id + '/' + 'labels')
    .then(response => {
      this.labels = response.data.labels;         
    })
    .catch(() => {
      this.labels = undefined;
    });
  }        
},
mounted() {
  this.animationFrameCallback = this.animationFrame.bind(this);
  document.addEventListener('keydown', this.onKeyDown.bind(this));
  document.addEventListener('keyup', this.onKeyUp.bind(this));
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
  grid-template-columns: auto auto auto;
  justify-items: center;
  align-items: center;
}

.labeling-label {
  font-weight: bold;
  font-size: 1.5rem;
}

.labeled-selected {
  border-style: solid;
  border-width: 1px;
  border-color: #0000;
  border-radius: 6px;
  -webkit-box-shadow: 0px 0px 15px #443c319c;
  box-shadow: 0px 0px 15px #443c319c;
  background-color: #00000012;
}

.hidden {
  visibility: hidden;
}

.sending {
  grid-column-start: 1;
  grid-column-end: 4;
  color: #ffa502;
  text-shadow: 0 0 15px #d35400;
  font-size: 2em;
}

.sending-error {
    grid-column-start: 1;
    grid-column-end: 4;
    color: #ff0202;
    text-shadow: 0 0 15px #d30000;
    font-size: 2em;
}

button:active {
  background-color: aqua;
}

.generated-labels {
  display: grid;
  grid-template-columns: repeat(3, auto);
  justify-items: center;
  align-items: center;
}

.generated-labels-header {
  color: rgb(161, 161, 161);
  padding-right: 10px;
  padding-bottom: 5px;
}

.generated-labels-cell {
  color: rgb(94, 88, 88);
  padding-right: 10px;
  padding-bottom: 5px;
}

.generated-labels-cell-reliability {
  font-weight: bold;
  font-size: 1.5em;
}

.audio-position {
  position: absolute; 
  z-index: 1; 
  width: 1px;
  height: 512px;
  background-color: #ff0000b0;
  box-shadow: 0px 0px 4px red;
  transition: all 0.05s linear;
}

.confirmed-low {
  color: red;
}

.confirmed-done {
  color: green;
}

</style>
