<template>
  <q-page class="flex flex-center">

<div v-for="photo in photos" :key="photo.id">
{{photo.id}}
</div>

  </q-page>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'PageIndex',

  data: () => ({
    photos: [],
    photosMessage: 'init',
  }),  

  computed: {
    ...mapGetters({
      api: 'api',
      apiGET: 'apiGET',
    }),
  },

  async mounted() {
    this.photosMessage = 'loading';
    try {
      var r =  await this.apiGET(['photo']);
      this.photos = r.data.photos;
      this.photosMessage = undefined;
    } catch {
      this.photosMessage = 'error';
    }
  },
}
</script>
