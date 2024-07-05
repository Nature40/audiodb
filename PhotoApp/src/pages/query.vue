<template>
  <q-page class="fit column items-center">

    <div v-if="projects === undefined">
      loading projects
    </div>
    <q-select 
      v-else 
      rounded 
      outlined 
      bottom-slots 
      v-model="selectedProject" 
      :options="projects" 
      label="Project" 
      dense 
      options-dense 
      options-selected-class="text-deep-blue" 
      style="min-width: 200px;"
      title="Select one project of images."
    >
      <template v-slot:prepend>
        <q-icon name="menu_book" />
      </template>
      <template v-slot:option="scope">
        <q-item v-bind="scope.itemProps" v-on="scope.itemEvents">
          {{scope.opt}}
        </q-item>
      </template>
      <template v-slot:selected-item="scope">
        {{scope.opt}}
      </template>
    </q-select>
    <div v-if="project === undefined">
      no project selected
    </div>
    <div v-else-if="meta === undefined || meta.project !== project">
      loading project...
    </div>
    <div class="column items-center">

      <q-btn-toggle
        v-model="selectedQueryMode"
        push
        glossy
        toggle-color="primary"
        :options="[
          {label: 'Query', value: 'query', icon: 'manage_search'},
          {label: 'Review List', value: 'review_list', icon: 'assignment'},
        ]"
        title="Select working images by subset query or by image lists."
      />
    </div>
    
    <hr style="min-width: 500px;">
    
    <div class="column items-center" v-if="selectedQueryMode === 'query'">
      <q-select 
        rounded 
        outlined 
        bottom-slots 
        v-model="selectedLocation" 
        :options="locations" 
        label="Location" 
        dense 
        options-dense 
        options-selected-class="text-deep-blue" 
        style="min-width: 200px;"
        title="Select one location of images."
      >
        <template v-slot:prepend>
          <q-icon name="location_on" />
        </template>
        <template v-slot:option="scope">
          <q-item v-bind="scope.itemProps" v-on="scope.itemEvents">
            {{scope.opt}}
          </q-item>
        </template>
        <template v-slot:selected-item="scope">
          {{scope.opt}}
        </template>
      </q-select>
      <div v-if="selectedLocation === ''">
        No location selected. 
        <br><q-icon name="info"/> Select a location!
      </div>
    </div>
    
    <div class="column items-center" v-if="selectedQueryMode === 'review_list'">
      <div class="row">
      <q-select 
        rounded 
        outlined 
        bottom-slots 
        v-model="selectedReviewListSet" 
        :options="review_list_sets" 
        option-label="name" 
        label="Review list set" 
        dense 
        options-dense 
        options-selected-class="text-deep-blue" 
        style="min-width: 200px;"
        title="Select one set of image lists."
      >
        <template v-slot:prepend>
          <q-icon name="folder" />
        </template>
      </q-select>      
      <q-select 
        rounded 
        outlined 
        bottom-slots 
        v-model="selectedReviewList" 
        :options="review_lists" 
        label="Review list" 
        dense 
        options-dense 
        options-selected-class="text-deep-blue" 
        style="min-width: 400px;"
        title="Select one image list."
      >
        <template v-slot:prepend>
          <q-icon name="playlist_play" />
        </template>
        <template v-slot:option="scope">
          <q-item v-bind="scope.itemProps" v-on="scope.itemEvents">
            {{scope.opt.name}}  <span style="color: grey; padding-left: 10px;">{{scope.opt.count}}</span>
          </q-item>
        </template>
        <template v-slot:selected-item="scope">
          {{scope.opt.name}}
        </template>        
        <template v-slot:after>
          <q-btn push color="grey-7" round icon="edit_note" @click="$refs.manageReviewListSetsDialog.show()" title="Manage image list sets."/>
          <manage-review-list-sets-dialog ref="manageReviewListSetsDialog" @closed="refreshProjectMeta" @refresh="refreshProjectMeta"/>          
        </template>
      </q-select>
      </div>
      <div v-if="review_lists === undefined || review_lists === null || review_lists.length === 0">
        No review_list found.
      </div>
    </div>    

    <hr style="min-width: 500px;">

    <div class="column items-center">
      {{photos.length}} photos selected.
    </div>    

  </q-page>
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

