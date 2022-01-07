<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="task_refresh_label">
        refresh_label
      </q-btn>
    </q-toolbar>


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

    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,
    }),
  },

  methods: {
    async task_refresh_label() {
      try {
        var urlPath = 'tasks';
        var data = {action: {action: 'submit', task: { task: 'refresh_label'}}};
        var response = await this.$api.post(urlPath, data);       
      } catch(e) {
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
