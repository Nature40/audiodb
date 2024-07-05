<template>
  
<q-dialog v-model="shown" persistent>
  <q-card>
    <q-bar>
      <div>Tags</div>
      <q-space />
      <q-btn dense flat icon="close" v-close-popup>
        <q-tooltip>Close</q-tooltip>
      </q-btn>
    </q-bar>

    <q-card-section>
      <q-select
        label="Selected tags"
        hint="To add new tag, type and press ENTER."
        filled
        v-model="selectedTags"
        use-input
        use-chips
        multiple
        hide-dropdown-icon
        input-debounce="0"
        @new-value="createValue"
        style="width: 250px"
        :options="filteredTags"
        @filter="filterFn"
      />
    </q-card-section>

    <q-card-section class="scroll" style="max-height: 70vh">
      <div v-for="tag in selectedTags" :key="tag">
        {{tag}}
      </div> 
    </q-card-section>

    <q-card-actions align="right">
      <q-btn flat label="Cancel" color="primary" v-close-popup />
      <q-btn flat label="Save" color="primary" v-close-popup />
    </q-card-actions>
  </q-card>
</q-dialog>


  
  
  
</template>

<script>
import {mapState, mapGetters, mapActions} from 'vuex'

export default {
  name: 'tags-dialog',

  data: () => ({
    shown: false,
    allTags: ['a','b','c'],
    filteredTags: [],
    selectedTags: [],
  }),  

  computed: {
    ...mapState({
      photo: state => state.photo.photo,
      photoMeta: state => state.photo.meta,
    }),
  },

  methods: {
    ...mapActions({
      move: 'photo/move',
    }),
    show() {
      this.shown = true;
    },
    createValue (val, done) {
      done(val, 'add-unique')
    },
    filterFn (val, update) {
      update(() => {
      var f = val === undefined || val === null || val === '' ? '' : val.toLowerCase();
      this.filteredTags = this.allTags.filter(v => v.toLowerCase().indexOf(f) > -1);
      });
    }           
  },  

  async mounted() {
  },
}
</script>

<style scoped>


</style>
