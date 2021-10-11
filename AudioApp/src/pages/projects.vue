<template>
  <q-page class="flex flex-center">
    Select Project:
    <div v-if="projects === undefined">
      no projects loaded
    </div>
    <div v-else-if="projects.length === 0">
      no projects found
    </div>
    <div v-else>
      <div v-for="project in projects" :key="project.id">
        <br><router-link :to="toProjectHash(project)">{{project.id}}</router-link>
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
    }),
  },

  methods: {
    toProjectHash(project) {
      return '/projects/' + project.id;
    }
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
