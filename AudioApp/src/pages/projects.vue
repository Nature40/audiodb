<template>
  <q-page class="flex flex-center">
    Projects
    <a href="#/projects/one">one</a>
  </q-page>
</template>

<script>
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'Projects',
  
  async mounted() {
    this.$store.dispatch('projects/init');    
  },
})
</script>
