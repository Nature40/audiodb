<template>
<div style="overflow: auto" class="fit">

<q-page class="flex flex-center" v-if="photos.length === 0">
No image selected on the query page.
</q-page>  

<!--<div style="overflow: auto" class="fit" v-if="photos.length > 0">
  <span v-for="(photo, index) in photos" :key="photo" style="display: inline-block; width: 320px; height: 240px;">
    <img :src="api('photodb2', 'photos', photo, 'image.jpg') + '?width=320&height=240&cached'" @click="setIndex(index);" :class="{selected: index === photoIndex}" :alt="photo"/>
  </span>
</div>-->

<!--<q-infinite-scroll style="overflow: auto" class="fit" v-if="photos.length > 0">
  <span v-for="(photo, index) in photos" :key="photo" style="display: inline-block; width: 320px; height: 240px;">
    <img :src="api('photodb2', 'photos', photo, 'image.jpg') + '?width=320&height=240&cached'" @click="setIndex(index);" :class="{selected: index === photoIndex}" :alt="photo"/>
  </span>
</q-infinite-scroll>-->

<q-virtual-scroll 
  :items="browserRowIndexStart" 
  class="fit" 
  :virtual-scroll-item-size="240" 
  :virtual-scroll-slice-ratio-before="1"
  :virtual-scroll-slice-ratio-after="1"
>
  <template v-slot="{item}">
    <q-item dense style="padding: 0px;" :key="'row_' + getRowKey(item)">
      <!--{{item}} ==> {{getRowKey(item)}} |||-->
      <template v-for="col in browserColumns">
        <!--{{photos[item + col]}}-->
        <img 
          v-if="item + col < photos.length" 
          class="thumb" 
          :src="api('photodb2', 'photos', photos[item + col], 'image.jpg') + '?width=320&height=320'" 
          :key="'thumb_' + photos[item + col]" 
          @click="setIndex(item + col);" 
          :class="{selected: item + col === photoIndex}" 
          :alt="photos[item + col]"
        />
      </template>
    </q-item>
  </template>
</q-virtual-scroll>

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
    browserColumnCount() {
      return 5;
    },
    browserColumns() {
      let a = [];
      for(let i = 0; i < this.browserColumnCount; i++) {
        a[i] = i;
      }
      return a;     
    },
    browserRowIndexStart() {
      let photoCount = this.photos.length;
      let rowCount = photoCount / this.browserColumnCount;
      if(photoCount % this.browserColumnCount > 0) {
        rowCount++;
      }
      let a = [];
      for(let i = 0; i < rowCount; i++) {
        a[i] = i * this.browserColumnCount;
      }
      return a;
    },
  },

  methods: {
    ...mapActions({
      photoSetIndex: 'photo/setIndex',
    }),
    setIndex(index) {
      this.photoSetIndex(index);
      this.$router.push('/viewer');
    },
    getRowKey(rowIndexStart) {
      const a = []
      for (let i = 0; i < this.browserColumnCount; i++) {
        a.push(i)
      }
      return a.map(col => this.photos[rowIndexStart + col]).join();
    }
  },  

  async mounted() {

  },
}
</script>

<style scoped>
.selected {
  border: 2px dashed rgba(31, 16, 174, 0.7);
}

.thumb {
  min-width: 160px;
  min-height: 160px;
}
</style>
