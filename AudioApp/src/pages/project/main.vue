<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="$refs.browser.show = true;" icon="menu_book" title="Browse"  padding="xs"></q-btn>
      <audio-browser ref="browser"/>
      <q-space></q-space>
      <div v-if="sample !== undefined">
        <q-btn :icon="$refs.browser.movePrevSelectedSampleRequested ? 'recycling' : 'navigate_before'" padding="xs" :class="{'element-hidden': $refs.browser.movePrevSelectedSampleRequested || !$refs.browser.hasSelectedSamplePrev}" @click="if(userSelectedLabelNamesChanged) {onSaveLabels();} $refs.browser.movePrevSelectedSampleRequested = true" title="Move to previous sample on the browsed list of samples"/>
        <span class="text-weight-bold" v-if="sample.location"><q-icon name="home"/>{{sample.location}}</span>
        <span class="text-weight-regular text-grey-9" style="padding-left: 10px;" v-if="sample.date"><q-icon name="calendar_today"/>{{sample.date}}</span>
        <span class="text-weight-light text-grey-7" style="padding-left: 5px;" v-if="sample.time"><q-icon name="query_builder"/>{{sample.time}}</span>
        <span class="text-weight-thin text-grey-6" style="padding-left: 10px; font-family: monospace;" v-if="sample.device"><q-icon name="memory"/>{{sample.device}}</span>
        <span class="text-weight-bold" v-if="(!sample.location || !sample.device) && sample.date === undefined"><q-icon name="fingerprint"/>{{sample.id}}</span>
        <span class="text-weight-thin text-grey-6" style="padding-left: 10px;" v-if="sampleRate"><q-icon name="leaderboard"/>{{Math.trunc(sampleRate/1000)}}<sup style="font-size: 0.8em">.{{sampleRatemhz}}</sup> kHz</span>
        <span class="text-weight-thin text-grey-6" style="padding-left: 10px;" v-if="duration !== undefined"><q-icon name="alarm"/><span v-if="durationHH !== '00'">{{durationHH}}:</span><span class="text-grey-8">{{durationMM}}</span><span class="text-grey-6">:{{durationSS}}</span><sup class="text-grey-5" style="font-size: 0.7em" v-if="durationMS !== '000'">.{{durationMS}}</sup></span>
        <q-btn :icon="$refs.browser.moveNextSelectedSampleRequested ? 'recycling' : 'navigate_next'" padding="xs" :class="{'element-hidden': $refs.browser.moveNextSelectedSampleRequested || !$refs.browser.hasSelectedSampleNext}" @click="if(userSelectedLabelNamesChanged) {onSaveLabels();} $refs.browser.moveNextSelectedSampleRequested = true" title="Move to next sample on the browsed list of samples"/>
      </div>
      <div :style="{visibility: sample === undefined ? 'visible' : 'hidden',}">
        <q-badge color="grey-4" text-color="grey-14" label="<== use the 'browse'-button on the left"/> 
        <q-badge color="yellow-14" text-color="accent" label="no audio sample selected"/> 
      </div>
      <q-space></q-space>
      <q-btn @click="$refs.settings.show = true;" icon="tune" title="Settings" padding="xs"></q-btn>
      <audio-settings ref="settings"/>
    </q-toolbar>
    <q-separator/>
    <q-toolbar class="bg-grey-3" :class="sampleVisibility">
      <!--<q-space></q-space>-->
      <q-btn @click="if(audioPlaying) {onAudioPauseButton();} else {onAudioPlayButton();}" :icon="audioPlaying?'pause':'play_arrow'" :title="audioPlaying?'Pause audio':'Play audio'" padding="xs"></q-btn>
      <div v-if="labels !== undefined && labels.length > 0 && newSegmentStart === undefined" class="q-ml-lg row">
        <span>Segment</span> 
        <q-btn icon="fast_rewind" padding="xs" @click="if(userSelectedLabelNamesChanged) {onSaveLabels();} onMovePrevLabel();" title="Move to previous label segment within this samples"/>
        <q-select v-model="selectedLabelIndex" dense options-dense hide-bottom-space filled :options="labelIndices" style="min-width: 100px;">
          <template v-slot:selected>
            {{selectedLabelIndex === undefined ? '-' : (selectedLabelIndex + 1)}} / {{labels.length}}
          </template>
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps" style="min-width: 150px;">
              <q-item-section>
                <q-item-label>{{scope.opt + 1}}</q-item-label>
                <q-item-label caption>{{labels[scope.opt].start.toFixed(3)}} .. {{labels[scope.opt].end.toFixed(3)}}</q-item-label>
              </q-item-section>
            </q-item>
          </template>                    
        </q-select>
        <q-btn icon="fast_forward" padding="xs" @click="if(userSelectedLabelNamesChanged) {onSaveLabels();} onMoveNextLabel();" title="Move to next label segment within this samples"/>
      </div>  
      <div v-if="selectedLabel !== undefined" class="q-ml-lg row">
        <q-btn icon-right="subdirectory_arrow_right" padding="xs" margin="xs" @click="onMoveToLabelStart" title="Move to label start time.">
          <q-checkbox
          v-model="autoMoveToLabelStart"
          size="xs"
          padding="xs"
          margin="xs"      
          color="grey-6"
          style="padding: 0px; margin: -4px;"
          title="Automatically move to label start time at each label change."
          />
        </q-btn>
        <span class="q-pa-sm">{{selectedLabel.start.toFixed(3)}} .. {{selectedLabel.end.toFixed(3)}}</span>
        <q-badge 
          v-for="(label,i) in selectedLabel.generated_labels" 
          :key="i" 
          color="grey-4" 
          :text-color="userSelectedLabelNamesSet.has(label.name) ? 'green' : 'grey-7'" class="q-ma-sm" @click="addLabel(label.name);" 
          style="cursor: pointer;"
          :title="selectedLabel === undefined ? 'Generated label.' : 'Click to add generated label to user selected labels.'"
        >
          {{label.name}}
        </q-badge>
        <q-select
          filled
          v-model="userSelectedLabelNames"
          :options="selectableLabels"
          label="Labels"
          style="width: 250px"
          dense
          multiple
          @update:model-value="userSelectedLabelNamesChanged = true"
          option-label="name"
          option-value="name"
          emit-value
          clearable
        >
          <template v-slot:append>
            <q-icon name="apps" @click.stop="labelSelectDialogShow = true;" />
            <q-dialog v-model="labelSelectDialogShow" transition-show="rotate" transition-hide="rotate" class="q-pt-none" full-width full-height>
              <div class="q-pt-none column wrap justify-start content-around fit" style="background-color: white;"> 
              <q-badge  v-for="labelDefinition in labelDefinitions" :key="labelDefinition.name" @click="addLabel(labelDefinition.name);"  color="grey-3" :text-color="userSelectedLabelNamesSet.has(labelDefinition.name) ? 'green' : 'grey-7'" style="width: 200px; margin: 1px; overflow: hidden;" class="text-h6" :title="labelDefinition.desc">
                <span v-if="labelDefinition.n" class="label-definition-n">{{labelDefinition.name}}</span>
                <span v-else>{{labelDefinition.name}}</span>
              </q-badge>
              </div>
            </q-dialog>            
          </template>
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps">
              <q-item-section>
                <q-item-label><b>{{scope.opt.name}}</b> <span style="color: grey;">- {{scope.opt.desc}}</span></q-item-label>
              </q-item-section>
            </q-item>
          </template>          
        </q-select>
        <q-btn icon-right="push_pin" padding="xs" margin="xs" @click="onSaveLabels" :disabled="!userSelectedLabelNamesChanged" title="Save changes.">
          <q-checkbox
          v-model="autoSaveLabels"
          size="xs"
          padding="xs"
          margin="xs"      
          color="grey-6"
          style="padding: 0px; margin: -4px;"
          title="! currently NOT IMPLEMENTED !  Automatically save changes when moving to another label. ! currently NOT IMPLEMENTED !"
          />
        </q-btn>
        <q-badge v-if="userSelectedLabelNamesChanged" color="grey-3" text-color="accent" label="...unsaved changes..."/>
        <q-btn icon="delete_forever" text-color="red" size="s" padding="xs" margin="xs" title="Remove selected time segment." @click="removeTimeSegmentShow = true;" />
        <q-dialog v-model="removeTimeSegmentShow" transition-show="rotate" transition-hide="rotate" class="q-pt-none" full-width full-height>
          <div class="q-pt-none column wrap justify-around content-around" style="background-color: white;"> 
          <q-btn icon="delete_forever" text-color="red" size="s" padding="xs" margin="xs" title="Remove selected time segment." @click="onRemoveTimeSegment" :disabled="saveLabelsLoading">
            Confirm to remove the currently selected time segment. This can not be undone.
            <q-spinner color="primary" size="1em" :thickness="2" v-if="saveLabelsLoading"/>
          </q-btn>
          <q-btn icon="home" text-color="green" size="s" padding="xs" margin="xs" title="Close dialog box." @click="removeTimeSegmentShow = false;"  :disabled="saveLabelsLoading">
            No I do not want to remove it after all.
          </q-btn>
          </div>
        </q-dialog>                   
        <q-space></q-space>        
      </div>
      <div v-if="newSegmentEnd !== undefined"  class="q-ml-lg row">
        New Time segment
        <span class="q-pa-sm">{{(newSegmentStart/sampleRate).toFixed(3)}} .. {{(newSegmentEnd/sampleRate).toFixed(3)}}</span>
        <q-select
          filled
          v-model="userSelectedLabelNames"
          :options="selectableLabels"
          label="Labels"
          style="width: 250px"
          dense
          multiple
          @update:model-value="userSelectedLabelNamesChanged = true"
          option-label="name"
          option-value="name"
          emit-value
          clearable
        >
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps">
              <q-item-section>
                <q-item-label><b>{{scope.opt.name}}</b> <span style="color: grey;">- {{scope.opt.desc}}</span></q-item-label>
              </q-item-section>
            </q-item>
          </template>
        </q-select>
        <q-badge color="grey-3" text-color="accent" label="...unsaved changes..."/>    
      </div>
      <q-space></q-space>
      <q-badge v-if="saveLabelsLoading" color="grey-3" text-color="accent" label="Sending label..."/>
      <q-badge v-if="saveLabelsError" color="grey-3" text-color="red" label="Error sending label. You may try again."/>
      <q-btn icon="add" size="xs" text-color="green" padding="xs" margin="xs" title="Add new time segment with start at current time position." @click="onNewTimeSegmentStart" v-if="newSegmentStart === undefined" :disabled="samplePos === undefined"/>
      <q-btn icon="cancel" size="xs" padding="xs" margin="xs" text-color="red" title="Cancel adding new time segment." @click="onNewTimeSegmentCancel" v-if="newSegmentStart !== undefined"/>
      <q-btn icon="navigation" label="Set end" size="xs" padding="xs" margin="xs" text-color="green" title="Set end time of new time segment at current time position." @click="onNewTimeSegmentEnd" v-if="newSegmentStart !== undefined && newSegmentEnd === undefined" :disabled="samplePos === undefined"/>
      <q-btn icon="push_pin" label="Save new segment" size="xs" padding="xs" margin="xs" text-color="green" title="Save new time segment with currently selected labels and switch back to segment selection view." @click="onNewTimeSegmentSave" v-if="newSegmentStart !== undefined && newSegmentEnd !== undefined"/>

    </q-toolbar>
    <q-separator/>    
    <div :class="sampleVisibility" style="margin-left: 15px; margin-right: 15px; position: relative;" ref="sliderDiv">
      <canvas ref="labelMap" style="position: absolute; top: 0px; left: 0px; bottom: 0px; pointer-events: none;"/>
      <q-slider v-model="canvasPixelPosX" :min="0" :max="spectrogramFullPixelLen - 1" @update:model-value="onSliderUpdate" @change="onSliderChange"/>
      <div style="position: absolute; top: 0px; left: 0px; pointer-events: none;" v-if="sampleRate !== undefined && samplePos !== undefined">
        <span style="font-weight: bold;">{{(samplePos/sampleRate).toFixed(3)}}</span>
      </div>
      <div style="position: absolute; top: 0px; right: 0px; pointer-events: none;" v-if="sampleRate !== undefined && sampleLen !== undefined">
        {{(sampleLen/sampleRate).toFixed(3)}}
      </div>
    </div>
    <q-separator/>      
    <div :class="sampleVisibility" style="position: relative;" ref="canvasContainer" :style="{height: player_fft_cutoff_range + 'px'}">
      <detail-view ref="detail" :sampleRate="sampleRate" :labels="labels" @save="onSaveDetailViewLabel"/>
      <canvas ref="spectrogram" style="position: absolute; top: 0px; left: 0px;" :width="canvasWidth" :height="player_fft_cutoff_range" :style="{width: canvasWidth + 'px', height: player_fft_cutoff_range + 'px'}" class="spectrogram" @mousedown="onCanvasMouseDown" @mousemove="onCanvasMouseMove" @mouseleave="onCanvasMouseleave" @contextmenu="onCanvasContextmenu"/>
      <q-linear-progress :value="spectrogramLoadedprogress" class="q-mt-md" size="25px" v-if="spectrogramLoadedprogress < 1 && spectrogramImagesErrorCount === 0" style="position: absolute; top: 0px; left: 0px; pointer-events: none;">
        <div class="absolute-full flex flex-center">
          <q-badge color="white" text-color="accent" label="loading spectrogram" />
        </div>
      </q-linear-progress>
      <q-badge v-if="spectrogramImagesErrorCount > 0" style="position: absolute; top: 0px; left: 0px;" color="white" text-color="accent">
        ERROR laoding spectrogram
        <q-btn color="grey" @click="spectrogramImagesErrorCount = 0; loadSpectrogramImage(spectrogramId, 0);">retry</q-btn>
      </q-badge>                
      <q-badge v-if="audioWaiting" color="yellow-14" text-color="accent" label="loading audio" style="position: absolute; top: 10px; right: 10px;"/>    
      <q-badge v-if="audioStalled" color="yellow-14" text-color="red" label="stalled loading audio" style="position: absolute; top: 30px; right: 10px;"/>

      <q-badge v-if="sampleLoading" color="grey-3" text-color="accent" label="loading metadata" style="position: absolute; top: 50px; left: 10px;"/>    
      <q-badge v-if="sampleError" color="grey-3" text-color="red" style="position: absolute; top: 80px; left: 10px;">
        error loading metadata
        <q-btn color="grey" @click="refreshSample">refresh</q-btn>
      </q-badge>

      <template v-if="staticLinesCanvasPosY !== undefined">
        <template v-for="staticLineCanvasPosY in staticLinesCanvasPosY" :key="staticLineCanvasPosY">
          <div v-if="staticLineCanvasPosY >= player_fft_cutoff_lower && staticLineCanvasPosY < player_fft_cutoff" style="position: absolute; pointer-events: none; left: 0px; right: 0px; height: 1px; background-color: rgba(0, 255, 255, 0.30);" :style="{bottom: (staticLineCanvasPosY - player_fft_cutoff_lower) + 'px',}"></div>
        </template>
      </template>

      <div v-if="mouseFreuqencyPos !== undefined" style="position: absolute; pointer-events: none; left: 0px; right: 0px; height: 1px; background-color: rgba(255, 255, 255, 0.41);" :style="{bottom: canvasMousePixelPosY + 'px',}"></div>
      <q-badge v-if="mouseFreuqencyPos !== undefined" style="position: absolute; pointer-events: none;" :style="{bottom: canvasMousePixelPosY + 'px', left: canvasMousePixelPosX + 'px',}" color="white" text-color="accent">
        <span v-html="mouseFreuqencyText"></span> kHz
        <span style="padding-left: 10px;">{{mouseTimePosText}}</span> s
      </q-badge> 
    </div>
    <q-separator/>  
   
  <div class="q-pa-md row" v-if="labelDefinitionsError">
    <q-badge text-color="red" color="grey-3">Error loading label definitions</q-badge>
    <q-btn color="grey" @click="refreshLabelDefinitions">refresh</q-btn>
  </div>

  <div class="q-pa-md row" v-if="selectedLabel !== undefined">
    <div class="q-ma-md" v-if="selectedLabel.generated_labels.length > 0">
      Generated labels
      <q-markup-table dense>
        <thead>
          <tr>
            <th class="text-left">Name</th>
            <th class="text-left">Reliability</th>
            <th class="text-left">Generator</th>
            <th class="text-left">Generation date</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(label, index) in selectedLabel.generated_labels" :key="index">
            <td class="text-left">{{label.name}}</td>
            <td class="text-left">{{label.reliability}}</td>
            <td class="text-left">{{label.generator}}</td>
            <td class="text-left">{{label.generation_date}}</td>
          </tr>
        </tbody>
      </q-markup-table>
    </div>

    <div class="q-ma-md" v-if="selectedLabel.labels.length > 0">
      Created labels
      <q-markup-table dense>
        <thead>
          <tr>
            <th class="text-left">Name</th>
            <th class="text-left">Creator</th>
            <th class="text-left">Creation date</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(label, index) in selectedLabel.labels" :key="index">
            <td class="text-left">{{label.name}}</td>
            <td class="text-left">{{label.creator}}</td>
            <td class="text-left">{{label.creation_date}}</td>
          </tr>
        </tbody>
      </q-markup-table>
    </div>
  </div>

  <div class="q-ma-md" v-if="labels.length > 0">
    Occurring labels:
    <q-badge 
      v-for="name in occurringLabels"
      :key="name" 
      color="grey-4" 
      :text-color="userSelectedLabelNamesSet.has(name) ? 'green' : 'grey-7'" 
      class="q-ma-sm" 
      @click="if(selectedLabel !== undefined) {addLabel(name);}" 
      :style="{cursor: selectedLabel === undefined ? 'default' : 'pointer'}"
      :title="selectedLabel === undefined ? 'Label occurring in one of the segments of this audio sample.' : 'Click to add label to selected segment.'"
    >
      {{name}}
    </q-badge>
  </div>

  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';

