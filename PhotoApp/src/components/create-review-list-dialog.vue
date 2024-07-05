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
        <q-icon name="playlist_add" style="font-size: 2em;" class="text-grey-7"/>
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
        <q-btn flat label="Create" class="text-teal" @click="onSubmitCreateReviewList" :disabled="loading" title="Create your specified image list set. This may take some time."/>
        <q-btn flat label="Cancel" v-close-popup :disabled="loading" title="Discard creating image list set."/>
      </q-toolbar>
    </q-footer>

    <q-page-container>
      <q-page padding>
        <b>Name</b>
        <q-input v-model="set_name" label="Review list set name" placeholder="automatic generated set name" stack-label dense bottom-slots>
          <template v-slot:hint>
            (For existing set name, old set will be overwritten.)
          </template>
        </q-input>
        <hr style="margin-bottom: 20px;">
        <b>Prefilter</b>
        <span class="row">
        <q-input outlined v-model="prefilter_classificator" label="Prefilter classificator" stack-label dense title="Name of classificator."/>
        <q-input outlined v-model="prefilter_threshold" label="Prefilter confidence threshold" stack-label dense type="number" v-if="prefilter_classificator" min="0" max="1" step=".1" title="Discard confidence values lower than this number between 0 and 1."/>
        <span v-if="prefilter_classificator && !prefilter_threshold">(no threshold)</span>
        <span v-if="!prefilter_classificator">(not applied)</span>
        </span>
        <hr style="margin-bottom: 20px;">
        <b>Classification filter</b>
        <span class="row">
        <q-input outlined v-model="classification_classificator" label="Classification classificator" stack-label dense title="Name of classificator."/>
        <q-input outlined v-model="classification_threshold" label="Classification confidence threshold" stack-label dense  type="number"  v-if="classification_classificator" min="0" max="1" step=".1" title="Discard confidence values lower than this number between 0 and 1."/>
        <span v-if="classification_classificator && !classification_threshold">(no threshold)</span>
        <span v-if="!classification_classificator">(not applied)</span>
        </span>
        <hr style="margin-bottom: 20px;">
        <b>Groups</b>
        <br><q-select label="Group into review lists by" v-model="groupByOption" :options="groupByOptions" outlined stack-label dense options-dense title="The Classification of selected classification filter is used." />        
        <hr style="margin-bottom: 20px;">
        <b>Options</b>
        <!--<br><q-checkbox v-model="omit_expert_classified" label="Omit expert classified detections" dense title="All detections that are already labeled by expert are not included in resulting lists." />-->
        <br><q-checkbox v-model="omit_expert_classified" label="Unreviewed images only (Omit images with expert classifications.)" dense title="Images with at least one expert classification are not included in resulting lists. With this option, images that are not already reviewed by experts are listed only." />

        <br><q-checkbox v-model="sorted_by_ranking" label="Sort by classification confidence" dense title="The Classification of selected classification filter is used." style="padding-top: 10px;"/>
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
    loading: undefined,
    loadingError: undefined,
    groupByOptions: [
      {label: 'classification', value: 'classification'},
      {label: 'location', value: 'location'},
      {label: 'classification and location', value: 'classification_location'},
      {label: 'location and classification', value: 'location_classification'},
    ],
    groupByOption: undefined,
    omit_expert_classified: false,    
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
      var action = {action: "create_review_list_set"};
      if(this.set_name !== undefined && this.set_name !== null && this.set_name.length > 0) {
        action.set_name = this.set_name;
      }
      if(this.prefilter_classificator) {
        action.prefilter_classificator = this.prefilter_classificator;
        if(this.prefilter_threshold) {
          action.prefilter_threshold = this.prefilter_threshold;
        }
      }
      if(this.classification_classificator) {
        action.classification_classificator = this.classification_classificator;
        if(this.classification_threshold) {
          action.classification_threshold = this.classification_threshold;
        }
      }
      if(this.groupByOption) {
        action.group_by = this.groupByOption.value;
      }
      action.sorted_by_ranking = this.sorted_by_ranking;
      action.omit_expert_classified = this.omit_expert_classified;
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
    this.groupByOption = this.groupByOptions[0];
  },
}
</script>

<style scoped>


</style>