import manageReviewListSetsDialog from '../components/manage-review-list-sets-dialog'

const entryCompareFn = function(a, b) {
  const nameA = a.name.toUpperCase();
  const nameB = b.name.toUpperCase();
  if (nameA < nameB) {
    return -1;
  }
  if (nameA > nameB) {
    return 1;
  }
  return 0;
}

export default {
  name: 'query',

  components: {
    manageReviewListSetsDialog,
  },  

  data: () => ({
    photosMessage: 'init',
    selectedProject: undefined,
    selectedLocation: '',
    selectedQueryMode: 'query',
    selectedReviewListSet: undefined,
    selectedReviewList: undefined,
  }),  

  computed: {
    ...mapState({
      project: state => state.project,
      photos: state => state.photos.data,
      photoIndex: state => state.photo.index,
      projects: state => state.projects?.data?.projects,
      meta: state => state.meta?.data,
      locations: state => state.meta?.data?.locations,
      review_lists_unfiltered: state => state.meta?.data?.review_lists,
      review_list_sets: state => state.meta?.data?.review_list_sets,
    }),    
    ...mapGetters({
      api: 'api',
      apiGET: 'apiGET',
    }),
    review_lists() {
      if(this.review_lists_unfiltered === undefined) {
        return [];
      }
      if(this.selectedReviewListSet === undefined) {
        return this.review_lists_unfiltered.slice().sort(entryCompareFn);
      }
      return this.review_lists_unfiltered.filter(entry => entry.set === this.selectedReviewListSet.id).sort(entryCompareFn);
    },
  },

  watch: {
    projects() {
      console.log(this.projects);
      if(this.projects === undefined || this.projects.length === 0) {
        this.selectedProject = undefined; 
      } else {
        if(this.selectedProject === undefined) {
          this.selectedProject = this.projects[0];
        }
      }
    },
    async selectedProject() {
      this.refreshProjectMeta();
    },
    locations() {
      if(this.locations === undefined || this.locations.length === 0) {
        this.selectedLocation = '';
      } else {
        this.selectedLocation = this.locations[0];
      }
    },
    selectedLocation() {
      this.$nextTick(() => this.sendQuery());
    },
    selectedReviewList() {
      this.$nextTick(() => this.sendQuery());
    },
    selectedQueryMode() {
      this.$nextTick(() => this.sendQuery());
    },
    review_lists() {
      if(this.review_lists === undefined || this.review_lists.length === 0) {
        this.selectedReviewList = undefined;
      } else {
        this.selectedReviewList = this.review_lists[0];
      }
    },
    review_list_sets() {
      if(this.review_list_sets === undefined || this.review_list_sets.length === 0) {
        this.selectedReviewListSet = undefined;
      } else {
        this.selectedReviewListSet = this.review_list_sets[0];
      }
    },         
  },

  methods: {
    ...mapActions({
      metaQuery: 'meta/query',
      photosQuery: 'photos/query',
      photoSetIndex: 'photo/setIndex',
    }),
    setIndex(index) {
      this.photoSetIndex(index);
      this.$router.push('/viewer');
    },
    sendQuery() {
      if(this.selectedQueryMode === 'query') {
        if(this.selectedProject !== undefined && this.selectedLocation !== undefined && this.selectedLocation !== null && this.selectedLocation !== '') {
          this.photosQuery({project: this.selectedProject, location: this.selectedLocation});
        } else {
          this.photosQuery();
        }
      } else if(this.selectedQueryMode === 'review_list') {
        if(this.selectedProject !== undefined && this.selectedReviewList !== undefined && this.selectedReviewList !== null) {
          this.photosQuery({project: this.selectedProject, review_list: this.selectedReviewList.id});
        } else {
          this.photosQuery();
        }
      }
    },
    async refreshProjectMeta() {
      await this.$store.dispatch('setProject', this.selectedProject);
      this.$nextTick(() => this.sendQuery());
    },    
  },

  async mounted() {
    this.$store.dispatch('projects/init');    
  },
}
</script>

<style scoped>
.selected {
  color: red;
}
</style>
