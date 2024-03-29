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
        <v-list-tile>
        <v-list-tile-title><a href="#/labeling"><v-icon>arrow_forward</v-icon>labeling lists</a></v-list-tile-title>
        </v-list-tile>                        
      </v-list>
    </v-menu> 
    <v-toolbar-title class="headline text-uppercase">
      Review list:  
    </v-toolbar-title> 
    <div style="margin-left: 10px;" v-if="review_lists_message === undefined">
      <v-select v-model="selected_review_list" :items="review_lists" label="Review list" solo item-text="id">
        <template v-slot:item="props">
          <span  style="white-space: nowrap;">
            <span v-show="review_list_open_counts[props.item.id] !== undefined">{{props.item.id}}, {{review_list_open_counts[props.item.id]}} <span v-show="review_list_unsure_counts[props.item.id] !== undefined"> ({{(review_list_open_counts[props.item.id] + review_list_unsure_counts[props.item.id])}})</span> left </span>
            <span v-show="review_list_open_counts[props.item.id] === undefined" style="color: grey;">{{props.item.id}} </span>
            <span v-if="review_yes_counts[props.item.id] !== undefined" :class="{'confirmed-low': review_yes_counts[props.item.id] < 50, 'confirmed-done': review_yes_counts[props.item.id] >= 50}"> ({{review_yes_counts[props.item.id]}} confirmed)</span>
            <span v-else class="confirmed-low"> (no confirmed)</span>
            <span v-if="review_unsure_counts[props.item.id] !== undefined"> ({{review_unsure_counts[props.item.id]}} unsure)</span>
            <span v-if="review_no_counts[props.item.id] !== undefined"> ({{review_no_counts[props.item.id]}} disapproved)</span>
          </span>
        </template>
      </v-select>
    </div>
    <div v-if="review_list !== undefined && reviewedCount === review_list.entries.length && !isReviewedOnly">all entries done in this list</div>
    <div style="margin-left: 10px;" v-if="review_lists_message !== undefined">{{review_lists_message}}</div>
    <span v-if="isReadOnly" style="color: #e11111; padding-left: 10px;">readOnly</span>
    <span v-if="isReviewedOnly" style="color: #e11111; padding-left: 10px;">reviewedOnly</span>
    <div style="display: flex; position: absolute; right: 2px; width: 200px;" >
      <review-statistics-dialog />
      <identity-dialog />
    </div>
  </v-toolbar>

  <v-content v-resize="onResize" id="layoutElement">

  <v-layout align-center justify-start column fill-height v-if="selected_review_list !== undefined">

      <div style="display: grid; justify-items: center; align-items: center;">
        <span :class="{hidden: review_list_pos_pre_start}" style="grid-row-start: 1; grid-column-start: 1;">
          [Arrow Left]
          <v-btn @click="movePrevReviewListEntry"><v-icon>fast_rewind</v-icon></v-btn>
        </span>
        
        <span :class="{hidden: !review_list_pos_pre_start}" style="grid-row-start: 1; grid-column-start: 2;">
          <b>Start of list reached</b>
        </span>
        
        <span :class="{hidden: (review_list_pos_pre_start || review_list_pos_past_end)}" style="grid-row-start: 1; grid-column-start: 2;">
          <b>Position {{review_list_pos === undefined ? 0 : (review_list_pos + 1)}} of {{review_list === undefined ? 0 : review_list.entries.length}}</b>
        </span>        
        
        <span :class="{hidden: !review_list_pos_past_end}" style="grid-row-start: 1; grid-column-start: 2;">
          <b>End of list reached</b>
        </span>
        
        <span :class="{hidden: review_list_pos_past_end}" style="grid-row-start: 1; grid-column-start: 3;">
          <v-btn @click="moveNextReviewListEntry"><v-icon>fast_forward</v-icon></v-btn>
          [Arrow Right]
        </span>
        
        <span style="grid-row-start: 1; grid-column-start: 4; margin-left: 50px; margin-top: 10px;">
          <v-switch
                v-model="skip_review_entries"
                label="unreviewed only"
                color="success"
                hide-details
                height="1"
                style="margin-top: 0px;"
                v-if="!isReviewedOnly"
          ></v-switch> 
          <v-checkbox v-show="skip_review_entries"
            v-model="skip_not_review_unsure_entries"
            label="including unsure"
          ></v-checkbox>
        </span>         
        <span style="grid-row-start: 1; grid-column-start: 5; margin-left: 50px; background: #f8fff4; color: #499d2a;">
          Reviewed {{reviewedCount}} <span v-if="!isReviewedOnly">of {{review_list === undefined ? NaN : review_list.entries.length}}</span>
        </span>
        
        <span :class="{hidden: review_list_pos_past_end}" style="grid-row-start: 1; grid-column-start: 6;">
          <v-menu 
            transition="scale-transition" 
            :close-on-content-click="false" 
            ref="jumpMenu"
          >
            <template v-slot:activator="{on}">
              <v-btn v-on="on" title="Move to specific position in the entry list. If specified position is not selectable, next valid position is targeted.">Jump to <v-icon style="padding-left: 5px;">arrow_right_alt</v-icon></v-btn>
            </template>
            <v-list>
                <v-text-field
                  v-model="jumpText" 
                  :append-outer-icon="jumpTextValid ? 'arrow_right_alt' : ''"
                  box
                  label="Jump to entry number"
                  type="text"
                  @click:append-outer="onJump"
                                @keydown.stop=""
              @keyup.stop=""
              @keypress.stop="" 
              @keypress.enter="onJump"
                />
            </v-list>        
          </v-menu>
        </span>

        <span style="grid-row-start: 1; grid-column-start: 7;">
          <player-settings />
        </span>                
      </div>

      <div>
        <span style="font-size: 1.5em; background-color: #0000000a; padding: 2px;" title="currently selected audio sample">
          <span v-if="sampleMeta !== undefined">
            <b><v-icon>place</v-icon> {{sampleMeta.location === undefined ? '-' : sampleMeta.location}} </b> 
            <span><v-icon>date_range</v-icon> {{sampleMeta.datetime === undefined ? '-' : toDate(sampleMeta.datetime)}} </span> 
            <span style="color: grey;"><v-icon>access_time</v-icon> {{sampleMeta.datetime === undefined ? '-' : toTime(sampleMeta.datetime)}}</span>
          </span>
          <!--<span v-if="review_list_entry_sample_id !== undefined">-->
            <span v-else-if="review_list_entry_sample_id !== undefined">
            <!--<b :class="{hidden: sampleMeta !== undefined}">{{review_list_entry_sample_id}}</b>-->
            Loading sample ...
          </span>
        </span>
        <span v-if="review_list_entry_sample_id !== undefined" style="padding-left: 100px; font-size: 1.2em;">
          {{review_list_entry.label_start.toFixed(2) + ' - ' + review_list_entry.label_end.toFixed(2)}}
        </span>
      </div>  

      <div v-if="review_list_entry_sample_id !== undefined && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)" style="position: relative;">
        <audio ref="audio" :src="audioUrl" type="audio/wav" preload="auto" />
        <div class="audio-position" :style="audioPositionStyle"></div>        
        <img ref="spectrogram" :src="spectrogramUrl" class="spectrogram" draggable="false" v-if="spectrogramUrl !== undefined" @mousedown="onSpectrogramMouseDown" style="z-index: 0;"  :style="{ 'max-width': spectrogramMaxWidth + 'px' }"/>
      </div>

      <div v-if="review_list_entry_sample_id !== undefined && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)" class="review-label">
        {{review_list_entry.label_name}}
      </div>

      <div class="controls" v-if="review_list_entry_sample_id !== undefined && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)">
        <div :class="{ 'reviewed-selected': storedReviewed === 'no' }"><v-btn @click="setReviewed('no')" color="red" :disabled="isReadOnly"><v-icon dark>clear</v-icon> NO</v-btn></div>
        <div :class="{ 'reviewed-selected': storedReviewed === 'unsure' }"><v-btn @click="setReviewed('unsure')" color="yellow" :disabled="isReadOnly"><v-icon dark>code</v-icon> UNSURE</v-btn></div> 
        <div :class="{ 'reviewed-selected': storedReviewed === 'yes' }"><v-btn @click="setReviewed('yes')" color="green" :disabled="isReadOnly"><v-icon dark>done</v-icon> YES</v-btn></div>
        <div><v-btn @click="stopAudio()" icon title="stop audio"><v-icon dark>stop</v-icon></v-btn></div>
        <div><v-btn @click="replayAudio()" icon title="replay audio"><v-icon dark>replay</v-icon></v-btn></div>
        <div><review-special-dialog :sampleId="review_list_entry_sample_id" @lock-audio-sample="onLockAudioSample" v-if="!isReadOnly"/></div>
        <div>[Esc]</div>
        <div>[Enter]</div>
        <div>[Space]</div>
        <div>[End]</div>        
        <div>[Tab]</div>
        <div class="sending" :class="{hidden: !postReviewSending}">{{postReviewMessage}}</div>    
        <div class="sending-error" :class="{hidden: !postReviewError}">{{postReviewMessage}}</div> 
      </div>

      <div class="generated-labels" v-if="generated_labels !== undefined && generated_labels.length > 0 && (sampleMeta === undefined || sampleMeta.sample_locked === undefined)">
        <div class="generated-labels-header">Model</div>
        <div class="generated-labels-header">Version</div>
        <div class="generated-labels-header">Reliability</div>    

        <template v-for="g in generated_labels">
          <div class="generated-labels-cell" :key="JSON.stringify(g)+1">{{g.generator}}</div>
          <div class="generated-labels-cell" :key="JSON.stringify(g)+2">{{g.model_version}}</div>
          <div class="generated-labels-cell generated-labels-cell-reliability" :key="JSON.stringify(g)+4">{{g.reliability === undefined ? '' : Math.round(g.reliability * 100)}}</div>
        </template>      
      </div>

      <div v-if="sampleMeta !== undefined && sampleMeta.sample_locked !== undefined" style="margin-top: 100px;">
        <h2>This audio sample has been locked. Possibly because a human was audible.</h2>
      </div>
  </v-layout>

  <v-layout align-center justify-start column fill-height v-if="selected_review_list === undefined">
    no review list selected
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
import reviewStatisticsDialog from './review-statistics-dialog'
import playerSettings from './player-settings'


