<template>
  <q-page class="flex flex-center">
    <img :src="api('PhotoDB', 'photos', photo.id)" width="1280"/>
  </q-page>
</template>

<script>
import {mapState, mapGetters} from 'vuex'

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
    }),
  },

  async mounted() {
  },
}
</script>
