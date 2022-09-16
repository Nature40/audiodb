<template>
  
<q-dialog 
  v-model="shown" 
  persistent
  :full-width="maximized"
  :full-height="maximized"
>
  <q-layout view="Lhh lpR fff" container class="bg-white text-black" style="min-width: 600px;">
    <q-header class="bg-grey-2 text-black">
      <q-toolbar>
        <q-icon name="edit_note" style="font-size: 2em;" class="text-grey-7"/>
        <q-toolbar-title>
          Manage review list sets
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
        <q-btn flat label="Close" v-close-popup :disabled="loading" />
      </q-toolbar>
    </q-footer>

    <q-page-container>
      <q-page padding>
        <q-btn push icon="playlist_add" @click="$refs.createReviewListDialog.show()">Add new review list set</q-btn>
        <create-review-list-dialog ref="createReviewListDialog" @closed="$emit('refresh')"/>
        <hr style="margin-bottom: 20px;">
        <b>Review list sets:</b>
        <br><br>
        <q-table
          v-if="review_list_sets !== undefined && review_list_sets.length > 0"
          class="my-sticky-header-column-table"
          :data="review_list_sets"
          :columns="columns"
          row-key="id"
          rows-per-page="0"
          hide-bottom
          selection="multiple"
          :selected.sync="selected"
          dense
        >
        </q-table>
        <div v-else>
          <b>No review list sets available. You may create new review list sets!</b>
        </div>
        
        <hr style="margin-bottom: 20px;">
        <b>Actions for selected sets:</b>
        <br>
        <q-btn 
          push 
          icon="refresh" 
          @click="onRefreshSets" 
          :disabled="selectedEmpty" 
          text-color="green" 
          style="margin-top: 20px;" 
          title="Refresh creation of image sets with current images and current meta data."
        >
          regenerate lists of selected sets
        </q-btn>
        <br>
        <q-btn 
          push 
          icon="delete_forever" 
          @click="onRemove" 
          :disabled="selectedEmpty" 
          text-color="red" 
          style="margin-top: 20px;"
          title="Delete image sets. Contained images are not deleted."
        >
          remove selected sets
        </q-btn>
      </q-page>
    </q-page-container>
  </q-layout>
</q-dialog>  
  
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'
import createReviewListDialog from '../components/create-review-list-dialog'

export default {
  name: 'manage-review-list-sets-dialog',

  components: {
    createReviewListDialog,
  },

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
    columns: [
      {label: 'Project', field: 'project', name: 'project', align: 'center', sortable: true,},
      {label: 'Name', field: 'name', name: 'name', align: 'center', sortable: true,},
    ],
    selected: [],    
  }),  

  computed: {
    ...mapState({
      project: state => state.project,
      review_list_sets: state => state.meta?.data?.review_list_sets,
    }),
    ...mapGetters({
      apiPOST: 'apiPOST',      
    }),
    selectedEmpty() {
      return this.selected === undefined || this.selected.length === 0;
    },    
  },

  watch: {
    shown() {
      if(!this.shown) {
        this.$emit('closed');
      }
    },
    review_list_sets() {
      this.selected = [];
    },
  },

  methods: {
    ...mapActions({
    }),
    show() {
      this.selected = [];
      this.shown = true;
    },
    async onRefreshSets() {
      var action = {action: "refresh_review_list_set"};
      action.sets = this.selected.map(set => set.id);
      var content = {actions: [action]}; 
      let params = {project: this.project};     
      try {
        this.loading = "Processing";
        this.loadingError = undefined;
        var response = await this.apiPOST(['photodb2', 'review_lists'], content, {params});
        this.loading = undefined;
        this.loadingError = undefined;
      } catch {
        this.loading = undefined;
        this.loadingError = 'Proccessing error';
        console.log("error");
      } finally {
        this.selected = [];
        this.$emit('refresh');
      }
    },     
    async onRemove() {
      var action = {action: "remove_review_list_set"};
      action.sets = this.selected.map(set => set.id);
      var content = {actions: [action]}; 
      let params = {project: this.project};     
      try {
        this.loading = "Processing";
        this.loadingError = undefined;
        var response = await this.apiPOST(['photodb2', 'review_lists'], content, {params});
        this.loading = undefined;
        this.loadingError = undefined;
      } catch {
        this.loading = undefined;
        this.loadingError = 'Proccessing error';
        console.log("error");
      } finally {
        this.selected = [];
        this.$emit('refresh');
      }
    },          

  },  

  async mounted() {
  },
}
</script>

<style scoped>


</style>
