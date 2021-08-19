<template>
  
<q-dialog 
  v-model="shown" 
  persistent
  :full-width="maximized"
  :full-height="maximized"
>
  <q-layout view="Lhh lpR fff" container class="bg-white text-black">
    <q-header class="bg-grey-2 text-black">
      <q-toolbar>
        <q-icon name="note_add" style="font-size: 2em;" class="text-grey-7"/>
        <q-toolbar-title>
          Create review list
        </q-toolbar-title>
        <q-space />

        <q-btn v-if="maximized" dense flat icon="content_copy" @click="maximized = false">
          <q-tooltip>Shrink</q-tooltip>
        </q-btn>
        
        <q-btn v-if="!maximized" dense flat icon="crop_square" @click="maximized = true">
          <q-tooltip>Maximize</q-tooltip>
        </q-btn>          

        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip>Close</q-tooltip>
        </q-btn>
      </q-toolbar>
      <q-separator />
    </q-header>

    <q-footer class="bg-white text-black">
      <q-separator />
      <q-toolbar inset>        
        <q-space />
        <q-btn flat label="Create" class="text-teal" @click="onSubmitCreateReviewList" />
        <q-btn flat label="Cancel" v-close-popup />
      </q-toolbar>
    </q-footer>

    <q-page-container>
      <q-page padding>
        TODO
      </q-page>
    </q-page-container>
  </q-layout>
</q-dialog>


  
  
  
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

export default {
  name: 'create-review-list-dialog',

  data: () => ({
    shown: false,
    maximized: true,
  }),  

  computed: {
    ...mapState({
      project: state => state.project,
    }),
    ...mapGetters({
      apiPOST: 'apiPOST',      
    }),    
  },

  methods: {
    ...mapActions({
    }),
    show() {
      this.shown = true;
    }, 
    async onSubmitCreateReviewList() {
      var action = {action: "create_review_list"};
      var content = {actions: [action]}; 
      let params = {project: this.project};
      try {
        var response = await this.apiPOST(['photodb2', 'review_lists'], content, {params});
        this.shown = false;
      } catch {
        console.log("error");
      }
    },             
  },  

  async mounted() {
  },
}
</script>

<style scoped>


</style>
