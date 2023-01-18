<template>
  <q-page class="fit column content-center bg-grey-1" id="list_view_page">
    <q-toolbar class="bg-grey-3" id="viewWidth">
      <q-btn @click="stop()" icon="stop" padding="xs" push title="Stop audio playback."></q-btn>      
      <q-btn @click="replay()" icon="play_arrow" padding="xs" push title="Play back the audio segment starting from beginning."></q-btn>
      <q-checkbox v-model="autoplay" label="auto-play" unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: autoplay ? 'black' : 'grey'}" title="When moving in the list of audio segments, directly play back the audio."/>      
      <q-space />
      <q-btn @click="onPrev" icon="skip_previous" :loading="indexActionLoading" padding="xs" push title="Move to previous audio segment in the list." />
        <q-btn-dropdown 
          :label="'[[' + (Number.isFinite(index) ? (index + 1) : '-') + ']]'" 
          dense 
          padding="xs" 
          push size="md" 
          :loading="indexActionLoading" 
          dropdown-icon="more_vert"
          title="Directly jump to a worklist entry."
        >
          <div style="background-color: #eaeaea;" class="column">
            <b style="text-align: center;">Directly jump to entry position in worklist.</b>
            <div class="row justify-between">
              <q-btn 
                push 
                no-caps 
                @click="jumpToLast(jumpPos - 1)" 
                :disabled="!Number.isFinite(jumpPos)"
                title="Jump to requested position or, if position is not available to next lower available position."
              >
                (&lt;) Jump to 
              </q-btn>
              <q-input
                v-model.number="jumpPos"
                type="number"
                outlined
                style="width: 100px"
                dense
                bg-color="white"
                title="Requested position. To Jump to that position, click the jump-button on the left or on the right (or press enter key)."
                @keyup.enter.prevent="onEnterKeyJump"
              />
              <q-btn 
                push 
                no-caps 
                @click="jumpToFirst(jumpPos - 1)" 
                :disabled="!Number.isFinite(jumpPos)"
                title="Jump to requested position or, if position is not available to next higher available position."
              >
                Jump to (&gt;)
              </q-btn>
            </div>  
            <div class="row justify-between">
              <q-btn push no-caps @click="jumpToFirst()" title="Jump to first available position.">Jump to first</q-btn>
              <q-btn push no-caps @click="jumpToLast()" title="Jump to last available position.">Jump to last</q-btn>
            </div>
            <div>Range of worklist entry positions: <b>1</b> to <b>{{worklistEntryCount}}</b></div>
            <div>Note: If your requested entry position is not available, jump to next (respective previous) available position.</div>                        
            <div>Note: If option 'skip done' is checked, positions marked as 'done' are not available in the worklist.</div> 
          </div>
        </q-btn-dropdown>
      <q-btn @click="onNext" icon="skip_next" :loading="indexActionLoading" padding="xs" push title="Move to next audio segment in the list." />
      <q-checkbox v-model="skipdone" label="skip 'done'" unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: skipdone ? 'black' : 'grey'}" title="When moving in the list of audio segments, skip all entries that are already marked as 'done'."/>      
      <span v-if="indexActionError" style="color: red;">{{indexActionError}}</span>
      <q-space />
      <q-btn @click="$refs.listmanager.show = true;" icon="menu_book" title="Select worklist of audio segments." push padding="xs" no-caps>List [<b>{{listId}}</b>]</q-btn>
      <list-manager ref="listmanager" @set_worklist="setWorklist($event)" />
      <q-btn @click="onFullscreenClick" :icon="isFullscreen ? 'close_fullscreen' : 'fullscreen'" title="Toggle fullscreen." round padding="xs" no-caps class="q-ml-sm"></q-btn>

      <q-btn @click="dialoghelpShow = true;" icon="help" dense flat style="margin-left: 10px;" title="Get help."></q-btn>
      <q-dialog
        v-model="dialoghelpShow"
        :maximized="dialoghelpMaximizedToggle"
        transition-show="slide-down"
        transition-hide="slide-up"
      >
        <q-card class="bg-grey-3 text-black">
          <q-bar>
            <q-icon name="help_outline" />
            <div>Help</div>
            <q-space />
            <q-btn dense flat icon="window" @click="dialoghelpMaximizedToggle = false" v-show="dialoghelpMaximizedToggle">
              <q-tooltip v-if="dialoghelpMaximizedToggle">Minimize</q-tooltip>
            </q-btn>
            <q-btn dense flat icon="crop_square" @click="dialoghelpMaximizedToggle = true" v-show="!dialoghelpMaximizedToggle">
              <q-tooltip v-if="!dialoghelpMaximizedToggle">Maximize</q-tooltip>
            </q-btn>
            <q-btn dense flat icon="close" v-close-popup>
              <q-tooltip>Close</q-tooltip>
            </q-btn>
          </q-bar>

          <q-card-section class="q-pt-none">
            <div class="text-h6">Work list view</div>
              <i>Hover mouse over a control to show a tooltip description.</i>                                  
          </q-card-section>
          
          <q-card-section class="q-pt-none">
            <div class="text-h6">Work list</div>
              <ul>
                <li>Click the <b>book-button</b> on the upper right to select a work list from all available work lists.</li>
                <li>The label on the book-button shows the currently chosen work list.</li>
              </ul>          
          </q-card-section>


          <q-card-section class="q-pt-none">
            <div class="text-h6">Work list entry</div>
              <i>Controls for the work list entry are on the upper middle.</i>
              <ul>
                <li>Click the <b>move-left-button</b> / <b>move-right-button</b> the move to the previous / next entry in the work list.</li>
                <li>Click <b>work-list-entry-number-button</b> in the middle to directly jump to a work list entry.</li>
                <li>Activate <b>skip 'done'-checkbox</b> to skip work list entries that are marked as 'done' already.</li>
              </ul>          
          </q-card-section>


          <q-card-section class="q-pt-none">
            <div class="text-h6">Play audio</div>
              <i>Audio of currently selected work list entry.</i>
              <br><i><b>Current audio position</b> is marked by the vertical line that moves over the spectrogram when playing audio.</i>                   
              <ul>
                <li>Click the <b>stop-button</b> on the upper left to stop audio play before finish.</li>                
                <li>Click the <b>play-button</b> on the upper left. Click it again to replay.</li>
                <li>Activate <b>auto-play-checkbox</b> to directly play audio when another work list entry is chosen.</li>
                <li>Click with <b>left mouse button</b> on the spectrogram to start audio play from that position.</li>
              </ul>           
          </q-card-section>

          <q-card-section class="q-pt-none">
            <div class="text-h6">Labelling</div>
              <i>Controls for labelling are on the upper row below the play and worklist select row.</i>
              <br><i>On the left, click the <b>link-symbol</b> to open the full audio sample of the current worklist entry in the audio view.</i>

              <br><br>
              <p>Label selection:</p>
              <ul>
                <li>Click the <b>label on the right</b> to select the label suggested by the worklist entry.</li>                
                <li>Click <b>labels form the tables of generated or user selected labels</b> below the spectrogram.</li>
                <li>Click on the <b>middle selection control</b> to select labels from the list of defined labels.</li>
                <li>Click on the <b>right side grid symbol of the middle selection control</b> to open a grid of defined labels.</li>
                <li>Click on the <b>left side edit symbol of the middle selection control</b> to type a custom label name. Only use this if there is no fitting label in the defined label list.</li>
              </ul> 

              <p>Submitting:</p>
              <ul>
                <li>Click the <b>submit-button</b> to save the currently selected labels for that worklist entry.</li>                
                <li>Optionally, click the <b>comment-bubble-symbol</b> right of the submit-button to write a comment for that worklist entry before submit.</li> 
                <li>Activate <b>set-done-checkbox</b> to mark this worklist entry as 'done' on submit.</li>
                <li>Activate <b>auto-next-checkbox</b> to move to next worklist entry on submit.</li>
              </ul>           
          </q-card-section>          
        </q-card>
      </q-dialog>

    </q-toolbar>
    <q-toolbar class="bg-grey-4" v-if="workingEntry !== undefined && sample !== undefined">
      <a :href="'#/projects/' + project + '/main?sample=' + sample.id" target="_blank" rel="noopener noreferrer" title="Open full audio sample view at new tab.">
        <q-icon name="open_in_new" />
      </a>        
      <span style="padding-left: 10px;" v-if="sample.location">{{sample.location}}</span>
      <span style="padding-left: 10px;" v-if="sample.date">{{sample.date}}</span>
      <span style="padding-left: 5px;" v-if="sample.time">{{sample.time}}</span>
      <span style="padding-left: 10px; font-family: monospace;" v-if="(sample.location === undefined || sample.date === undefined) && sample.device">{{sample.device}}</span>
      <span style="padding-left: 10px;" v-if="(!sample.location || !sample.device) && sample.date === undefined">{{sample.id}}</span>
      <!--<span style="padding-left: 20px;">{{workingEntry.start}} - {{workingEntry.end}}</span>-->
      <q-badge color="grey-4" text-color="grey-8" style="margin-left: 50px;" v-if="Number.isFinite(workingEntry.start) && Number.isFinite(workingEntry.end)">
        Segment  
        {{workingEntry.start.toFixed(3)}}
        ..
        {{workingEntry.end.toFixed(3)}}
        (
        <b>{{(workingEntry.end - workingEntry.start).toFixed(3)}}</b>
        )
      </q-badge>
      <span style="padding-left: 20px; color: grey;">{{Number.isFinite(currentTime) && player_time_expansion_factor ? (currentTime / player_time_expansion_factor).toFixed(2) : '---'}}</span>
      <q-space />

      <q-select
        filled
        v-model="userSelectedLabelNames"
        :options="selectableLabels"
        :label="userSelectedLabelNames === undefined || userSelectedLabelNames === null || userSelectedLabelNames.length === 0 ? '(none)' : 'Labels'"
        style="min-width: 400px"
        dense
        multiple
        @update:model-value="userSelectedLabelNamesChanged = true"
        option-label="name"
        option-value="name"
        emit-value
        clearable
        ref="selectLabel"
        title="Select labels form the list of defined labels. Click the grid icon on the right to open a grid of defined labels. Click the edit icon on the left to enter a custom label."
      >
        <template v-slot:prepend>
          <div class="cursor-pointer" @click.stop.prevent="">
            <q-icon name="edit" /> 
            <q-popup-edit v-model="customLabel" v-slot="scope" title="Add a custom label" @before-show="customLabel = undefined" @save="addCustomLabel">
              <span style="color: green;">Confirm label by return key or by add-button.</span> 
              <q-input v-model="scope.value" dense autofocus @keyup.enter="scope.set" hint="Type your new custom label here.">
                <template v-slot:after>
                  <q-btn 
                    @click="scope.set" 
                    icon="keyboard_return" 
                    padding="xs" 
                    push 
                    :disabled="scope.value === undefined || scope.value === null || scope.value.length === 0"
                    title="Add the new custom label to your list of currently selected labels."
                  />
                </template>
              </q-input>
              <br><i style="color: #b11c1c;"><q-icon name="speaker_notes" size="xs"/> Only add a custom label if there is no corresponding item in the given list of labels.</i>
              <br><br><i><q-icon name="speaker_notes" size="xs"/> To open the list of labels, click outside of this box to close it and then click at the area on the right next to this edit button to open the drop down label selector.</i>                
              </q-popup-edit>
          </div>                   
        </template>      
        <template v-slot:append>
          <q-icon name="apps" @click.stop.prevent=" labelSelectDialogShow = true" />          
          <q-dialog v-model="labelSelectDialogShow" transition-show="rotate" transition-hide="rotate" class="q-pt-none" full-width full-height>
            <div class="q-pt-none column wrap justify-start content-around fit" style="position: relative; background-color: white;">
              <q-btn dense icon="close" v-close-popup style="position: absolute; top: 0px; right: 0px;">
                <q-tooltip>Close</q-tooltip>
              </q-btn>
              <q-badge 
                v-for="labelDefinition in selectableLabels" 
                :key="labelDefinition.name" 
                @click="addLabel(labelDefinition.name);"  
                color="grey-3" 
                :text-color="userSelectedLabelNamesSet.has(labelDefinition.name) ? 'green' : 'grey-7'" 
                style="width: 200px; margin: 1px; overflow: hidden;" 
                class="text-h6" 
                :title="labelDefinition.desc"
              >
                <span v-if="labelDefinition.n" class="label-definition-n">{{labelDefinition.name}}</span>
                <span v-else class="label-definition-r">{{labelDefinition.name}}</span>
              </q-badge>
            </div>
          </q-dialog>                    
        </template>
        <template v-slot:option="scope">
          <q-item v-bind="scope.itemProps">
            <q-item-section>
              <q-item-label>
                <b>{{scope.opt.name}}</b>
                <span style="color: grey;" v-if="scope.opt.desc !== undefined && scope.opt.desc.length > 0"> - {{scope.opt.desc}}</span>
              </q-item-label>
            </q-item-section>
          </q-item>
        </template>          
      </q-select>

      <q-btn push @click="onSubmit" no-caps :loading="saveLabelsLoading" title="Submit currently selected labels and, if enabled, set this audio segment as 'done' and move to next audio segment in the list.">Submit</q-btn>
      <q-btn-dropdown 
        dense 
        padding="xs" 
        flat 
        size="sm" 
        :loading="indexActionLoading" 
        dropdown-icon="rate_review" 
        style="color: rgba(64, 96, 136, 0.55);"
        title="Add comment to current audio segment."
        ref="commentDropdown"
      >
        <b>Comment on current audio segment</b>
        <q-input 
          v-model="labelComment" 
          dense 
          autofocus 
          hint="Type your comment here. It will be saved next time when you click the submit-button." 
          style="min-width: 600px;"
          @keyup.enter.prevent="$refs.commentDropdown.hide()"
        />
        <i>To close this box, press enter key or click outside of this box.</i>
      </q-btn-dropdown>
      <q-checkbox v-model="statusDone" label="Set 'done'." title="On submit, set user label status to 'done'." unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: statusDone ? 'black' : 'grey'}" />      
      <q-checkbox v-model="autonext" label="auto-next" unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: autonext ? 'black' : 'grey'}" title="On submit, move to next audio segment in the list."/>      

      <q-space />
      <q-badge v-if="workingEntry.title" title="Add to selected labels."
        color="grey-4" 
        :text-color="userSelectedLabelNamesSet.has(workingEntry.title) ? 'green' : 'red'" class="q-ma-sm" @click="addLabel(workingEntry.title, !userSelectedLabelNamesSet.has(workingEntry.title));" 
        style="cursor: pointer;"
      >
        {{workingEntry.title}}
      </q-badge>
    </q-toolbar>
    <q-toolbar v-show="workingEntry !== undefined && sample !== undefined">      
      <q-space />
      <div style="position: relative;">
        <canvas 
          ref="spectrogram" 
          class="spectrogram"
          :class="{blur: loadingSpectrogram, 'no-blur': !loadingSpectrogram}" 
          @mousemove="onCanvasMouseMove" 
          @mouseleave="onCanvasMouseleave"
          @click="onCanvasClick"
        />
        <div v-if="spectrogramPos !== undefined" class="spectrogram-position" :style="{top: 0 + 'px', left: spectrogramPos + 'px',}" />
        <div v-if="spectrogramPos !== undefined" class="spectrogram-position" :style="{bottom: 0 + 'px', left: spectrogramPos + 'px',}" />
        <div v-if="loadingSpectrogram" style="position: absolute; color: red; top: 0px;">Loading spectrogram.</div>
        <div v-if="canvasMousePixelPosY !== undefined" style="position: absolute; pointer-events: none; left: 0px; right: 0px; height: 1px; background-color: rgba(255, 255, 255, 0.3);" :style="{bottom: canvasMousePixelPosY + 'px',}" />        
        <div v-if="mouseFrequencyPos !== undefined" style="position: absolute; pointer-events: none; background-color: rgba(255, 255, 255, 0.5); border-radius: 10px;" :style="{bottom: canvasMousePixelPosY + 'px', left: canvasMousePixelPosX + 'px',}">
          <span v-html="mouseFrequencyText"></span> kHz
        </div>        
      </div>
      <q-space />
    </q-toolbar>
    <q-toolbar v-if="sampleLabel !== undefined">
    <div class="row">
      <div class="q-ma-md" v-if="sampleLabel.generated_labels.length > 0">
        Generated labels
        <q-markup-table dense title="Treats">
          <thead>
            <tr>
              <th class="text-left">Name</th>
              <th class="text-left">Confidence</th>
              <th class="text-left">Generator</th>
              <th class="text-left">Generation date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(label, index) in sampleLabel.generated_labels" :key="index">
              <td class="text-left" style="cursor: pointer;" @click="addLabel(label.name, !userSelectedLabelNamesSet.has(label.name));">
                <q-badge color="grey-1" :text-color="userSelectedLabelNamesSet.has(label.name) ? 'green' : 'red'" class="q-ma-sm">
                  {{label.name}}
                </q-badge>
              </td>
              <td class="text-left">{{isFinite(label.reliability) ? label.reliability.toFixed(3) : ''}}</td>
              <td class="text-left">{{label.generator}}</td>
              <td class="text-left">{{label.generation_date === undefined ? '' : label.generation_date.slice(0,16)}}</td>
            </tr>
          </tbody>
        </q-markup-table>
      </div>
      <div class="q-ma-md" v-if="sampleLabel.labels.length > 0">
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
          <tr v-for="(label, index) in sampleLabel.labels" :key="index">
            <td class="text-left" style="cursor: pointer;" @click="addLabel(label.name, !userSelectedLabelNamesSet.has(label.name));">
              <q-badge color="grey-1" :text-color="userSelectedLabelNamesSet.has(label.name) ? 'green' : 'red'" class="q-ma-sm">
                {{label.name}}
              </q-badge>
            </td>
            <td class="text-left">{{label.creator}}</td>
            <td class="text-left">{{label.creation_date === undefined ? '' : label.creation_date.slice(0,16)}}</td>
          </tr>
        </tbody>
      </q-markup-table>
      </div>
      <div class="column">
        <div v-if="sampleLabel.label_status !== undefined"><b>Label status: </b> {{sampleLabel.label_status}} <q-icon name="done" color="green" v-if="sampleLabel.label_status === 'done'" /></div>
        <div v-if="sampleLabel.comment !== undefined"><b>Comment: </b> {{sampleLabel.comment}}</div>
      </div>      
    </div>
    </q-toolbar>
    <q-toolbar v-if="workingEntry !== undefined && sampleLabel === undefined">
      No labels for worklist entry at sample.
    </q-toolbar>
    <q-toolbar v-if="workingEntry === undefined">
      No list entry selected.
    </q-toolbar>
  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';
