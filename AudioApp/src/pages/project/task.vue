<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-select rounded outlined v-model="descriptorName" :options="descriptorNames" label="Selected task" style="min-width: 200px;">
        <template v-slot:option="scope">
          <q-item v-bind="scope.itemProps">
            <q-item-section>
              <q-item-label><b>{{scope.opt}}</b> <span style="color: grey; padding-left: 20px;">{{descriptors[scope.opt].description}}</span></q-item-label>
            </q-item-section>
          </q-item>
        </template>
      </q-select>
    </q-toolbar>
    <div v-if="descriptor !== undefined">
      <div v-if="descriptor.description !== undefined">
        {{descriptor.description}}
      </div>
      <q-btn @click="execute" icon="keyboard_return" style="margin-left: 200px; margin-top: 50px;">
        (Execute task) {{descriptor.name}}
      </q-btn>
    </div>

    <task-console ref="TaskConsole"/>

  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';

import TaskConsole from 'components/task-console';

export default defineComponent({
  name: 'Main',

  components: {
    TaskConsole,
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
    execute() {
      var task = { task: this.descriptor.name};
      this.$refs.TaskConsole.submit(task);
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
