<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="replay()">Play</q-btn>
      <q-btn @click="stop()">Stop</q-btn>
        <q-space />
        <q-btn @click="onPrev" :loading="indexActionLoading">Prev</q-btn>
        Index  [<b>{{index}}</b>]
        <q-btn @click="onNext" :loading="indexActionLoading">Next</q-btn>
        <span v-if="indexActionError" style="color: red;">{{indexActionError}}</span>
        <q-space />
        List [<b>{{listId}}</b>]
    </q-toolbar>
    <q-toolbar class="bg-grey-3" v-if="workingEntry !== undefined && sample !== undefined">
      {{sample.id}} :: {{workingEntry.start}} - {{workingEntry.end}} <span style="padding-left: 20px; color: grey;">{{currentTime.toFixed(2)}}</span>
    </q-toolbar>
    <q-toolbar>
      <q-space />
      <canvas ref="spectrogram"/>
      <q-space />
    </q-toolbar>

  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';


export default defineComponent({
  name: 'List',

  components: {

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
  },

  methods: {
    setActionStatus(loading, error) {
      this.indexActionLoading = loading;
      this.indexActionError = error;
    },
    async onPrev() {
      try {
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
      await this.runWorkingEntry_stage_2a();
      await this.runWorkingEntry_stage_2b();
      await this.runWorkingEntry_stage_3();
    },
    async runWorkingEntry_stage_1() {
      if(this.workingEntry !== undefined) {
        try {
          this.setActionStatus(true, undefined);
          var urlPath = 'samples2/' + this.workingEntry.sample;
          var params = {sample_rate: true,};
          var response = await this.$api.get(urlPath, {params});
          this.sample = response.data.sample;
          this.setActionStatus(false, undefined);
        } catch(e) {
          this.setActionStatus(false, e.response && e.response.data ? (e.response.data.error ? e.response.data.error : e.response.data) : 'error');
          //console.log(e);
        }
      } else {
        this.sample = undefined;
      }
    },
    async runWorkingEntry_stage_2a() {
      if(this.sample !== undefined) {
        var apiUrl = this.$store.getters.api('samples2', this.sample.id, 'audio');
        var url = new URL(apiUrl);
        if(this.audio !== undefined) {
          this.audio.pause();
          this.audio.ontimeupdate = undefined;
          this.audio.oncanplay = undefined;
          this.audio.src = undefined;
          this.audio = undefined;
        }
        console.log(url.href);
        this.audio = new Audio(url.href);
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
    },
    async runWorkingEntry_stage_2b() {
      if(this.sample !== undefined) {
        console.log('runWorkingEntry_stage_2b');
        var start_sample = this.workingEntry.start * this.sample.sample_rate;
        var end_sample = this.workingEntry.end * this.sample.sample_rate;
        var apiUrl = this.$store.getters.api('samples2', this.sample.id, 'spectrogram');
        var url = new URL(apiUrl);
        url.searchParams.append('start_sample', start_sample);
        url.searchParams.append('end_sample', end_sample);
        url.searchParams.append('cutoff_lower', this.player_fft_cutoff_lower);
        url.searchParams.append('cutoff', this.player_fft_cutoff);
        url.searchParams.append('step', this.player_fft_step);
        url.searchParams.append('window', this.player_fft_window);
        url.searchParams.append('threshold', this.player_spectrum_threshold);
        url.searchParams.append('intensity_max', this.player_fft_intensity_max);
        if(this.player_spectrum_shrink_Factor !== undefined && this.player_spectrum_shrink_Factor > 1) {        
          url.searchParams.append('shrink_factor', this.player_spectrum_shrink_Factor);
        }
        console.log(url.href);
        var image = new Image();
        image.src = url.href;
        await image.decode();
        this.imageBitmap = await createImageBitmap(image);
        console.log('runWorkingEntry_stage_2b   end');
      } else {
        this.imageBitmap = undefined;
      }
    },
    runWorkingEntry_stage_3() {
      console.log('runWorkingEntry_stage_3');
      this.repaint(); 
      console.log('runWorkingEntry_stage_3 end');
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
      this.audio.currentTime = this.workingEntry.start;
      this.audio.oncanplay = (event) => { 
        this.audio.play();
      };
      this.audio.play();
    },
    stop() {
      this.audio.pause();
      this.audio.oncanplay = undefined;
      this.audio.currentTime = this.workingEntry.start;
    },
  },

  watch: {
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

</style>
