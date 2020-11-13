<template>
  <q-page class="flex flex-center">

    <q-select rounded outlined bottom-slots v-model="location" :options="locations" label="Location" dense options-dense options-selected-class="text-deep-blue" style="min-width: 200px;">
      <template v-slot:prepend>
        <q-icon name="location_on" />
      </template>
      <template v-slot:option="scope">
        <q-item v-bind="scope.itemProps" v-on="scope.itemEvents">
          {{scope.opt.id}}
        </q-item>
      </template>
      <template v-slot:selected-item="scope">
        {{scope.opt.id}}
      </template>

    </q-select>

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
    location: "",
  }),  

  computed: {
    ...mapState({
      photos: state => state.photos.data,
      photoIndex: state => state.photo.index,
      locations: state => state.meta?.data?.locations,
    }),    
    ...mapGetters({
      api: 'api',
      apiGET: 'apiGET',
    }),
  },

  watch: {
    location() {
      this.photosQuery({location: this.location.id});
    },
  },

  methods: {
    ...mapActions({
      photosQuery: 'photos/query',
      photoSetIndex: 'photo/setIndex',
    }),
    setIndex(index) {
      this.photoSetIndex(index);
      this.$router.push('/viewer');
    },
  },

  async mounted() {
    this.$store.dispatch('meta/init');
  },
}
</script>

<style scoped>
.selected {
  color: red;
}
</style>
