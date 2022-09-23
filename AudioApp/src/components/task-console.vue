<template>
  <q-dialog v-model="show" full-width>
      <q-card v-if="show">
        <q-bar>
          <q-icon name="event_note"/>
          <div>Task console</div>
          <div v-if="submitting" class="text-primary">
            <q-spinner color="primary" />
            Submitting ...
          </div>
          <div v-if="submittingError" class="text-red">
            Error at submitting or network connection.
            <q-btn dense icon="refresh" @click="submit(task);">
            <q-tooltip>Resubmit</q-tooltip>            
            </q-btn>
          </div>
          <div v-if="name" class="text-grey-3">
            {{name}}
          </div>          
          <q-space />
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>
        <q-separator />
        <div style="font-size: 0.75em; font-style: italic; color: grey;">
          {{task}}
        </div>
        <div style="font-size: 0.75em;">
          Id: <span style="font-style: italic; color: grey;">{{id}}</span>
        </div>
        <div>
          Identity: <span style="font-style: italic; color: grey;">{{identity}}</span>
        </div>        
        <div>
          Start: <span style="color: grey;">{{start}}</span> 
        </div>
        <div>
          Runtime: <span style="color: blue;">{{runtime}}</span>
        </div>
        <div>
          State: <span style="color: black; font-weight: bold;">{{state}}</span> <q-btn v-if="cancelable && state === 'RUNNING'" @click="onCancel" icon="clear" dense>Cancel</q-btn>
        </div>
        <div>
          Message: {{message}}
        </div>
        <hr>
        <div v-if="results != undefined">
          <b>Results:</b>
          <div v-for="(result, i) in results" :key="i">
            <span v-if="result.type === 'text'">{{result.text}}</span>
            <span v-else-if="result.type === 'file'"><a :href="$store.getters['api']('tasks' + '/' + id + '/' + 'files' + '/' + i + '/' + result.filename)" target="_blank">{{result.filename}}</a></span>
            <span v-else>Unknown type: {{result.type}}</span>
          </div>
          <hr>
        </div>
        <div  class="text-grey-6">
        <div v-for="(s, i) in log" :key="i">{{s}}</div>
        </div>
      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'task-console',
  setup () {
    const show = ref(false);
    return {
      show,
    };
  },
  data() {
    return {  
      submitting: false, 
      submittingError: false, 
      task: undefined,
      id: undefined,
      identity: undefined,
      start: undefined,
      runtime: undefined,
      state: undefined,
      message: undefined,
      results: undefined,
      name: undefined,
      cancelable: false,
      log: undefined,
    };
  },
  computed: {
    ...mapState({
    
    }),

  },
  methods: {
    async refresh(id) {
      if(this.id && (id === undefined || id === this.id)) {
        var urlPath = 'tasks/' + this.id;
        var response = await this.$api.get(urlPath);
        const data = response.data;
        this.identity = data.identity;
        this.start = data.start;
        this.runtime = data.runtime;
        this.state = data.state;
        this.message = data.message;
        this.results = data.results;
        this.name = data.name;
        this.log = data.log;
        this.cancelable = data.cancelable;
        if(this.show && this.state !== 'ERROR' && this.state !== 'DONE') {
          setTimeout( () => this.refresh(this.id), 250); 
        }
      }
    },
    async submit(task) {
      this.task = task;
      this.id = undefined;
      this.state = undefined;
      this.message = undefined;
      this.name = undefined;
      this.show = true;
      try {
        this.submitting = true;
        this.submittingError = false;
        var urlPath = 'tasks';
        var data = {action: {action: 'submit', task: task}};
        var response = await this.$api.post(urlPath, data);
        this.id = response.data.result.id;
        this.submitting = false;
        this.submittingError = false;
        this.refresh(); 
      } catch(e) {
        console.log(e);
        this.submitting = false;
        this.submittingError = true;
      }      
    },
    view(id) {
      this.show = true;
      this.id = id;
      this.refresh(id);
    },
    async onCancel() {
      try {
        var urlPath = 'tasks/' + this.id;
        var data = {action: {action: 'cancel'}};
        var response = await this.$api.post(urlPath, data);
      } catch(e) {
        console.log(e);
      }
    },
  },
  watch: {
    show() {
      if(this.show) {
        this.refresh();
      }
    },
  },
  async mounted() {
  },
});
</script>

<style scoped>

</style>
