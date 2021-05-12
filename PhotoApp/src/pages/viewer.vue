<template>
  
<div style="overflow: auto" class="fit">

<q-page class="flex flex-center" v-if="photo === undefined">
  No photo selected.
</q-page>

<q-page v-if="photo !== undefined" class="flex flex-center column justify-center">
    <div class="col-auto">
      <q-btn :disable="!hasPrev" @click="move(-1)">prev</q-btn>
      <span class="time-text">{{dateText}}</span>
      <q-btn :disable="!hasNext" @click="move(+1)">next</q-btn>
    </div>
    <div style="width: 1024px; height: 768px;">
      <img :src="imageURL" style="max-width: 1024px; max-height: 768px;"/>
    </div>
    <div>
      <q-btn :disable="!hasPrev" @click="$refs.tagsDialog.show()">tags</q-btn>
    </div>    
</q-page>

<tags-dialog ref="tagsDialog"/>

</div>
  
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

import tagsDialog from '../components/tags-dialog'

function pad(number) {
  if (number < 10) {
    return '0' + number;
  }
  return number;
}

export default {
  name: 'viewer',

  components: {
    tagsDialog,
  },

  data: () => ({
  }),  

  computed: {
    ...mapState({
      photo: state => state.photo.photo,
      photoMeta: state => state.photo.meta,
    }),     
    ...mapGetters({
      api: 'api',
      hasPrev: 'photo/hasPrev',
      hasNext: 'photo/hasNext',
    }),
    imageURL() {
      return this.api('photodb2', 'photos', this.photo, 'image.jpg');
      //return this.api('photodb2', 'photos', this.photo, 'image.jpg') + '?width=1280&height=960';
    },
    dateText() {
      var date = this.photoMeta.date;
      if(date === undefined) {
        return "0000-00-00 00:00";
      }
      return date.getUTCFullYear() +
        '-' + pad(date.getUTCMonth() + 1) +
        '-' + pad(date.getUTCDate()) +
        ' ' + pad(date.getUTCHours()) +
        ':' + pad(date.getUTCMinutes());
    }
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

<style scoped>
.time-text {
  font-family: "Lucida Console", Courier, monospace;
}

</style>
