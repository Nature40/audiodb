<template>
  

<div style="overflow: auto" class="fit">
<span v-for="(photo, index) in photos" :key="photo.id">
<img :src="api('PhotoDB', 'photos', photo.id)" width="320" @click="setIndex(index);" :class="{selected: index === photoIndex}" :alt="photo.id"/>
</span>
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
