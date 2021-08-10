<template>
  <q-page class="fit column items-center">

    <div v-if="projects === undefined">
      loading projects
    </div>
    <q-select v-else rounded outlined bottom-slots v-model="selectedProject" :options="projects" label="Project" dense options-dense options-selected-class="text-deep-blue" style="min-width: 200px;">
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
      />
    </div>
    
    <hr style="min-width: 500px;">
    
    <div class="column items-center" v-if="selectedQueryMode === 'query'">
      <q-select rounded outlined bottom-slots v-model="selectedLocation" :options="locations" label="Location" dense options-dense options-selected-class="text-deep-blue" style="min-width: 200px;">
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
      <q-select rounded outlined bottom-slots v-model="selectedReviewList" :options="review_lists" option-label="name" label="Review List" dense options-dense options-selected-class="text-deep-blue" style="min-width: 200px;">
        <template v-slot:prepend>
          <q-icon name="rule" />
        </template>
      </q-select>
      <div v-if="review_lists === undefined || review_lists === null || review_lists.length === 0">
        No review_list found.
      </div>
    </div>

    <hr style="min-width: 500px;">

    <div class="column items-center">
      {{photos.length}} photos
    </div>    

  </q-page>
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

export default {
  name: 'query',

  data: () => ({
    photosMessage: 'init',
    selectedProject: undefined,
    selectedLocation: '',
    selectedQueryMode: 'query',
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
      review_lists: state => state.meta?.data?.review_lists,
    }),    
    ...mapGetters({
      api: 'api',
      apiGET: 'apiGET',
    }),
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
      await this.$store.dispatch('setProject', this.selectedProject);
      this.$nextTick(() => this.sendQuery());
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