import AudioBrowser from 'components/browser';
import AudioSettings from 'components/settings';
import DetailView from 'components/detail';

export default defineComponent({
  name: 'Main',

  components: {
    AudioBrowser,
    AudioSettings,
    DetailView,
  },

  data() {
    return {
      sampleLoading: false,
      sampleError: false,
      sample: undefined,
      samplePos: undefined,
      sampleLen: undefined,
      spectrogramImages: [],
      imageNextIndex: undefined,
      spectrogramImagesLoadedCount: 0,
      spectrogramImagesErrorCount: 0,
      spectrogramId: 0,
      spectrogramImageMaxPixelLenUnaligned: 2048,
      spectrogramImageMaxSampleLenUnaligned: 134217728,
      canvasPixelPosX: 0,
      canvasPixelPosXrequest: undefined,
      canvasMovePixelStartX: undefined,
      canvasWidth: 1024,
      canvasMousePixelPosX: undefined,
      canvasMousePixelPosY: undefined,
      paintSpectrogramRequested: false,
      audio: undefined,
      audioPlaying: false,
      audioWaiting: false,
      audioStalled: false,
      sampleRate: undefined,
      sliderPanValue: undefined,
      selectedLabelIndex: undefined,
      autoMoveToLabelStart: true,
      userSelectedLabelNames: undefined,
      userSelectedLabelNamesChanged: false,
      autoSaveLabels: true,
      newSegmentStart: undefined,
      newSegmentEnd: undefined,
      labelDefinitions: [],
      labelDefinitionsLoading: false,
      labelDefinitionsError: false,
      saveLabelsLoading: false,
      saveLabelsError: false,
      labelSelectDialogShow: false,
      removeTimeSegmentShow: false,
    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,      
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      player_fft_window: state => state.project.player_fft_window,
      player_fft_step: state => state.project.player_fft_step,
      /*player_fft_cutoff: state => state.project.player_fft_cutoff,*/
      player_fft_cutoff_lower_frequency: state => state.project.player_fft_cutoff_lower_frequency,
      player_fft_cutoff_upper_frequency: state => state.project.player_fft_cutoff_upper_frequency,
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
      player_spectrum_shrink_Factor: state => state.project.player_spectrum_shrink_Factor,
      player_time_expansion_factor: state => state.project.player_time_expansion_factor,
      player_static_lines_frequency: state => state.project.player_static_lines_frequency, 
    }),
    /*player_fft_cutoff_lower_frequency() {
      return 5000;
    },
    player_fft_cutoff_upper_frequency() {
      return 10000;
    },*/
    player_fft_cutoff_lower() {
      let c = Math.floor((this.player_fft_cutoff_lower_frequency *  this.player_fft_window) / this.sampleRate);
      return c < 0 ? 0 : c > (this.player_fft_window / 2) - 1 ? (this.player_fft_window / 2) - 1 : c;
    },    
    player_fft_cutoff() {
      let c = Math.floor((this.player_fft_cutoff_upper_frequency *  this.player_fft_window) / this.sampleRate);
      return c < 1 ? 1 : c > this.player_fft_window / 2 ? this.player_fft_window / 2 : c;
    },
    player_fft_cutoff_range() {
      return this.player_fft_cutoff - this.player_fft_cutoff_lower;
    },
    spectrogramSettingsQuery() {
      var q = "&cutoff_lower=" + this.player_fft_cutoff_lower + "&cutoff=" + this.player_fft_cutoff + "&step=" + this.player_fft_step + "&window=" + this.player_fft_window + "&threshold=" + this.player_spectrum_threshold + "&intensity_max=" + this.player_fft_intensity_max;
      if(this.player_spectrum_shrink_Factor !== undefined && this.player_spectrum_shrink_Factor > 1) {
        q += "&shrink_factor=" + this.player_spectrum_shrink_Factor;
      }
      return q;
    },     
    selectedSampleId() {
      return this.$route.query.sample;
    },
    /*spectrogramImageMaxSampleLen() {
      return (this.spectrogramImageMaxPixelLen - 1) * (this.player_fft_step * this.player_spectrum_shrink_Factor) + this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) + this.player_fft_window;
    },*/
    spectrogramImageMaxPixelLen() {
      const p = Math.trunc((this.spectrogramImageMaxSampleLenUnaligned - this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) - this.player_fft_window) / (this.player_fft_step * this.player_spectrum_shrink_Factor)) + 1;
      return p < this.spectrogramImageMaxPixelLenUnaligned ? p : this.spectrogramImageMaxPixelLenUnaligned;      
    },
    spectrogramImageMaxSampleLen() {
      return (this.spectrogramImageMaxPixelLen - 1) * (this.player_fft_step * this.player_spectrum_shrink_Factor) + this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) + this.player_fft_window;
    },
    spectrogramFullPixelLen() {
      if(this.sampleLen === undefined) {
        return 0;
      }
      var p = Math.trunc((this.sampleLen - this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) - this.player_fft_window) / (this.player_fft_step * this.player_spectrum_shrink_Factor)) + 1;
      return p < 0 ? 0 : p;
    },
    spectrogramFullSampleLen() {
      if(this.spectrogramFullPixelLen < 1) {
        return 0;
      }
      return (this.spectrogramFullPixelLen - 1) * (this.player_fft_step * this.player_spectrum_shrink_Factor) + this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) + this.player_fft_window;
    },
    spectrogramImagesLen() {
      return Math.ceil(this.spectrogramFullPixelLen / this.spectrogramImageMaxPixelLen);
    },
    spectrogramLoadedprogress() {
      return this.spectrogramImagesLoadedCount / this.spectrogramImagesLen;
    },
    audioURL() {
      if(!this.sample) {
        return;
      }
      const baseURL = this.$api.defaults.baseURL;
      var url = baseURL + 'samples2/' + this.sample.id + '/audio';
      if(this.sampleRate !== this.playerSampleRate) {
        url += '?overwrite_sampling_rate=' + this.playerSampleRate;
      }
      return url;
    },
    staticLinesCanvasPosY() {
      return this.sampleRate === undefined || this.player_fft_window === undefined || this.player_static_lines_frequency === undefined || this.player_static_lines_frequency.length === 0 ? undefined : this.player_static_lines_frequency.map(staticLineFrequency => Math.round((staticLineFrequency * this.player_fft_window) / this.sampleRate));
    },    
    mouseFreuqencyPos() {
      return this.canvasMousePixelPosY === undefined || this.sampleRate === undefined || this.player_fft_window === undefined ? undefined : (((this.player_fft_cutoff_lower + this.canvasMousePixelPosY) * this.sampleRate) / this.player_fft_window);
    },
    mouseFreuqencyText() {
      return (this.mouseFreuqencyPos < 100000 ? (this.mouseFreuqencyPos < 10000 ? '&numsp;&numsp;' : '&numsp;' ) : '' ) + (this.mouseFreuqencyPos / 1000).toFixed(2);
    },
    mouseSamplePos() {
      if(this.canvasMousePixelPosX === undefined || !this.player_fft_step || !this.player_spectrum_shrink_Factor || this.canvasPixelPosX === undefined || this.player_time_expansion_factor === undefined || !this.canvasWidth) {
        return undefined;
      }
      return (this.canvasPixelPosX - Math.trunc(this.canvasWidth / 2) + this.canvasMousePixelPosX) * (this.player_fft_step * this.player_spectrum_shrink_Factor);
    },
    mouseTimePos() {
      if(this.mouseSamplePos === undefined || !this.sampleRate) {
        return undefined;
      }
      return this.mouseSamplePos / this.sampleRate;
    },
    mouseTimePosText() {
      return this.mouseTimePos === undefined ? '' : this.mouseTimePos.toFixed(3);
    },
    duration() {
      if(!this.sampleLen || !this.sampleRate) {
        return undefined;
      }
      return this.sampleLen / this.sampleRate;
    },
    durationHH() {
      if(this.duration === undefined) {
        return '--';
      }
      var h = Math.trunc(this.duration / 3600);
      var h10 = Math.trunc(h/10);
      var h1 = h%10; 
      return '' + h10 + '' + h1;
    },
    durationMM() {
      if(this.duration === undefined) {
        return '--';
      }
      var m = Math.trunc(this.duration / 60) % 60;
      var m10 = Math.trunc(m/10);      
      var m1 = m%10;
      return '' + m10 + '' + m1;
    },
    durationSS() {
      if(this.duration === undefined) {
        return '--';
      }
      var s = Math.trunc(this.duration % 60);
      var s10 = Math.trunc(s/10);
      var s1 = s%10;
      return '' + s10 + '' + s1;
      return (s < 10 ? '0' : '') + s.toFixed(3);
    },
    durationMS() {
      if(this.duration === undefined) {
        return '---';
      }
      var s = Math.trunc(this.duration * 1000) % 1000;
      var s100 = Math.trunc(s/100);
      var s10 = Math.trunc(s/10) % 10;
      var s1 = s % 10;
      return '' + s100 + '' + s10 + '' + s1;
    },
    sampleRatemhz() {
      if(this.sampleRate === undefined) {
        return '---';
      }
      var s = Math.trunc(this.sampleRate * 1000) % 1000;
      var s100 = Math.trunc(s/100);
      var s10 = Math.trunc(s/10) % 10;
      var s1 = s % 10;
      return '' + s100 + '' + s10 + '' + s1;
    },
    sampleVisibility() {
      return {
        'element-hidden': this.sample === undefined,
      };
    },
    playerSampleRate() {
      return this.sampleRate === undefined ? undefined : Math.trunc(this.sampleRate / this.player_time_expansion_factor);
    },
    labels() {
      return this.sample === undefined ? [] : this.sample.labels;
    },
    selectedLabel() {
      return this.selectedLabelIndex === undefined || this.selectedLabelIndex >= this.labels.length  ? undefined : this.labels[this.selectedLabelIndex];
    },
    selectableLabels() {
      return this.labelDefinitions;
    },
    occurringLabels() {
      var set = new Set();
      this.labels.forEach(label => {
        label.generated_labels.forEach(generated_label => {
          set.add(generated_label.name);
        });
        label.labels.forEach(e => {
          set.add(e.name);
        });        
      });
      return set;
    },
    userSelectedLabelNamesSet() {
      return new Set(this.userSelectedLabelNames);
    },
    labelIndices() {
      if(this.labels === undefined || this.labels.length === 0) {
        return [];
      }
      var indices = [];
      for (var i = 0; i < this.labels.length; i++) {
          indices.push(i);
      }
      return indices;
    },
  },

  methods: {
    paintSpectrogram() {      
      var canvasContainer = this.$refs.canvasContainer;
      this.canvasWidth = canvasContainer.clientWidth;
      //console.log(canvasContainer);
      var canvas = this.$refs.spectrogram;
      var ctx = canvas.getContext("2d");
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      if(this.sampleLen === undefined) {
        return;
      }
      const boxOffset = Math.trunc(canvas.width / 2);
      const drawPixelPosX = this.canvasPixelPosX - boxOffset;

      var imageIndexStart = Math.trunc(drawPixelPosX / this.spectrogramImageMaxPixelLen);
      if(imageIndexStart < 0) {
        imageIndexStart = 0;
      }
      if(imageIndexStart >= this.spectrogramImagesLen) {
        return;
      }
      var imageIndexEnd = Math.trunc((drawPixelPosX + (this.canvasWidth - 1)) / this.spectrogramImageMaxPixelLen);
      if(imageIndexEnd < 0) {
        return;
      }
       if(imageIndexEnd >= this.spectrogramImagesLen) {
        imageIndexEnd = this.spectrogramImagesLen - 1;
      }
      //console.log('draw');
      var next = undefined;
      for(var i = imageIndexStart; i <= imageIndexEnd; i++) {        
        var image = this.spectrogramImages[i];
        var canvasPixelX = i * this.spectrogramImageMaxPixelLen;
        var dstX = canvasPixelX - drawPixelPosX;
        var dstY = 0;
        //console.log('draw image ' + i + " at " + dstX + " of images " +  this.spectrogramImagesLen);
        if(image === undefined) {
          if(next === undefined) {
            next = i;
          }
          ctx.fillStyle = '#404040';
          ctx.fillRect(dstX, dstY, this.spectrogramImageMaxPixelLen, this.player_fft_cutoff_range);
        } else {
          ctx.drawImage(image, dstX, dstY);
        }
      }
      this.paintLabels();
      this.paintLabelMap();
      ctx.fillStyle = '#ffffff50';
      ctx.fillRect(boxOffset, 0, 1, this.player_fft_cutoff_range);
      ctx.fillStyle = '#00000050';
      ctx.fillRect(boxOffset - 1, 0, 1, this.player_fft_cutoff_range);
      ctx.fillRect(boxOffset + 1, 0, 1, this.player_fft_cutoff_range);

      /*ctx.beginPath();
      ctx.lineWidth = '1'; // width of the line
      ctx.strokeStyle = 'red'; // color of the line
      ctx.moveTo(boxOffset, 0); // begins a new sub-path based on the given x and y values.
      ctx.lineTo(boxOffset, this.player_fft_cutoff_range); // used to create a pointer based on x and y  
      ctx.stroke();*/

      if(next !== undefined) {
        this.imageNextIndex = next;
      }
      if(!this.audio.paused) {
        this.$nextTick(() => {
          //console.log("next");
          this.onAudioTimeupdate();
        });
      }
    },
    paintLabels() {
      var canvas = this.$refs.spectrogram;
      var ctx = canvas.getContext("2d");
      const boxOffset = Math.trunc(canvas.width / 2);
      const canvasPixelXmin = this.canvasPixelPosX - boxOffset;
      const canvasPixelXmax = this.canvasPixelPosX + (this.canvasWidth - 1);
      for(var i = 0; i < this.labels.length; i++) {
        var label = this.labels[i];
        var labelPixelXmin = Math.trunc(Math.trunc(label.start * this.sampleRate) / (this.player_fft_step * this.player_spectrum_shrink_Factor));
        var labelPixelXmax = Math.trunc(Math.trunc(label.end * this.sampleRate) / (this.player_fft_step * this.player_spectrum_shrink_Factor));
        //console.log(labelPixelXmin + ' ' + labelPixelXmax + '  ' + canvasPixelXmin + ' ' + canvasPixelXmax);
        if(canvasPixelXmin <= labelPixelXmax && canvasPixelXmax >= labelPixelXmin) {
          //console.log('fill ' + labelPixelXmin + ' ' + labelPixelXmax + '  ' + canvasPixelXmin + ' ' + canvasPixelXmax);
          ctx.fillStyle = i === this.selectedLabelIndex ? 'rgba(255,0,0,0.3)' : 'rgba(0,255,0,0.3)';
          ctx.fillRect(labelPixelXmin - canvasPixelXmin, 0, labelPixelXmax - labelPixelXmin + 1, this.player_fft_cutoff_range);
        }      
      }
      if(this.newSegmentStart !== undefined) {
        const end = this.newSegmentEnd === undefined ? this.samplePos : this.newSegmentEnd;
        var labelPixelXmin = Math.trunc(this.newSegmentStart / (this.player_fft_step * this.player_spectrum_shrink_Factor));
        //var labelPixelXmax = Math.trunc(end / (this.player_fft_step * this.player_spectrum_shrink_Factor));
        var labelPixelXmax = this.newSegmentEnd === undefined ? this.canvasPixelPosX : Math.trunc(this.newSegmentEnd / (this.player_fft_step * this.player_spectrum_shrink_Factor));
        if(canvasPixelXmin <= labelPixelXmax && canvasPixelXmax >= labelPixelXmin) {
          //console.log('fill ' + labelPixelXmin + ' ' + labelPixelXmax + '  ' + canvasPixelXmin + ' ' + canvasPixelXmax);
          ctx.fillStyle = 'rgba(0,0,255,0.3)';
          ctx.fillRect(labelPixelXmin - canvasPixelXmin, 0, labelPixelXmax - labelPixelXmin + 1, this.player_fft_cutoff_range);
        }           
      }
    },
    paintLabelMap() {
      var sliderDiv = this.$refs.sliderDiv;
      var canvas = this.$refs.labelMap;
      canvas.width = sliderDiv.clientWidth;
      canvas.height = sliderDiv.clientHeight;
      var width = canvas.width;
      var height = canvas.height;
      var ctx = canvas.getContext("2d");
      ctx.fillStyle = 'white';
      ctx.fillRect(0, 0, width, height);
      var duration = this.sampleLen / this.sampleRate;
      for(var i = 0; i < this.labels.length; i++) {
        if(i !== this.selectedLabelIndex) {
          var label = this.labels[i];
          var labelPixelXmin = Math.trunc(label.start * width / duration);
          var labelPixelXmax = Math.trunc(label.end * width / duration);
          ctx.fillStyle = 'rgba(0,255,0,0.3)';
          ctx.fillRect(labelPixelXmin, 0, labelPixelXmax - labelPixelXmin + 1, height);             
        }
      }
      if(this.selectedLabel !== undefined) {
        var label = this.selectedLabel;
        var labelPixelXmin = Math.trunc(label.start * width / duration);
        var labelPixelXmax = Math.trunc(label.end * width / duration);
        ctx.fillStyle = 'rgba(255,0,0,1)';
        ctx.fillRect(labelPixelXmin, 0, labelPixelXmax - labelPixelXmin + 1, height);
      }
    },
    loadSpectrogram() {
      if(Number.isFinite(this.spectrogramImagesLen)) {
        this.spectrogramId++;
        var id = this.spectrogramId;
        //console.log('loadSpectrogram ' + id + "    " + this.spectrogramImagesLen + "  " + this.spectrogramFullPixelLen + "  " + this.sampleLen);
        this.spectrogramImages = new Array(this.spectrogramImagesLen);
        this.spectrogramImagesLoadedCount = 0;
        this.spectrogramImagesErrorCount = 0;
        if(this.spectrogramImagesLen > 0) {   
          this.loadSpectrogramImage(id, 0);
        }
        this.paintSpectrogramRequested = true;
      }
    },
    async loadSpectrogramImage(id, imageIndex) {
      //console.log('loadSpectrogramImage ' + id + '  ' + imageIndex);
      if(this.spectrogramImages[imageIndex] === undefined) {
        try {
          var baseURL = this.$api.defaults.baseURL;
          var start_sample = imageIndex * this.spectrogramImageMaxSampleLen;
          var end_sample = start_sample + (this.spectrogramImageMaxSampleLen - 1);
          if(end_sample >= this.spectrogramFullSampleLen) {
            end_sample = this.spectrogramFullSampleLen - 1;
            //var p = Math.trunc((this.sampleLen - this.player_fft_step * (this.player_spectrum_shrink_Factor - 1) - this.player_fft_window) / (this.player_fft_step * this.player_spectrum_shrink_Factor)) + 1;
          }
          var image = new Image();
          image.src = baseURL + 'samples2/' + this.sample.id + '/spectrogram' + '?start_sample=' + start_sample + '&end_sample=' + end_sample + this.spectrogramSettingsQuery;
          await image.decode();
          if(id != this.spectrogramId) {
            return;
          }
          var imageBitmap = await createImageBitmap(image);
          if(id != this.spectrogramId) {
            return;
          }
          this.spectrogramImages[imageIndex] = imageBitmap;
          this.paintSpectrogramRequested = true;
          this.spectrogramImagesLoadedCount++;          
          var next = this.imageNextIndex;
          this.imageNextIndex = undefined;
          if(next == undefined) {
            next = imageIndex + 1;
          }
          if(next < this.spectrogramImagesLen) {            
            this.loadSpectrogramImage(id, next);
          } else if(this.spectrogramImagesLoadedCount < this.spectrogramImagesLen && this.spectrogramImagesLen > 0) {
            this.loadSpectrogramImage(id, 0);
          }           
        } catch {
          this.spectrogramImagesLoadedCount++;
          this.spectrogramImagesErrorCount++;         
        }        
      } else {
        var next = this.imageNextIndex;
        this.imageNextIndex = undefined;
        if(next == undefined) {
          next = imageIndex + 1;
        }
        if(next < this.spectrogramImagesLen) {            
          this.loadSpectrogramImage(id, next);
        } else if(this.spectrogramImagesLoadedCount < this.spectrogramImagesLen && this.spectrogramImagesLen > 0) {
          this.loadSpectrogramImage(id, 0);
        }
      }
    },
    onCanvasMouseDown(e) {      
      this.canvasMovePixelStartX = e.pageX;
      //console.log('onCanvasMouseDown ' + this.canvasMovePixelStartX);
    },
    onCanvasMouseMove(e) {
      if(this.canvasMovePixelStartX === undefined) {
        if(e.buttons == 1) { // left mouse button
          this.canvasMovePixelStartX = e.pageX;
        }
      } else {
        if(e.buttons == 1) { // left mouse button
          const mouseSpeedup = 8;
          //console.log('offsetX ' + (e.pageX - this.canvasMovePixelStartX));
          var deltaX = (e.pageX - this.canvasMovePixelStartX);
          var offsetX = deltaX;
          //if(Math.abs(deltaX) > 20) {
            var offsetX = deltaX * mouseSpeedup;
          //}
          //console.log('offsetX ' + offsetX);
          var newCanvasPixelPosX = this.canvasPixelPosX - offsetX;
          this.moveToCanvasPixelPosX(newCanvasPixelPosX);
          this.canvasMovePixelStartX = e.pageX;
        } else {
          this.canvasMovePixelStartX = undefined;
        }
      }
      var rect = this.$refs.spectrogram.getBoundingClientRect();
      this.canvasMousePixelPosX = e.clientX - rect.left;
      this.canvasMousePixelPosY = (this.player_fft_cutoff_range - 1) - (e.clientY - rect.top);
    },
    onCanvasMouseleave(e) {
      this.canvasMousePixelPosX = undefined;
      this.canvasMousePixelPosY = undefined;
    },
    paintSpectrogramRequestedAnimationFrame() {
      this.paintSpectrogram();
      this.paintSpectrogramRequested = false;
    },
    onAudioPlayButton() {
      //console.log("play audio A " + this.audio.currentTime);
      this.audio.play();
      //console.log("play audio B " + this.audio.currentTime);
    },
    onAudioPauseButton() {
      this.audio.pause();
    },    
    onAudioPlaying() {
      this.audioPlaying = true;
      this.audioWaiting = false;
    },
    onAudioPause() {
      this.audioPlaying = false;
    },
    onAudioTimeupdate() {
      const tPlayer = this.audio.currentTime;
      var newSamplePos = tPlayer * this.playerSampleRate;
      if(newSamplePos < 0) {
        newSamplePos = 0;
      }
      if(newSamplePos >= this.sampleLen) {
        newSamplePos = this.sampleLen - 1;
      }
      newSamplePos = Math.trunc(newSamplePos);
      var newCanvasPixelPosX = newSamplePos / (this.player_fft_step * this.player_spectrum_shrink_Factor);
      if(newCanvasPixelPosX < 0) {
        newCanvasPixelPosX = 0;
      }
      if(newCanvasPixelPosX >= this.spectrogramFullPixelLen) {
        newCanvasPixelPosX = this.spectrogramFullPixelLen - 1;
      }
      newCanvasPixelPosX = Math.trunc(newCanvasPixelPosX);
      this.samplePos = newSamplePos;
      this.canvasPixelPosX = newCanvasPixelPosX;
      //console.log(tPlayer + "  " + this.samplePos + "  " + this.canvasPixelPosX + "  " + this.player_fft_step + "  " + this.player_spectrum_shrink_Factor);
    },
    moveToSamplePos(newSamplePos) {
      if(newSamplePos < 0) {
        newSamplePos = 0;
      }
      if(newSamplePos >= this.sampleLen) {
        newSamplePos = this.sampleLen - 1;
      }
      var tPlayer = newSamplePos / this.playerSampleRate;
      this.audio.currentTime = tPlayer;
      //console.log("set audio " + tPlayer + '   ' + this.audio.currentTime + '   ' + this.audio.duration);
    },
    moveToCanvasPixelPosX(newCanvasPixelPosX) {
      //console.log("moveToCanvasPixelPosX  " + newCanvasPixelPosX);
      this.canvasPixelPosX = newCanvasPixelPosX;
      var newSamplePos = newCanvasPixelPosX * (this.player_fft_step * this.player_spectrum_shrink_Factor);
      this.moveToSamplePos(newSamplePos);
    },
    onSliderUpdate(newValue) {
      this.sliderPanValue = newValue;
    },
    onSliderChange() {
      if(this.sliderPanValue != undefined) {
        this.moveToCanvasPixelPosX(this.sliderPanValue);
      }
    },
    onAudioWaiting() {
      this.audioWaiting = true;
    },
    onAudioStalled() {
      this.audioStalled = true;
    },
    onAudioLoadeddata() {
      this.audioStalled = false;
    },
    onAudioCanplay() {
      this.audioStalled = false;
    },
    onAudioCanplaythrough() {
      this.audioStalled = false;
    },
    async refreshSample() {
      if(this.audio !== undefined) {
        this.audio.pause();
        this.audioPlaying = false;
      }
      if(this.selectedSampleId === undefined) {
        return;
      }
      //console.log("querySample");
      try {
        var urlPath = 'samples2/' + this.selectedSampleId;
        var params = {samples: true, sample_rate: true, labels: true,};
        this.sampleLoading = true;
        this.sampleError = false;
        var response = await this.$api.get(urlPath, {params});
        this.sampleLoading = false;
        this.sampleError = false;
        var sample = response.data?.sample;
        //console.log(sample);
        this.sample = sample;
        this.sampleLen = sample.samples;
        this.sampleRate = sample.sample_rate;
      } catch(e) {
        this.sampleLoading = false;
        this.sampleError = true;
        console.log(e);
      }
    },
    onMovePrevLabel() {
      if(this.selectedLabelIndex === undefined) {
        this.selectedLabelIndex = this.labels.length - 1;
      } else if(this.selectedLabelIndex === 0) {
        this.selectedLabelIndex = undefined;
      } else {
        this.selectedLabelIndex = this.selectedLabelIndex - 1;
      }
    },
    onMoveNextLabel() {
      if(this.selectedLabelIndex === undefined) {
        this.selectedLabelIndex = 0;
      } else if(this.selectedLabelIndex >= this.labels.length - 1) {
        this.selectedLabelIndex = undefined;
      } else {
        this.selectedLabelIndex = this.selectedLabelIndex + 1;
      }
    },
    onMoveToLabelStart() {
      if(this.selectedLabel !== undefined && this.selectedLabel.start !== undefined) {
        var newSamplePos = this.selectedLabel.start * this.sampleRate;
        this.moveToSamplePos(newSamplePos);
      }
    },
    async onSaveLabels() {
      const labelNames = !this.userSelectedLabelNames ? [] : this.userSelectedLabelNames;
      try {
        var urlPath = 'samples2/' + this.selectedSampleId;
        var names = !this.userSelectedLabelNames ? [] : this.userSelectedLabelNames;
        var data = {actions: [{action: 'set_label_names', names: names, start: this.selectedLabel.start, end: this.selectedLabel.end,}]};
        this.saveLabelsLoading = true;
        this.saveLabelsError = false;
        var response = await this.$api.post(urlPath, data);
        this.userSelectedLabelNamesChanged = false;
        this.saveLabelsLoading = false;
        this.saveLabelsError = false;
        this.refreshSample();
        this.$q.notify({type: 'positive', message: 'Changed labels saved.'});
      } catch(e) {
        this.saveLabelsLoading = false;
        this.saveLabelsError = true;
        console.log(e);
      }
    },
    onNewTimeSegmentStart() {
      this.selectedLabelIndex = undefined;
      this.newSegmentStart = this.samplePos;
      this.newSegmentEnd = undefined;
      this.userSelectedLabelNames = undefined;
      this.userSelectedLabelNamesChanged = false;
    },
    onNewTimeSegmentCancel() {
      this.selectedLabelIndex = undefined;
      this.newSegmentStart = undefined;
      this.newSegmentEnd = undefined;
      this.userSelectedLabelNames = undefined;
      this.userSelectedLabelNamesChanged = false;      
    },
    onNewTimeSegmentEnd() {
      this.newSegmentEnd = this.samplePos;
      this.userSelectedLabelNames = undefined;
      this.userSelectedLabelNamesChanged = false;
    },
    async onNewTimeSegmentSave() {
      const labelNames = !this.userSelectedLabelNames ? [] : this.userSelectedLabelNames;
      try {
        var urlPath = 'samples2/' + this.selectedSampleId;
        var names = !this.userSelectedLabelNames ? [] : this.userSelectedLabelNames;
        var data = {actions: [{action: 'add_label', names: names, start: this.newSegmentStart/this.sampleRate, end: this.newSegmentEnd/this.sampleRate,}]};
        this.saveLabelsLoading = true;
        this.saveLabelsError = false;
        var response = await this.$api.post(urlPath, data);
        this.saveLabelsLoading = false;
        this.saveLabelsError = false;
        this.selectedLabelIndex = undefined;
        this.newSegmentStart = undefined;
        this.newSegmentEnd = undefined;
        this.userSelectedLabelNames = undefined;
        this.userSelectedLabelNamesChanged = false;
        this.refreshSample();
        this.$q.notify({type: 'positive', message: 'New segment and new labels saved.'});
      } catch(e) {
        this.saveLabelsLoading = false;
        this.saveLabelsError = true;
        console.log(e);
      }
    },
    async refreshLabelDefinitions() {
      this.labelDefinitions = [];
      if(this.project !== undefined) {
        try {
          var urlPath = 'projects/' + this.project + "/label_definitions";
          var params = {};
          this.labelDefinitionsLoading = true;
          this.labelDefinitionsError = false;
          var response = await this.$api.get(urlPath, {params});
          this.labelDefinitionsLoading = false;
          this.labelDefinitionsError = false;
          var d = response.data?.label_definitions;
          if(d) {
            var prev = '?';
            for(var i=0; i<d.length; i++) {
              var name = d[i].name;
              var cur = name.length > 0 ? name[0] : '?';
              if(prev !== cur) {
                d[i].n = true;
              }
              prev = cur;              
            }
          }
          this.labelDefinitions = d;          
        } catch(e) {
          this.labelDefinitionsLoading = false;
          this.labelDefinitionsError = true;
          console.log(e);
        }
      }
    },
    addLabel(name) {
      if(!this.userSelectedLabelNames) {
        this.userSelectedLabelNames = name;
        this.userSelectedLabelNamesChanged = true;
      } else if(!this.userSelectedLabelNamesSet.has(name)){
        this.userSelectedLabelNames.push(name);
        this.userSelectedLabelNamesChanged = true;
      }
    },
    onCanvasContextmenu(e) {
      e.preventDefault();
      //console.log('onCanvasContextmenu');
      if(this.sample !== undefined && this.mouseSamplePos !== undefined) {
        this.$refs.detail.sample = this.sample;      
        this.$refs.detail.samplePos = this.mouseSamplePos; 
        this.$refs.detail.userSelectedLabelNames = this.userSelectedLabelNames; 
        this.$refs.detail.selectableLabels = this.selectableLabels;
        this.$refs.detail.show = true;
      }
    },
    onSaveDetailViewLabel(e) {
      console.log('onSaveDetailViewLabel');
      console.log(e);
      this.newSegmentStart = e.start;
      this.newSegmentEnd = e.end;
      this.userSelectedLabelNames = e.names;      
      this.onNewTimeSegmentSave();
    },
    async onRemoveTimeSegment() {
      try {
        var urlPath = 'samples2/' + this.selectedSampleId;
        var data = {actions: [{action: 'remove_label', start: this.selectedLabel.start, end: this.selectedLabel.end,}]};
        this.saveLabelsLoading = true;
        this.saveLabelsError = false;
        var response = await this.$api.post(urlPath, data);
        this.userSelectedLabelNamesChanged = false;
        this.saveLabelsLoading = false;
        this.saveLabelsError = false;
        this.selectedLabelIndex = undefined;
        this.refreshSample();
        this.$q.notify({type: 'positive', message: 'Remove time segment saved.'});
        this.removeTimeSegmentShow = false;        
      } catch(e) {
        this.saveLabelsLoading = false;
        this.saveLabelsError = true;
        console.log(e);
      }
    },
  },

  watch: {
    selectedSampleId: {
      immediate: true,   
      async handler() {
        this.refreshSample();
      }
    },
    async sample(oldSample, newSample) {
      var resetPos = oldSample === undefined || newSample === undefined || oldSample.id !== newSample.id;
      this.$nextTick( () => {
        if(resetPos) {
          this.canvasPixelPosX = 0;
        }
        this.loadSpectrogram();
      });        
    },
    canvasPixelPosX() {
      this.paintSpectrogramRequested = true;
    },
    paintSpectrogramRequested() {
      if(this.paintSpectrogramRequested) {
        requestAnimationFrame(() => {
          this.paintSpectrogramRequestedAnimationFrame();
        });
        
      }
    }, 
    spectrogramSettingsQuery() {
      this.$nextTick( () => {
        this.loadSpectrogram();
      });
    },
    audioURL() {
      this.audio.pause();
      this.audioPlaying = false;
      this.audio.src = this.audioURL;
    },
    labels() {
      if(this.labels.length == 0) {
        this.selectedLabelIndex = undefined;
      } else if(this.selectedLabelIndex === undefined || this.selectedLabelIndex >= this.labels.length) {
        this.selectedLabelIndex = undefined;
      }
    },
    selectedLabelIndex() {
      this.paintSpectrogramRequested = true;
      if(this.autoMoveToLabelStart) {
        this.onMoveToLabelStart();
      }
    },
    selectedLabel() {
      this.userSelectedLabelNames = this.selectedLabel === undefined ? undefined : this.selectedLabel.labels.map(label => label.name);
      this.userSelectedLabelNamesChanged = false;
    },
    autoMoveToLabelStart() {
      if(this.autoMoveToLabelStart) {
        this.onMoveToLabelStart();
      }
    },
    project: {
      immediate: true,   
      async handler() {
        this.refreshLabelDefinitions();
      }
    },    
  },
  async mounted() {
    this.audio = new Audio();    
    this.audio.addEventListener('playing', e => this.onAudioPlaying(e));
    this.audio.addEventListener('pause', e => this.onAudioPause(e));
    this.audio.addEventListener('timeupdate', e => this.onAudioTimeupdate(e));
    this.audio.addEventListener('waiting', e => this.onAudioWaiting(e));
    this.audio.addEventListener('stalled', e => this.onAudioStalled(e));
    this.audio.addEventListener('loadeddata', e => this.onAudioLoadeddata(e));
    this.audio.addEventListener('canplay', e => this.onAudioCanplay(e));
    this.audio.addEventListener('canplaythrough', e => this.onAudioCanplaythrough(e));
   },  
})
</script>

<style scoped>
.spectrogram {
  /*border-style: solid;
  border-width: 1px;
  border-color: black;*/
  /*background-color: #e5e5f7;*/
  opacity: 1;
  background-image:  repeating-linear-gradient(45deg, #202020 25%, transparent 25%, transparent 75%, #202020 75%, #202020), repeating-linear-gradient(45deg, #202020 25%, #404040 25%, #404040 75%, #202020 75%, #202020);  background-position: 0 0, 10px 10px;
  background-size: 20px 20px;
  cursor: crosshair;
}

.spectrogram:active{
  cursor: col-resize;
 }

.element-hidden {
  visibility: hidden;
}

.label-definition-n::first-letter {
  color: black;
  font-size: 120%;
}


</style>
