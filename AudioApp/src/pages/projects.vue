<template>
  <q-page class="flex justify-center" style="color: black;">
    <div>
    <b>Select one project:</b>
    <br>
    <div v-if="projectsLoading" style="color: blue;">
      Loading projects...
    </div>
    <div v-if="projects === undefined">
      no projects loaded
      <div v-if="projectsError !== undefined" style="color: red;">
      error loading projects
      <q-btn @click="refresh" v-if="!projectsLoading">try again</q-btn>
      </div>
    </div>
    <div v-else-if="projects.length === 0">
      no projects found
    </div>
    <div v-else>
      <q-list bordered separator>
        <q-item clickable v-ripple v-for="project in projects" :key="project.id" :to="toProjectHash(project)">
          <q-item-section avatar><q-icon name="arrow_right_alt" /></q-item-section>
          <q-item-section>{{project.id}}</q-item-section>
        </q-item>
      </q-list>
    </div>
    </div>
    
  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'Projects',

  computed: {
    ...mapState({
      projects: state => state.projects.data?.projects,
      projectsLoading: state => state.projects.loading,
      projectsError: state => state.projects.error,
    }),
  },

  methods: {
    toProjectHash(project) {
      return '/projects/' + project.id + '/main';
    },
    refresh() {
      this.$store.dispatch('projects/refresh'); 
    },
  },
  
  watch: {
    projects() {
      console.log("watch");
      if(this.projects !== undefined && this.projects.length === 1) {
        console.log("watch!");
        var hash = this.toProjectHash(this.projects[0]);
        this.$router.push(hash);
      }
    },
  },
  
  async mounted() {
    this.$store.dispatch('projects/init');    
  },
})
</script>