function equals_tolerant(a, b) {
	return (a - 0.001) < b && b < (a + 0.001);
}

const yearFormat = new Intl.DateTimeFormat('en', { year: 'numeric' });
const monthFormat = new Intl.DateTimeFormat('en', { month: '2-digit' });
const dayFormat = new Intl.DateTimeFormat('en', { day: '2-digit' });
const hourFormat = new Intl.DateTimeFormat('en', { hour: '2-digit', hour12: false });

export default {
name: 'review-view',
components: {
  identityDialog,
  reviewSpecialDialog,
  reviewStatisticsDialog,
  playerSettings,
},
data () {
  return {
    sampleMeta: undefined,
    labels: undefined,
    canvasHeight: 512,
    animationFrameCallback: undefined,
    animationFrameID: undefined,
    review_lists: [],
    review_lists_message: 'init',
    selected_review_list: undefined,
    review_list: undefined,
    review_list_message: 'init',
    review_list_pos: undefined,
    skip_review_entries: true,
    skip_not_review_unsure_entries: true,
    postReviewMessage: 'init',
    postReviewSending: false,
    postReviewError: false,
    audioCurrentTime: undefined,
    audioColumnsPerSecond: undefined,
    jumpText: undefined,
    layoutWidth: 1024,
  }
},
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    threshold: state => state.settings.player_spectrum_threshold,   
    playbackRate: state => state.settings.player_playbackRate,
    preservesPitch: state => state.settings.player_preservesPitch,
    overwriteSamplingRate: state => state.settings.player_overwriteSamplingRate,
    samplingRate: state => state.settings.player_samplingRate,    
    review_statistics: state => state.review_statistics.data,    
  }),
  ...mapGetters({
    isReadOnly: 'identity/isReadOnly',   
    isReviewedOnly: 'identity/isReviewedOnly',  
  }),
  label() {
    if(this.labels === undefined || this.review_list_entry === undefined) {
      return undefined;
    }
    return this.labels.find(label => equals_tolerant(label.start, this.review_list_entry.label_start) && equals_tolerant(label.end, this.review_list_entry.label_end));
  },
  generated_labels() {
    if(this.label === undefined || this.review_list_entry === undefined) {
      return undefined;
    }
    return this.label.generated_labels.filter(generated_label => this.review_list_entry.label_name === generated_label.name);
  },
  spectrogramUrl() {
    if(this.review_list_entry_sample_id === undefined || this.review_list_entry === undefined) {
      return undefined;
    }
    return this.apiBase + 'samples/' + this.review_list_entry_sample_id + '/spectrum' + '?cutoff=' + this.canvasHeight + "&threshold=" + this.threshold + "&start=" + this.review_list_entry.label_start + "&end=" + this.review_list_entry.label_end;
  },
  storedReviewed() {
    if(this.label === undefined || this.review_list_entry === undefined) {
      return undefined;
    }
    var r = this.label.reviewed_labels.find(r => r.name === this.review_list_entry.label_name);
    return r === undefined ? undefined : r.reviewed;
  },
  review_list_entry() {
    if(this.review_list !== undefined && this.review_list_pos !== undefined && this.review_list_pos > -1 && this.review_list_pos < this.review_list.entries.length) {
      return this.review_list.entries[this.review_list_pos];
    } else {
      return undefined;
    }
  } ,
  review_list_entry_sample_id() {
    if(this.review_list_entry === undefined) {
      return undefined;
    }
    return this.review_list_entry.sample_id;
  },
  review_list_pos_past_end() {
    return this.review_list === undefined || this.review_list_pos === undefined || this.review_list_pos >= this.review_list.entries.length;
  },
  review_list_pos_pre_start() {
    return this.review_list === undefined || this.review_list_pos === undefined || this.review_list_pos < 0;
  },
  reviewedCount() {
    if(this.review_list === undefined) {
      return NaN;
    }
    return this.review_list.entries.reduce((acc, entry) => entry.classified ? acc + 1 : acc, 0);
  },
  audioCurrentTimePos() {
    if(this.audioCurrentTime === undefined || this.label === undefined) {
      return undefined;
    }
    return this.audioCurrentTime - (this.label.start * this.audioTimeFactor);
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
  review_yes_counts() {
    if(this.review_statistics === undefined) {
      return {};
    }
    return this.review_statistics.reviewed_yes_counts;
  },
  review_unsure_counts() {
    if(this.review_statistics === undefined) {
      return {};
    }
    return this.review_statistics.reviewed_unsure_counts;
  },
  review_no_counts() {
    if(this.review_statistics === undefined) {
      return {};
    }
    return this.review_statistics.reviewed_no_counts;
  },
  review_list_open_counts() {
    if(this.review_statistics === undefined) {
      return {};
    }
    return this.review_statistics.review_list_open_counts;
  },
  review_list_unsure_counts() {
    if(this.review_statistics === undefined) {
      return {};
    }
    return this.review_statistics.review_list_unsure_counts;
  },
  jumpTextValid() {
    return this.jumpText !== undefined 
      && this.jumpText !== null 
      && this.jumpText.length !== 0 
      && this.jumpText == Number.parseInt(this.jumpText)
      && this.review_list !== undefined 
      && Number.parseInt(this.jumpText) > 0
      && Number.parseInt(this.jumpText) <= this.review_list.entries.length;
  },
  spectrogramMaxWidth() {
    return this.layoutWidth - 32;
  },
  audioUrl() {
    if(this.overwriteSamplingRate && this.samplingRate !== undefined) {
      return this.apiBase + 'samples/' + this.review_list_entry_sample_id + '/data' + '?overwrite_sampling_rate=' + this.samplingRate;
    } else {
      return this.apiBase + 'samples/' + this.review_list_entry_sample_id + '/data';      
    }
  },
  audioTimeFactor() {
    if(!this.overwriteSamplingRate || this.samplingRate === undefined || this.sampleMeta === undefined || this.sampleMeta.SampleRate === undefined) {
      return 1;
    }
    console.log(this.sampleMeta.SampleRate);
    return this.sampleMeta.SampleRate / this.samplingRate;
  },   
},
watch: {
  isReviewedOnly: {
    immediate: true,
    handler() {
      if(this.isReviewedOnly) {
        this.skip_review_entries = false;
      }
    },
  },
  labels() {
    //this.labelIndex = 0;
  },
  label() {
    if(this.label !== undefined) {
      this.replayAudio();
    }
  },
  async selected_review_list() {
    if(this.selected_review_list !== undefined) {
      this.review_list_message = 'loading review_list...';
      this.review_list_pos = undefined;
      try {
        var response = await axios.get(this.apiBase + 'review_lists' + '/' + this.selected_review_list);
        this.review_list = response.data.review_list;
        this.review_list_message = undefined;
        console.log(this.review_list);
        if(this.review_list.entries.length > 0) {
          this.review_list_pos = -1;
          this.moveNextReviewListEntry();
        }
      } catch(e) {
        console.log(e);
        this.review_list_message = 'error loading review_list ' + this.selected_review_list;
      }      
    } else {
      this.review_list = undefined;
      this.review_list_message = "no review list selected";
      this.review_list_pos = undefined;
    }
  },
  review_list_entry_sample_id() {
    this.refresh_sample();
  },
  spectrogramMaxWidth() {
    this.updateSpectrogramMaxWidthStyle();
  },
  playbackRate() {
    this.$refs.audio.playbackRate = this.playbackRate;
    this.$refs.audio.defaultPlaybackRate = this.playbackRate;    
  },
  preservesPitch() {
    this.$refs.audio.preservesPitch = this.preservesPitch;
    this.$refs.audio.mozPreservesPitch = this.preservesPitch;
    this.$refs.audio.webkitPreservesPitch  = this.preservesPitch;
  },  
},
methods: {
  ...mapActions({
    review_statistics_refresh: 'review_statistics/refresh',
  }),
  async refresh() {
    this.review_statistics_refresh();
    this.review_lists_message = 'loading review_lists...';
    try {
      var response = await axios.get(this.apiBase + 'review_lists');
      this.review_lists = response.data.review_lists;
      this.review_lists_message = undefined;
    } catch {
      this.review_lists_message = 'error loading review_lists';
    }
    this.updateLayoutSize();
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
  replayAudio(startPos) {
    setTimeout(() => {
      requestAnimationFrame(() => {
        if(this.$refs.audio === undefined) {
          console.log("no audio");
          return;
        }
        //console.log(this.$refs.audio.src);
        this.$refs.audio.pause();
        if(this.label !== undefined) {
          let currentTime = startPos === undefined ? (this.label.start * this.audioTimeFactor) : startPos;
          //console.log(currentTime);
          this.$refs.audio.currentTime = currentTime;
          this.$refs.audio.play();
          this.requestAnimationFrame();
        } else {
          console.log("label undefined");
        }
      });
    }, 250);
  },
  stopAudio() {
    requestAnimationFrame(() => {
      if(this.$refs.audio === undefined) {
        console.log("no audio");
        return;
      }
      this.$refs.audio.pause();
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
    //this.audioColumnsPerSecond = undefined; 
    if(this.label !== undefined && !this.$refs.audio.paused) {
      if(this.$refs.audio.currentTime < (this.label.end * this.audioTimeFactor)) {
        //console.log(this.$refs.audio.currentTime);
        this.audioCurrentTime = this.$refs.audio.currentTime;
        if(this.$refs.spectrogram !== undefined && this.$refs.spectrogram.naturalWidth > 0) {
          let spectrogramWidth = this.$refs.spectrogram.naturalWidth;
          if(spectrogramWidth > this.spectrogramMaxWidth) {
            spectrogramWidth = this.spectrogramMaxWidth;
          }
          this.audioColumnsPerSecond =  spectrogramWidth / (this.label.end - this.label.start) / this.audioTimeFactor;
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
      case 'End':
        e.preventDefault();
        this.stopAudio();
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
        this.movePrevReviewListEntry();
        break;   
      case 'ArrowRight':
        e.preventDefault();
        this.moveNextReviewListEntry();
        break;                            
    }
  },
  async setReviewed(reviewed) {
    if(this.selected_review_list != undefined && this.review_list_entry !== undefined) {
      try {
        this.postReviewSending = true;
        this.postReviewError = false;
        this.postReviewMessage = 'Sending review to server ...';
        var content = {actions: [{action: "set_reviewed_label", sample_id: this.review_list_entry_sample_id, label_start: this.review_list_entry.label_start, label_end: this.review_list_entry.label_end, label_name: this.review_list_entry.label_name, reviewed: reviewed}]}; 
        var response = await axios.post(this.apiBase + 'review_lists' + '/' + this.selected_review_list, content);
        this.review_list = response.data.review_list;
        this.review_list_message = undefined;
        //this.labels = response.data.labels;
        console.log(this.review_list);
        this.moveNextReviewListEntry();
      } catch(e) {
        this.postReviewError = true;
        console.log(e);
        this.postReviewMessage = 'Error Sending review to server.';
      } finally {
        this.postReviewSending = false;
        this.review_statistics_refresh();
      }
    }
  },
  movePrevReviewListEntry() {
    requestAnimationFrame(() => {
      if(this.$refs.audio !== undefined) {
        this.$refs.audio.pause();
      }
      requestAnimationFrame(() => { 
        if(this.review_list !== undefined) {
          if(this.skip_review_entries) {
            if(this.skip_not_review_unsure_entries) {
              while(this.review_list_pos > -1) {
                this.review_list_pos--;
                if(this.review_list_pos > -1 && ((!this.review_list.entries[this.review_list_pos].classified) || (this.review_list.entries[this.review_list_pos].latest_review === 'unsure'))) {
                  break;
                }
              }
            } else {
              while(this.review_list_pos > -1) {
                this.review_list_pos--;
                if(this.review_list_pos > -1 && (!this.review_list.entries[this.review_list_pos].classified)) {
                  break;
                }
                }
              }
          } else {
            if(this.review_list_pos > -1) {
              this.review_list_pos--;
            }
          }
        }
      });
    });  
  }, 
  moveNextReviewListEntry() {
    requestAnimationFrame(() => {
      if(this.$refs.audio !== undefined) {
        this.$refs.audio.pause();
      }
      requestAnimationFrame(() => {    
        if(this.review_list !== undefined) {
          if(this.skip_review_entries) {
            if(this.skip_not_review_unsure_entries) {
              while(this.review_list_pos < this.review_list.entries.length) {
                this.review_list_pos++;
                if(this.review_list_pos < this.review_list.entries.length && ((!this.review_list.entries[this.review_list_pos].classified) || (this.review_list.entries[this.review_list_pos].latest_review === 'unsure'))) {
                  break;
                }
              }
            } else {
              while(this.review_list_pos < this.review_list.entries.length) {
                this.review_list_pos++;
                if(this.review_list_pos < this.review_list.entries.length && !this.review_list.entries[this.review_list_pos].classified) {
                  break;
                }
              }          
            }
          } else {
            if(this.review_list_pos < this.review_list.entries.length) {
              this.review_list_pos++;
            }
          }
        }
      });
    });         
  },
  jumpToReviewListEntry(targetIndex) {
    requestAnimationFrame(() => {
      if(this.$refs.audio !== undefined) {
        this.$refs.audio.pause();
      }
      requestAnimationFrame(() => {      
        if(this.review_list !== undefined) {
          if(this.skip_review_entries) {
            if(this.skip_not_review_unsure_entries) {
              while(targetIndex <= this.review_list.entries.length) {
                if(targetIndex < this.review_list.entries.length && ((!this.review_list.entries[targetIndex].classified) || (this.review_list.entries[targetIndex].latest_review === 'unsure'))) {
                  break;
                }
                targetIndex++;
              }
              this.review_list_pos = targetIndex;
            } else {
              while(targetIndex <= this.review_list.entries.length) {
                if(targetIndex < this.review_list.entries.length && !this.review_list.entries[targetIndex].classified) {
                  break;
                }
                targetIndex++;
              }
              this.review_list_pos = targetIndex;          
            }
          } else {
            if(targetIndex <= this.review_list.entries.length) {
              this.review_list_pos = targetIndex;
            }
          }
        }
      });
    });        
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
    if(this.review_list_entry_sample_id === undefined) {
      alert("no lock sent")
      return;
    }
    console.log("send locked");

    try {
      this.postReviewSending = true;
      this.postReviewError = false;
      this.postReviewMessage = 'Sending locked status to server ...';
      var content = {actions: [{action: "set_locked"}]}; 
      var response = await axios.post(this.apiBase + 'samples' + '/' + this.review_list_entry_sample_id, content);
      var r = response.data;
      console.log(r);
    } catch(e) {
      this.postReviewError = true;
      console.log(e);
      this.postReviewMessage = 'Error sending locked status to server.';
    } finally {
      this.postReviewSending = false;
      this.refresh_sample();
    }
  },
  refresh_sample() {
    this.sampleMeta = undefined;
    if(this.review_list_entry_sample_id === undefined) {
      return;
    }
    axios.get(this.apiBase + 'samples' + '/' + this.review_list_entry_sample_id + '/' + 'meta')
    .then(response => {
      var data = response.data;
      var parsed = YAML.parse(data);
      console.log(parsed);
      this.sampleMeta = parsed.meta;
      this.sampleMeta.datetime = this.sampleMeta.timestamp === undefined ? undefined : new Date(this.sampleMeta.timestamp * 1000);
    })
    .catch(() => {
      this.sampleMeta = undefined;
    });
    
    this.labels = undefined;
    axios.get(this.apiBase + 'samples' + '/' + this.review_list_entry_sample_id + '/' + 'labels')
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
  onJump() {
    if(this.jumpTextValid) {
      var targetIndex = Number.parseInt(this.jumpText) - 1;
      this.jumpToReviewListEntry(targetIndex);
      this.$refs.jumpMenu.save();
    }
  },
  onResize() {
    console.log("resize");
    this.updateLayoutSize();
  },
  updateLayoutSize() {
    let layoutElement = document.getElementById("layoutElement");
    if(layoutElement === undefined) {
      this.layoutWidth = 1024;
    } else {
      let clientWidth = layoutElement.clientWidth;
      this.layoutWidth = clientWidth < 256 ? 256 : clientWidth;
    }
    this.updateSpectrogramMaxWidthStyle();
  },
  updateSpectrogramMaxWidthStyle() {
    if(this.$refs.spectrogram !== undefined) {
      this.$refs.spectrogram.style.maxWidth = this.spectrogramMaxWidth + 'px';
    }
  },
  onSpectrogramMouseDown(e) {
    let rect = this.$refs.spectrogram.getBoundingClientRect();
    let xPos = e.clientX - rect.left;
    //console.log("MouseDown " + xPos);
    let startPos = (this.label.start * this.audioTimeFactor) + (xPos / this.audioColumnsPerSecond);
    //console.log("MouseDown " + xPos);
    this.replayAudio(startPos);
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
  grid-template-columns: auto auto auto auto auto auto;
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
