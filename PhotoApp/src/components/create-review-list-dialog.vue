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
          Create review lists
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
        <span v-if="loading"><q-spinner-gears color="primary" size="2em"/> {{loading}}</span>  
        <span v-if="loadingError"> {{loadingError}}</span>       
        <q-space />
        <q-btn flat label="Create" class="text-teal" @click="onSubmitCreateReviewList" :disabled="loading" />
        <q-btn flat label="Cancel" v-close-popup :disabled="loading" />
      </q-toolbar>
    </q-footer>

    <q-page-container>
      <q-page padding>
        Prefilter
        <span class="row">
        <q-input outlined v-model="prefilter_classificator" label="Prefilter classificator" stack-label dense />
        <q-input outlined v-model="prefilter_threshold" label="Prefilter confidence threshold" stack-label dense type="number" />
        </span>
        <hr>
        Classification
        <span class="row">
        <q-input outlined v-model="classification_classificator" label="Classification classificator" stack-label dense />
        <q-input outlined v-model="classification_threshold" label="Classification confidence threshold" stack-label dense  type="number" />
        </span>
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
    prefilter_classificator: 'MegaDetector',
    prefilter_threshold: 0.8,
    classification_classificator: 'EfficientNetB3',
    classification_threshold: 0.8,
    loading: undefined,
    loadingError: undefined,
  }),  

  computed: {
    ...mapState({
      project: state => state.project,
    }),
    ...mapGetters({
      apiPOST: 'apiPOST',      
    }),    
  },

  watch: {
    shown() {
      if(!this.shown) {
        this.$emit('closed');
      }
    },
  },

  methods: {
    ...mapActions({
    }),
    show() {
      this.shown = true;
    }, 
    async onSubmitCreateReviewList() {
      var action = {action: "create_review_list"};
      action.prefilter_classificator = this.prefilter_classificator;
      action.prefilter_threshold = this.prefilter_threshold;
      action.classification_classificator = this.classification_classificator;
      action.classification_threshold = this.classification_threshold;
      var content = {actions: [action]}; 
      let params = {project: this.project};     
      try {
        this.loading = "Processing";
        this.loadingError = undefined;
        var response = await this.apiPOST(['photodb2', 'review_lists'], content, {params});
        this.loading = undefined;
        this.loadingError = undefined;
        this.shown = false;
      } catch {
        this.loading = undefined;
        this.loadingError = 'Proccessing error';
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
