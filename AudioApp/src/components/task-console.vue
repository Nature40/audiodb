<template>
  <q-dialog v-model="show" full-width>
      <q-card v-if="show">
        <q-bar>
          <q-icon name="image"/>
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
        <div>
          {{task}}
        </div>
        <div>
          Id: {{id}}
        </div>
        <div>
          Runtime: {{runtime}}
        </div>
        <div>
          State: {{state}}
        </div>
        <div>
          Message: {{message}}
        </div>
        <hr>
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
      runtime: undefined,
      state: undefined,
      message: undefined,
      name: undefined,
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
        this.runtime = data.runtime;
        this.state = data.state;
        this.message = data.message;
        this.name = data.name;
        this.log = data.log;
        if(this.show) {
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
