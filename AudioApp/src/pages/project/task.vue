<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-select rounded outlined v-model="descriptorName" :options="descriptorNames" label="Selected task" style="min-width: 200px;"/>
    </q-toolbar>
    <div v-if="descriptor !== undefined">
      <q-btn @click="execute">
        Execute task {{descriptor.name}}
      </q-btn>
    </div>


  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'Main',

  components: {

  },

  data() {
    return {
      descriptors: {},
      descriptorName: undefined,
    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,
    }),
    descriptorNames() {
      return Object.keys(this.descriptors);
    },
    descriptor() {
      if(this.descriptorName === undefined || this.descriptorName === null) {
        return undefined;
      }
      return this.descriptors[this.descriptorName];
    }
  },

  methods: {
    async refresh() {
      try {
        var urlPath = 'tasks';
        var response = await this.$api.get(urlPath, {params: {descriptors: true,}});
        this.descriptors = response.data.descriptors;       
      } catch(e) {
        this.descriptors = {};
        console.log(e);
      }
    },
    async execute() {
      try {
        var urlPath = 'tasks';
        var data = {action: {action: 'submit', task: { task: this.descriptor.name}}};
        var response = await this.$api.post(urlPath, data);       
      } catch(e) {
        console.log(e);
      }
    },
  },

  watch: {
    descriptors() {
      if(this.descriptors.length == 0) {
        return;
      }
      if(this.descriptor === undefined) {
        this.descriptorName = this.descriptorNames[0];
      }
    },
  },
  async mounted() {
    this.refresh();
   },  
})
</script>

<style scoped>


</style>
