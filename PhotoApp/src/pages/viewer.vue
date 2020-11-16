<template>
  
<div style="overflow: auto" class="fit">

<q-page class="flex flex-center" v-if="photo === undefined">
  No photo selected.
</q-page>

<q-page v-if="photo !== undefined" class="flex flex-center column justify-center">
    <div class="col-auto">
      <q-btn :disable="!hasPrev" @click="move(-1)">prev</q-btn>
      {{photo.id}}
      <q-btn :disable="!hasNext" @click="move(+1)">next</q-btn>
      </div>
    <div style="width: 1280px; height: 960px;">
      <img :src="imageURL" style="max-width: 1280px; max-height: 960px;"/>
    </div>
</q-page>

</div>
  
  
  
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

export default {
  name: 'viewer',

  data: () => ({
  }),  

  computed: {
    ...mapState({
      photo: state => state.photo.photo,
    }),     
    ...mapGetters({
      api: 'api',
      hasPrev: 'photo/hasPrev',
      hasNext: 'photo/hasNext',
    }),
    imageURL() {
      return this.api('PhotoDB', 'photos', this.photo.id, 'image.jpg');
      //return this.api('PhotoDB', 'photos', this.photo.id, 'image.jpg') + '?width=1280&height=960';
    },
  },

  methods: {
    ...mapActions({
      move: 'photo/move',
    }),    
  },  

  async mounted() {
  },
}
</script>