import ListManager from 'components/list-manager';


export default defineComponent({
  name: 'WorklistView',

  components: {
    ListManager
  },

  data() {
    return {
      indexActionLoading: false,
      indexActionError: undefined,
      workingEntry: undefined,
      sample: undefined,
      imageBitmap: undefined,
      audio: undefined,
      currentTime: undefined,
      loadingSpectrogram: false,
      autoplay: false,
      labelDefinitions: [],
      labelDefinitionsLoading: false,
      labelDefinitionsError: false,    
      labelSelectDialogShow: false,
      userSelectedLabelNames: undefined,
      userSelectedLabelNamesChanged: false,  
      autonext: true,
      saveLabelsLoading: false,
      saveLabelsError: false,
      statusDone: true,
      skipdone: true, 
      canvasMousePixelPosX: undefined, 
      canvasMousePixelPosY: undefined,
      isFullscreen: false,
      fft_step: undefined,      
      shrink_Factor: undefined,
      customLabelDialogShow: false, 
      customLabel: undefined,
      customLabels: [],  
      labelComment: undefined,
      jumpPos: 1,
      worklistEntryCount: undefined,
      dialoghelpShow: false,
      dialoghelpMaximizedToggle: false,
    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,
      player_fft_cutoff_lower_frequency: state => state.project.player_fft_cutoff_lower_frequency,
      player_fft_window: state => state.project.player_fft_window,
      player_fft_cutoff_upper_frequency: state => state.project.player_fft_cutoff_upper_frequency,
      //player_fft_step: state => state.project.player_fft_step,
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
      //player_spectrum_shrink_Factor: state => state.project.player_spectrum_shrink_Factor,
      player_time_expansion_factor: state => state.project.player_time_expansion_factor,
    }),
    listId() {
      return this.$route.query.list;
    },
    index() {
      if(this.$route.query.index === undefined) {
        return undefined;
      }
      const i = parseInt(this.$route.query.index);
      if(!Number.isFinite(i)) {
        return undefined;
      }
      return i - 1;
    },
    sampleRate() {
      return this.sample === undefined || this.sample.sample_rate === undefined ? undefined : this.sample.sample_rate;
    },
    player_fft_cutoff_lower() {
      if(this.sampleRate === undefined) {
        return undefined;
      }
      let c = Math.floor((this.player_fft_cutoff_lower_frequency *  this.player_fft_window) / this.sampleRate);
      return c < 0 ? 0 : c > (this.player_fft_window / 2) - 1 ? (this.player_fft_window / 2) - 1 : c;
    },
    player_fft_cutoff() {
      if(this.sampleRate === undefined) {
        return undefined;
      }
      let c = Math.floor((this.player_fft_cutoff_upper_frequency *  this.player_fft_window) / this.sampleRate);
      return c < 1 ? 1 : c > this.player_fft_window / 2 ? this.player_fft_window / 2 : c;
    },
    player_fft_cutoff_range() {
      if(this.player_fft_cutoff === undefined || this.player_fft_cutoff_lower === undefined) {
        return undefined;
      }
      return this.player_fft_cutoff - this.player_fft_cutoff_lower;
    },    
    sampleLabel() {
      if(this.workingEntry === undefined || this.sample === undefined || this.sample.labels === undefined) {
        return undefined;
      }
      //const label = this.sample.labels.find(e => e.start === this.workingEntry.start && e.end === this.workingEntry.end);
      const label = this.sample.labels.find(e => 
        (e.start - 0.001) <= this.workingEntry.start 
        && this.workingEntry.start <= (e.start + 0.001) 
        && (e.end - 0.001) <= this.workingEntry.end 
        && this.workingEntry.end <= (e.end + 0.001)
      );
      return label; 
    },
    userSelectedLabelNamesSet() {
      return new Set(this.userSelectedLabelNames);
    },
    mouseFrequencyPos() {
      return this.canvasMousePixelPosY === undefined || this.sampleRate === undefined || this.player_fft_window === undefined ? undefined : (((this.player_fft_cutoff_lower + this.canvasMousePixelPosY) * this.sampleRate) / this.player_fft_window);
    },
    mouseFrequencyText() {
      return (this.mouseFrequencyPos < 100000 ? (this.mouseFrequencyPos < 10000 ? '&numsp;&numsp;' : '&numsp;' ) : '' ) + (this.mouseFrequencyPos / 1000).toFixed(2);
    },
    spectrogramPos() {
      if(this.currentTime === undefined 
        || this.currentTime < 0 
        || this.sampleRate === undefined 
        || this.sampleRate < 1
        || this.fft_step === undefined
        || this.fft_step < 1
        || this.shrink_Factor === undefined
        || this.shrink_Factor < 1
        || this.workingEntry === undefined
        || this.workingEntry.start === undefined
        || this.workingEntry.start < 0
        || this.player_time_expansion_factor === undefined        
      ) {
        return undefined;
      }
      return Math.floor((((this.currentTime / this.player_time_expansion_factor) - this.workingEntry.start) * this.sampleRate) / (this.fft_step * this.shrink_Factor));
    },
    selectableLabels() {
      return this.labelDefinitions.concat(this.customLabels);
    },
    playerSampleRate() {
      return this.sampleRate === undefined ? undefined : Math.trunc(this.sampleRate / this.player_time_expansion_factor);
    },            
  },

  methods: {
    setActionStatus(loading, error) {
      this.indexActionLoading = loading;
      this.indexActionError = error;
    },
    onPrev() {
      if(Number.isFinite(this.index)) {      
        this.jumpToLast(this.index - 1); 
      } else {
        this.jumpToLast();
      }       
    },
    async jumpToLast(last) {
      try {
        this.stop();
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists/' + this.listId + '/last';
        var params = {};
        if(Number.isFinite(last)) {
          params.last = last;
        }
        if(this.skipdone) {
          params.skip_done = true;
        }
        var response = await this.$api.get(urlPath, {params});
        this.setWorkingEntry(response.data);
        this.setActionStatus(false, undefined);
      } catch(e) {
        this.setWorkingEntry(undefined);
        this.setActionStatus(false, e.response && e.response.data ? (e.response.data.error ? e.response.data.error : e.response.data) : 'error');
        //console.log(e);
      }
    },
    async onNext() {
      if(Number.isFinite(this.index)) {      
        this.jumpToFirst(this.index + 1);
      } else {
        this.jumpToFirst();
      }      
    },
    async jumpToFirst(first) {
      try {
        this.stop();
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists/' + this.listId + '/first';
        var params = {};
        if(Number.isFinite(first)) {
          params.first = first;
        }
        if(this.skipdone) {
          params.skip_done = true;
        }        
        var response = await this.$api.get(urlPath, {params});
        this.setWorkingEntry(response.data);
        this.setActionStatus(false, undefined);
      } catch(e) {
        this.setWorkingEntry(undefined);
        this.setActionStatus(false, e.response && e.response.data ? (e.response.data.error ? e.response.data.error : e.response.data) : 'error');
        //console.log(e);
      }
    },
    async refreshWorkingEntry() {
      try {
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists/' + this.listId + '/first';
        var params = {};
        if(Number.isFinite(this.index)) {
          params.first = this.index;
        }
        var response = await this.$api.get(urlPath, {params});
        this.setWorkingEntry(response.data);
        this.setActionStatus(false, undefined);
      } catch(e) {
        this.setWorkingEntry(undefined);
        this.setActionStatus(false, e.response && e.response.data ? (e.response.data.error ? e.response.data.error : e.response.data) : 'error');
        //console.log(e);
      }
    },
    setWorkingEntry(workingEntry) {
      this.workingEntry = workingEntry;
      if(this.workingEntry !== undefined) {
        if(this.workingEntry.index !== this.index) {
          this.$router.replace({path: this.$route.path, query: {...this.$route.query, index: (this.workingEntry.index + 1)}});
        }
      } else {
        if(this.$route.query.index !== undefined) {
          this.$router.replace({path: this.$route.path, query: {...this.$route.query, index: undefined}});
        }
      }
      this.runWorkingEntry();
    },
    async runWorkingEntry() {
      await this.runWorkingEntry_stage_1();
      await this.runWorkingEntry_stage_2b();
      await this.runWorkingEntry_stage_3();
      await this.runWorkingEntry_stage_2a();      
    },
    async runWorkingEntry_stage_1() {
      if(this.workingEntry !== undefined) {
        try {
          this.setActionStatus(true, undefined);
          var urlPath = 'samples2/' + this.workingEntry.sample;
          var params = {sample_rate: true, labels: true};
          var response = await this.$api.get(urlPath, {params});
          this.sample = response.data.sample;
          this.setActionStatus(false, undefined);
        } catch(e) {
          this.setActionStatus(false, e.response && e.response.data ? (e.response.data.error ? e.response.data.error : e.response.data) : 'error');
          console.log('runWorkingEntry_stage_1');
          console.log(e);
        }
      } else {
        this.sample = undefined;
      }
    },
    async runWorkingEntry_stage_2a() {
      try {
        if(this.sample !== undefined) {
          var apiUrl = this.$store.getters.api('samples2', this.sample.id, 'audio');
          if(this.sampleRate !== this.playerSampleRate) {
            apiUrl += '?overwrite_sampling_rate=' + this.playerSampleRate;
          }          
          var url = new URL(apiUrl);
          if(this.audio === undefined) {
            this.audio = new Audio(url.href);
          } else {
            this.audio.src = url.href;
          }
          this.audio.ontimeupdate = undefined;
          this.audio.oncanplay = undefined;
          
          this.audio.ontimeupdate = (event) => {
            this.currentTime = this.audio.currentTime;
            if(this.player_time_expansion_factor && this.currentTime >= (this.workingEntry.end * this.player_time_expansion_factor)) {
              this.audio.pause();
              this.audio.oncanplay = undefined;
              this.audio.currentTime = this.workingEntry.start * this.player_time_expansion_factor;
            }
          };
          if(this.autoplay) {
            this.replay();
          }
        }
      } catch (e) {
        console.log('runWorkingEntry_stage_2a');
        console.log(e);
      }
    },
    async runWorkingEntry_stage_2b() {
      var loadingSpectrogramCurrentIndex = this.workingEntry.index;
      try {
        this.loadingSpectrogram = true;
        if(this.sample !== undefined) {
          var start_sample = Math.floor(this.workingEntry.start * this.sample.sample_rate);
          var end_sample = Math.floor(this.workingEntry.end * this.sample.sample_rate);
          var apiUrl = this.$store.getters.api('samples2', this.sample.id, 'spectrogram');
          var url = new URL(apiUrl);
          url.searchParams.append('start_sample', start_sample);
          url.searchParams.append('end_sample', end_sample);
          url.searchParams.append('cutoff_lower', this.player_fft_cutoff_lower);
          url.searchParams.append('cutoff', this.player_fft_cutoff);
          url.searchParams.append('threshold', this.player_spectrum_threshold);
          url.searchParams.append('intensity_max', this.player_fft_intensity_max);

          const maxWidth = document.getElementById('viewWidth').clientWidth;
          var entrySampleCount = end_sample - start_sample + 1;
          var {fft_step, shrink_Factor} = this.getShrinking(entrySampleCount, maxWidth);
          this.fft_step = fft_step;
          this.shrink_Factor = shrink_Factor;

          url.searchParams.append('window', this.player_fft_window);
          url.searchParams.append('step', fft_step);
          url.searchParams.append('shrink_factor', shrink_Factor);
          var image = new Image();
          if(loadingSpectrogramCurrentIndex !== this.workingEntry.index) {
            return;
          }
          image.src = url.href;
          if(loadingSpectrogramCurrentIndex !== this.workingEntry.index) {
            return;
          }
          await image.decode();
          if(loadingSpectrogramCurrentIndex !== this.workingEntry.index) {
            return;
          }
          const imageBitmap = await createImageBitmap(image);
          if(loadingSpectrogramCurrentIndex !== this.workingEntry.index) {
            return;
          }
          this.imageBitmap = imageBitmap;
        } else {
          this.imageBitmap = undefined;
        }
      } catch (e) {
        console.log('runWorkingEntry_stage_2b');
        console.log(e);
      } finally {
        if(loadingSpectrogramCurrentIndex === this.workingEntry.index) {
          this.loadingSpectrogram = false;
        }
      }      
    },
    runWorkingEntry_stage_3() {
      this.repaint(); 
    },
    repaint() {
      try {      
        const canvas = this.$refs.spectrogram;
        if(canvas) {
          const ctx = canvas.getContext("2d");
          if(this.imageBitmap !== undefined) {      
            canvas.width = this.imageBitmap.width;
            canvas.height = this.imageBitmap.height;
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.drawImage(this.imageBitmap, 0, 0);
          } else {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
          }
        }
      } catch(e) {
        console.log('repaint');
        console.log(e);
      }
    },
    replay(startTime) {
      if(this.player_time_expansion_factor && this.workingEntry !== undefined && this.audio !== undefined) {
        this.audio.currentTime = startTime === undefined ? this.workingEntry.start * this.player_time_expansion_factor : startTime;
        this.audio.oncanplay = (event) => {
          this.audio.play();
        };
        this.$nextTick(() => {
          this.audio.play();
        });
      }
    },
    stop() {
      if(this.audio !== undefined) {
        this.audio.pause();
        this.audio.oncanplay = undefined;
        if(this.player_time_expansion_factor && this.workingEntry !== undefined) {
          this.audio.currentTime = this.workingEntry.start * this.player_time_expansion_factor;
        }
      }
    },
    pixelLenToSampleCount(pixelLen, fft_step, shrink_Factor) {
      if(pixelLen < 1) {
        return 0;
      }
      return (pixelLen - 1) * (fft_step * shrink_Factor) + fft_step * (shrink_Factor - 1) + this.player_fft_window;
    },
    getShrinking(entrySampleCount, maxWidth) {
      var fft_step = Math.floor(this.player_fft_window / 16);
      if(fft_step < 1) {
        fft_step = 1;
      }
      var shrink_Factor = 1;
      var maxSampleCount = this.pixelLenToSampleCount(maxWidth, fft_step, shrink_Factor);
      while(maxSampleCount < entrySampleCount) {
        fft_step *= 2;
        if(Math.floor(this.player_fft_window / 4) < fft_step) {
          fft_step = Math.floor(this.player_fft_window / 4);
          break;
        }
        maxSampleCount = this.pixelLenToSampleCount(maxWidth, fft_step, shrink_Factor);
      }
      maxSampleCount = this.pixelLenToSampleCount(maxWidth, fft_step, shrink_Factor);
      shrink_Factor = Math.ceil(entrySampleCount / maxSampleCount);
        if(shrink_Factor < 1) {
          shrink_Factor = 1;
        }
        if(shrink_Factor > 256) {
          shrink_Factor = 256;
        }
        return {
          shrink_Factor: shrink_Factor,
          fft_step: fft_step,
        };
    },
    setWorklist(worklist) {
      console.log('setWorklist');
      console.log('from ' + this.listId + ' to ' + worklist.id);
      if(this.listId !== worklist.id) {
        this.$router.replace({path: this.$route.path, query: {...this.$route.query, list: worklist.id, index: undefined}});
        console.log('done');
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
          console.log('refreshLabelDefinitions');
          console.log(e);
        }
      }
    },
    addLabel(name, addOrRemove) {
      if(addOrRemove === undefined || addOrRemove) {
        if(!this.userSelectedLabelNames) {
          this.userSelectedLabelNames = [name];
          this.userSelectedLabelNamesChanged = true;
        } else if(!this.userSelectedLabelNamesSet.has(name)){
          this.userSelectedLabelNames.push(name);
          this.userSelectedLabelNamesChanged = true;
        }
      } else {
        if(this.userSelectedLabelNames !== undefined && this.userSelectedLabelNamesSet.has(name)) {
          const i = this.userSelectedLabelNames.indexOf(name);
          if(i >= 0) {
            this.userSelectedLabelNames.splice(i, 1);
          }
        }
      }
    },
    async onSubmit() {
      try {
        var urlPath = 'samples2/' + this.workingEntry.sample;
        var names = !this.userSelectedLabelNames ? [] : this.userSelectedLabelNames;
        var action = {action: 'set_label_names', names: names, start: this.workingEntry.start, end: this.workingEntry.end};
        if(this.labelComment !== undefined && this.labelComment !== null && this.labelComment.length > 0) {
          action.comment = this.labelComment;
        }
        action.set_label_status = this.statusDone ? 'done' : 'open';
        var data = {actions: [action]};
        this.saveLabelsLoading = true;
        this.saveLabelsError = false;
        var response = await this.$api.post(urlPath, data);
        this.userSelectedLabelNamesChanged = false;
        this.saveLabelsLoading = false;
        this.saveLabelsError = false;
        this.runWorkingEntry_stage_1();
        this.$q.notify({type: 'positive', message: 'Submitted.'});
        if(this.autonext) {
          this.onNext();
        }
      } catch(e) {
        this.saveLabelsLoading = false;
        this.saveLabelsError = true;
        console.log('onSubmit');
        console.log(e);
        this.$q.notify({type: 'negative', message: 'Error submitting label. You may try again to submit.'});
      }
    },
    onCanvasMouseMove(e) {
      var rect = this.$refs.spectrogram.getBoundingClientRect();
      this.canvasMousePixelPosX = e.clientX - rect.left;
      this.canvasMousePixelPosY = (this.player_fft_cutoff_range - 1) - (e.clientY - rect.top);
    },
    onCanvasMouseleave(e) {
      this.canvasMousePixelPosX = undefined;
      this.canvasMousePixelPosY = undefined;
    },
    onCanvasClick(e) {
      const rect = this.$refs.spectrogram.getBoundingClientRect();
      const xPos = e.clientX - rect.left;
      const startTime = (((xPos * this.fft_step * this.shrink_Factor) / this.sampleRate) + this.workingEntry.start) * this.player_time_expansion_factor;
      console.log('onCanvasClick ' + xPos + '   ' + startTime);
      this.replay(startTime);
    },
    onFullscreenClick() {
      if(document.fullscreenElement) {
        document.exitFullscreen();
      } else {
        //const e = document.getElementById('list_view_page'); // bugs in select elements at fullscreen
        const e = document.documentElement;
        if(e.requestFullscreen) {
          e.requestFullscreen();
        } else if (e.webkitRequestFullscreen) {
          e.webkitRequestFullscreen();
        }
      }    
    },
    addCustomLabel(customLabel) {
      //console.log('addCustomLabel');
      //console.log(customLabel);
      if(customLabel === undefined || customLabel === null || customLabel.length === 0) {
        return;
      }
      if(this.customLabels.some(e => e.name === customLabel)) {
        return;
      }
      const e = {name: customLabel, desc: ''};
      this.customLabels.push(e);
      this.$refs.selectLabel.add(e);
      //console.log(this.customLabels);
    },
    async refeshWorklistEntryCount() {
      try {
        if(this.listId != undefined) {
          this.worklistEntryCount = undefined;
          var urlPath = 'worklists/' + this.listId;
          var params = {};       
          var response = await this.$api.get(urlPath, {params});
          this.worklistEntryCount = response.data.size;
        }
      } catch(e) {
        console.log(e);
      }
    },
    onEnterKeyJump() {
      if(Number.isFinite(this.jumpPos)) {
        this.jumpToFirst(this.jumpPos - 1)
      }           
    },
  },

  watch: {
    listId: {
      immediate: true,   
      async handler() {
        this.refreshWorkingEntry();
        this.labelComment = undefined;
        this.refeshWorklistEntryCount();
      }      
    },
    index() {
      if(Number.isFinite(this.index)) {
        if(this.workingEntry === undefined || this.workingEntry.index !== this.index) {
          this.refreshWorkingEntry();
        }
      } else {
        this.setWorkingEntry(undefined);
      }
      this.labelComment = undefined;
    },
    project: {
      immediate: true,   
      async handler() {
        console.log('project changed');
        this.refreshLabelDefinitions();
        this.labelComment = undefined;
      }
    },
    workingEntry() {
      this.labelComment = undefined;
    },
  },  
  async mounted() {
    document.addEventListener('fullscreenchange', (e) => {
      this.isFullscreen = document.fullscreenElement ? true : false;
    });
    this.isFullscreen = document.fullscreenElement ? true : false;    
    this.refreshWorkingEntry();
  },    
})
</script>

<style scoped>

.blur {
  filter: blur(20px);
  transition: all 2s ease-in;
}

.no-blur {
  transition: all 0.25s ease-out;
}

.label-definition-n::first-letter {
  color: black;
  font-size: 120%;
}

.label-definition-r::first-letter {
  color: rgb(97, 97, 97);
  font-size: 120%;
}

.spectrogram {
  display: block; /* remove bottom border */
  cursor: crosshair;
}

.spectrogram-position {
  position: absolute; 
  z-index: 1; 
  width: 1px;
  height: 128px;
  background-color: #ff0000b0;
  box-shadow: 0px 0px 4px red;
  /*transition: all 0.05s linear;*/
}

</style>
