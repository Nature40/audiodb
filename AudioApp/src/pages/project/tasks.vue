<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="refresh" icon="refresh" title="Refresh task status.">
        
      </q-btn>
    </q-toolbar>
    <div>
      <table>
        <thead>
          <tr>
            <th>Id</th>
            <th>Identity</th>
            <th>Task</th>
            <th>State</th>
            <th>Start</th>
            <th>Runtime</th>
            <th>Message</th>  
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.id" @click="view(task.id);">
            <td>{{task.id}}</td>
            <td>{{task.identity}}</td>
            <td>{{task.task}}</td>
            <td>{{task.state}}</td>
            <td>{{task.start}}</td>
            <td>{{task.runtime}}</td>
            <td>{{task.message}}</td>
          </tr>
        </tbody>
      </table>
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
      tasks: [],
    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,
    }),
  },

  methods: {
    async refresh() {
      this.tasks = [];
      try {
        var urlPath = 'tasks';
        var response = await this.$api.get(urlPath, {params: {tasks: true,}});
        this.tasks = response.data.tasks;       
      } catch(e) {
        this.tasks = [];
        console.log(e);
      }
    },
    view(id) {
      this.$refs.TaskConsole.view(id);
    },
  },

  watch: {
    
  },
  
  async mounted() {
    this.refresh();
  },  
})
</script>

<style scoped>

td:nth-child(1) {
  font-style: italic;
  color: grey;
  font-size: 0.75em;
}

td:nth-child(2) {
  font-style: italic;
  color: grey;
}

td:nth-child(3) {
  color: black;
}

td:nth-child(4) {
  color: black;
  font-weight: bold;
}

td:nth-child(5) {
  color: grey;
}

td:nth-child(6) {
  color: blue;
}

tbody tr:nth-child(even) {
  background-color: #00000007;
}

tbody tr:hover {
  background-color: #ffff9944;
  cursor: pointer;
}

thead {
  background-color: #0000002e;
}

</style>
