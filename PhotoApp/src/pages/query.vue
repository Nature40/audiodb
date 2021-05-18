<template>
  <q-page class="fit column items-center">

    <div v-if="projects === undefined">
      loading projects
    </div>
    <q-select v-else rounded outlined bottom-slots v-model="project" :options="projects" label="Project" dense options-dense options-selected-class="text-deep-blue" style="min-width: 200px;">
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
      <q-select rounded outlined bottom-slots v-model="location" :options="locations" label="Location" dense options-dense options-selected-class="text-deep-blue" style="min-width: 200px;">
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
      <div>
        {{photos.length}} photos
      </div>
    </div>

    <!--<table>
      <tr v-for="(photo, index) in photos" :key="photo.id" @click="setIndex(index);" :class="{selected: index === photoIndex}">
        <td>{{photo.id}}</td>
      </tr>
    </table>-->

  </q-page>
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

export default {
  name: 'query',

  data: () => ({
    photosMessage: 'init',
    project: undefined,
    location: "",
  }),  

  computed: {
    ...mapState({
      photos: state => state.photos.data,
      photoIndex: state => state.photo.index,
      projects: state => state.projects?.data?.projects,
      meta: state => state.meta?.data,
      locations: state => state.meta?.data?.locations,
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
        this.project = undefined; 
      } else {
        if(this.project === undefined) {
          this.project = this.projects[0];
        }
      }
    },
    project() {
      if(this.project !== undefined) {
        this.metaQuery({project: this.project});
      }
    },
    location() {
      if(this.location !== undefined && this.location !== null && this.location !== '') {
        this.photosQuery({project: this.project, location: this.location});
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
