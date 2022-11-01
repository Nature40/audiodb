<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3" id="viewWidth">
      <q-btn @click="stop()" icon="stop" padding="xs" push></q-btn>      
      <q-btn @click="replay()" icon="play_arrow" padding="xs" push></q-btn>
      <q-checkbox v-model="autoplay" label="autoplay" unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: autoplay ? 'black' : 'grey'}" />      
      <q-space />
      <q-btn @click="onPrev" icon="skip_previous" :loading="indexActionLoading" padding="xs" push></q-btn>
        [[<b>{{Number.isFinite(index) ? (index + 1) : '-'}}</b>]]
      <q-btn @click="onNext" icon="skip_next" :loading="indexActionLoading" padding="xs" push></q-btn>
      <q-checkbox v-model="skipdone" label="skipdone" unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: skipdone ? 'black' : 'grey'}" />      
      <span v-if="indexActionError" style="color: red;">{{indexActionError}}</span>
      <q-space />
      <q-btn @click="$refs.listmanager.show = true;" icon="menu_book" title="Select audio sample." push padding="xs" no-caps>List [<b>{{listId}}</b>]</q-btn>
      <list-manager ref="listmanager" @set_worklist="setWorklist($event)" />
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
      <span style="padding-left: 20px;">{{workingEntry.start}} - {{workingEntry.end}}</span>
      <span style="padding-left: 20px; color: grey;">{{Number.isFinite(currentTime) ? currentTime.toFixed(2) : '---'}}</span>
      <q-space />


      <q-select
        filled
        v-model="userSelectedLabelNames"
        :options="labelDefinitions"
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
      >
        <template v-slot:append>
          <!--<q-icon name="edit" @click.stop.prevent="labelSelectDialogShow = true" />--> 
          <q-icon name="apps" @click.stop.prevent="labelSelectDialogShow = true" />          
          <q-dialog v-model="labelSelectDialogShow" transition-show="rotate" transition-hide="rotate" class="q-pt-none" full-width full-height>
            <div class="q-pt-none column wrap justify-start content-around fit" style="position: relative; background-color: white;">
              <q-btn dense icon="close" v-close-popup style="position: absolute; top: 0px; right: 0px;">
                <q-tooltip>Close</q-tooltip>
              </q-btn>
              <q-badge  v-for="labelDefinition in labelDefinitions" :key="labelDefinition.name" @click="addLabel(labelDefinition.name);"  color="grey-3" :text-color="userSelectedLabelNamesSet.has(labelDefinition.name) ? 'green' : 'grey-7'" style="width: 200px; margin: 1px; overflow: hidden;" class="text-h6" :title="labelDefinition.desc">
                <span v-if="labelDefinition.n" class="label-definition-n">{{labelDefinition.name}}</span>
                <span v-else class="label-definition-r">{{labelDefinition.name}}</span>
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

      <q-btn push @click="onSubmit" no-caps :loading="saveLabelsLoading">Submit</q-btn>
      <q-checkbox v-model="statusDone" label="Set 'done'." title="On submit, set user label status to 'done'." unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: statusDone ? 'black' : 'grey'}" />      
      <q-checkbox v-model="autonext" label="autonext" unchecked-icon="highlight_off" checked-icon="task_alt" dense size="xs" class="q-pl-xs q-pr-sm" :style="{color: autonext ? 'black' : 'grey'}" />      

      <q-space />
      <q-badge v-if="workingEntry.title"
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
        <canvas ref="spectrogram" :class="{blur: loadingSpectrogram, 'no-blur': !loadingSpectrogram}" />
        <div v-if="loadingSpectrogram" style="position: absolute; color: red; top: 0px;">Loading spectrogram.</div>
      </div>
      <q-space />
    </q-toolbar>
    <q-toolbar v-if="sampleLabel !== undefined">

    <div class="q-ma-md" v-if="sampleLabel.generated_labels.length > 0">
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
          <tr v-for="(label, index) in sampleLabel.generated_labels" :key="index">
            <td class="text-left" style="cursor: pointer;" @click="addLabel(label.name, !userSelectedLabelNamesSet.has(label.name));">
              <q-badge color="grey-1" :text-color="userSelectedLabelNamesSet.has(label.name) ? 'green' : 'red'" class="q-ma-sm">
                {{label.name}}
              </q-badge>
            </td>
            <td class="text-left">{{label.reliability}}</td>
            <td class="text-left">{{label.generator}}</td>
            <td class="text-left">{{label.generation_date === undefined ? '-' : label.generation_date.slice(0,16)}}</td>
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
          <td class="text-left">{{label.creation_date === undefined ? '-' : label.creation_date.slice(0,16)}}</td>
        </tr>
      </tbody>
    </q-markup-table>
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
  name: 'List',

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
    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,
      player_fft_cutoff_lower_frequency: state => state.project.player_fft_cutoff_lower_frequency,
      player_fft_window: state => state.project.player_fft_window,
      player_fft_cutoff_upper_frequency: state => state.project.player_fft_cutoff_upper_frequency,
      player_fft_step: state => state.project.player_fft_step,
      player_spectrum_threshold: state => state.project.player_spectrum_threshold,
      player_fft_intensity_max: state => state.project.player_fft_intensity_max,
      player_spectrum_shrink_Factor: state => state.project.player_spectrum_shrink_Factor,
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
    sampleLabel() {
      if(this.workingEntry === undefined || this.sample === undefined || this.sample.labels === undefined) {
        return undefined;
      }
      const label = this.sample.labels.find(e => e.start === this.workingEntry.start && e.end === this.workingEntry.end);
      return label;
    },
    userSelectedLabelNamesSet() {
      return new Set(this.userSelectedLabelNames);
    },    
  },

  methods: {
    setActionStatus(loading, error) {
      this.indexActionLoading = loading;
      this.indexActionError = error;
    },
    async onPrev() {
      try {
        this.stop();
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists/' + this.listId + '/last';
        var params = {};
        if(Number.isFinite(this.index)) {
          params.last = this.index - 1;
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
      try {
        this.stop();
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists/' + this.listId + '/first';
        var params = {};
        if(Number.isFinite(this.index)) {
          params.first = this.index + 1;
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
            if(this.currentTime >= this.workingEntry.end) {
              this.audio.pause();
              this.audio.oncanplay = undefined;
              this.audio.currentTime = this.workingEntry.start;
            }
          };
          if(this.autoplay) {
            this.replay();
          }
        }
      } catch (e) {
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
        var canvas = this.$refs.spectrogram;
        var ctx = canvas.getContext("2d");
        if(this.imageBitmap !== undefined) {      
          canvas.width = this.imageBitmap.width;
          canvas.height = this.imageBitmap.height;
          ctx.clearRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(this.imageBitmap, 0, 0);
        } else {
          ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
      } catch(e) {
        console.log(e);
      }
    },
    replay() {
      if(this.workingEntry !== undefined && this.audio !== undefined) {
        this.audio.currentTime = this.workingEntry.start;
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
        if(this.workingEntry !== undefined) {
          this.audio.currentTime = this.workingEntry.start;
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
        console.log(e);
        this.$q.notify({type: 'negative', message: 'Error submitting label. You may try again to submit.'});
      }
    },        
  },

  watch: {
    listId() {
      this.refreshWorkingEntry();
    },
    index() {
      if(Number.isFinite(this.index)) {
        if(this.workingEntry === undefined || this.workingEntry.index !== this.index) {
          this.refreshWorkingEntry();
        }
      } else {
        this.setWorkingEntry(undefined);
      }
    },
    project: {
      immediate: true,   
      async handler() {
        console.log('project changed');
        this.refreshLabelDefinitions();
      }
    },
  },  
  async mounted() {
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

</style>
