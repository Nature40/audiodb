<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="refresh">
        refresh task status
      </q-btn>
    </q-toolbar>
    <div>
      <div v-for="task in tasks" :key="task.id">{{task}}</div>
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
    }
  },

  watch: {
    
  },
  async mounted() {

   },  
})
</script>

<style scoped>


</style>
