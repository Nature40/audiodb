<template>
<div style="overflow: auto" class="fit">

<q-page class="flex flex-center" v-if="photos.length === 0">
No photos selected in query.
</q-page>  

<div style="overflow: auto" class="fit" v-if="photos.length > 0">
  <span v-for="(photo, index) in photos" :key="photo.id" style="display: inline-block; width: 320px; height: 240px;">
    <img :src="api('PhotoDB', 'photos', photo.id, 'image.jpg') + '?width=320&height=240&cached'" @click="setIndex(index);" :class="{selected: index === photoIndex}" :alt="photo.id"/>
  </span>
</div>

</div>
  
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

export default {
  name: 'browser',

  data: () => ({
    photosMessage: 'init',
  }),  

  computed: {
    ...mapState({
      photos: state => state.photos.data,
      photoIndex: state => state.photo.index,      
    }),     
    ...mapGetters({
      api: 'api',
      apiGET: 'apiGET',
    }),
  },

  methods: {
    ...mapActions({
      photoSetIndex: 'photo/setIndex',
    }),
    setIndex(index) {
      this.photoSetIndex(index);
      this.$router.push('/viewer');
    },
  },  

  async mounted() {

  },
}
</script>

<style scoped>
.selected {
  border: 2px dashed rgba(31, 16, 174, 0.7);
}
</style>
