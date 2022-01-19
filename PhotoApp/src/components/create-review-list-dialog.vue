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
        <q-icon name="create_new_folder" style="font-size: 2em;" class="text-grey-7"/>
        <q-toolbar-title>
          Create review list set
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
        <q-input v-model="set_name" label="Review list set name" placeholder="automatic generated set name" stack-label dense bottom-slots>
          <template v-slot:hint>
            (For existing set name, old set will be overwritten.)
        </template>
        </q-input>
        <hr>
        <b>Prefilter</b>
        <span class="row">
        <q-input outlined v-model="prefilter_classificator" label="Prefilter classificator" stack-label dense />
        <q-input outlined v-model="prefilter_threshold" label="Prefilter confidence threshold" stack-label dense type="number" />
        </span>
        <hr>
        <b>Classification</b>
        <span class="row">
        <q-input outlined v-model="classification_classificator" label="Classification classificator" stack-label dense />
        <q-input outlined v-model="classification_threshold" label="Classification confidence threshold" stack-label dense  type="number" />
        </span>
        <hr>
        <b>Options</b>
        <br><q-checkbox v-model="sorted_by_ranking" label="Sort by ranking" />
        <br><q-checkbox v-model="categorize_classification_location" label="Catgerize by classification and location" />
        <hr>
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
    maximized: false,
    set_name: undefined,
    prefilter_classificator: 'MegaDetector',
    prefilter_threshold: 0.8,
    classification_classificator: 'EfficientNetB3',
    classification_threshold: 0.8,
    sorted_by_ranking: true,
    categorize_classification_location: false,
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
      if(this.set_name !== undefined && this.set_name !== null && this.set_name.length > 0) {
        action.set_name = this.set_name;
      }
      action.prefilter_classificator = this.prefilter_classificator;
      action.prefilter_threshold = this.prefilter_threshold;
      action.classification_classificator = this.classification_classificator;
      action.classification_threshold = this.classification_threshold;
      action.sorted_by_ranking = this.sorted_by_ranking;
      action.categorize_classification_location = this.categorize_classification_location;
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
