<template>
  <q-page class="flex flex-center">
<div>
<ul>
<li>On query page select set of camera trap photos.</li>
<li>On browser page select one photo.</li>
<li>On viewer page inspect one photo.</li>
</ul>
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
      var r =  await this.apiGET(['PhotoDB','photos']);
      this.photos = r.data.photos;
      this.photosMessage = undefined;
    } catch {
      this.photosMessage = 'error';
    }
  },
}
</script>
