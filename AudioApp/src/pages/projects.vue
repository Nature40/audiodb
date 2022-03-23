<template>
  <q-page class="flex justify-center" style="color: black;">
    <div>
    <b>Select one project:</b>
    <br>
    <div v-if="projects === undefined">
      no projects loaded
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
    }),
  },

  methods: {
    toProjectHash(project) {
      return '/projects/' + project.id + '/main';
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
