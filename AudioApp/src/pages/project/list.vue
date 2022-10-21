<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3" id="viewWidth">
      <q-btn @click="replay()">Play</q-btn>
      <q-btn @click="stop()">Stop</q-btn>
        <q-space />
        <q-btn @click="onPrev" :loading="indexActionLoading">Prev</q-btn>
        Index  [<b>{{index}}</b>]
        <q-btn @click="onNext" :loading="indexActionLoading">Next</q-btn>
        <span v-if="indexActionError" style="color: red;">{{indexActionError}}</span>
        <q-space />
        <q-btn @click="$refs.listmanager.show = true;" icon="menu_book" title="Select audio sample."  padding="xs">List [<b>{{listId}}</b>]</q-btn>
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
      <span style="padding-left: 20px;">{{workingEntry.title}}</span>
    </q-toolbar>
    <q-toolbar>      
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
            <td class="text-left">{{label.name}}</td>
            <td class="text-left">{{label.reliability}}</td>
            <td class="text-left">{{label.generator}}</td>
            <td class="text-left">{{label.generation_date}}</td>
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
          <td class="text-left">{{label.name}}</td>
          <td class="text-left">{{label.creator}}</td>
          <td class="text-left">{{label.creation_date}}</td>
        </tr>
      </tbody>
    </q-markup-table>
    </div>
    </q-toolbar>
    <q-toolbar v-if="sampleLabel === undefined">
      No labels for worklist entry at sample.
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
      return parseInt(this.$route.query.index);
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
          this.$router.replace({path: this.$route.path, query: {...this.$route.query, index: this.workingEntry.index}});
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
          /*if(this.audio !== undefined) {
            this.audio.pause();
            this.audio.ontimeupdate = undefined;
            this.audio.oncanplay = undefined;
            //this.audio.src = undefined;  // loads undefined
            this.audio = undefined;
          }*/
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
          this.replay();
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
          var start_sample = this.workingEntry.start * this.sample.sample_rate;
          var end_sample = this.workingEntry.end * this.sample.sample_rate;
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
      if(this.audio !== undefined) {
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
        this.audio.currentTime = this.workingEntry.start;
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
    }
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

</style>
