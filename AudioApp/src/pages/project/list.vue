<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-btn @click="refresh">Action</q-btn>
        <q-space />
        <q-btn @click="onPrev" :loading="indexActionLoading">Prev</q-btn>
        Index  [<b>{{index}}</b>]
        <q-btn @click="onNext" :loading="indexActionLoading">Next</q-btn>
        <span v-if="indexActionError" style="color: red;">{{indexActionError}}</span>
        <q-space />
        List [<b>{{listId}}</b>]
    </q-toolbar>
    <div>

    </div>

  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';


export default defineComponent({
  name: 'List',

  components: {

  },

  data() {
    return {
      indexActionLoading: false,
      indexActionError: undefined,
    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,
    }),
    listId() {
      return this.$route.query.list;
    },
    index() {
      return parseInt(this.$route.query.index);
    }
  },

  methods: {
    refresh() {
      this.$router.push({path: this.$route.path, query: {...this.$route.query, index: 42} });
    },
    setActionStatus(loading, error) {
      this.indexActionLoading = loading;
      this.indexActionError = error;
    },
    setEntry(e) {
      this.$router.replace({path: this.$route.path, query: {...this.$route.query, index: e.index}});
    },
    async onPrev() {
      try {
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists/' + this.listId + '/last';
        var params = {last: this.index - 1,};
        var response = await this.$api.get(urlPath, {params});
        this.setEntry(response.data);
        this.setActionStatus(false, undefined);
      } catch(e) {
        this.setActionStatus(false, e.response && e.response.data ? e.response.data : 'error');
        console.log(e);
      }
    },
    async onNext() {
      try {
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists/' + this.listId + '/first';
        var params = {first: this.index + 1,};
        var response = await this.$api.get(urlPath, {params});
        this.setEntry(response.data);
        this.setActionStatus(false, undefined);
      } catch(e) {
        this.setActionStatus(false, e.response && e.response.data ? e.response.data : 'error');
        console.log(e);
      }
    },    
  },

  watch: {
    
  },
  
  async mounted() {
    this.refresh();
  },  
})
</script>

<style scoped>

</style>
