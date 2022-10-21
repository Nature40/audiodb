<template>
  <q-dialog v-model="show" style="width: 1000px;" :maximized="dialogMaximizedToggle">
      <q-card style="width: 1000px;">
        <q-bar>
          <q-icon name="menu_book"/>
          <div>Worklist manager</div>
          <q-space />
          <q-btn @click="dialoghelpShow = true;" icon="help_outline" dense flat style="margin-left: 10px;" title="Get help."></q-btn>
          <q-dialog
            v-model="dialoghelpShow"
            :maximized="dialoghelpMaximizedToggle"
            transition-show="slide-down"
            transition-hide="slide-up"
          >
            <q-card class="bg-grey-3 text-black">
              <q-bar>
                <q-icon name="help_outline" />
                <div>Help</div>
                <q-space />
                <q-btn dense flat icon="window" @click="dialoghelpMaximizedToggle = false" v-show="dialoghelpMaximizedToggle">
                  <q-tooltip v-if="dialoghelpMaximizedToggle">Minimize</q-tooltip>
                </q-btn>
                <q-btn dense flat icon="crop_square" @click="dialoghelpMaximizedToggle = true" v-show="!dialoghelpMaximizedToggle">
                  <q-tooltip v-if="!dialoghelpMaximizedToggle">Maximize</q-tooltip>
                </q-btn>
                <q-btn dense flat icon="close" v-close-popup>
                  <q-tooltip>Close</q-tooltip>
                </q-btn>
              </q-bar>

              <q-card-section class="q-pt-none">
                <div class="text-h6">Select worklist</div>
              </q-card-section>              
                           
            </q-card>
          </q-dialog>
          <q-btn dense flat icon="window" @click="dialogMaximizedToggle = false" v-show="dialogMaximizedToggle">
            <q-tooltip v-if="dialoghelpMaximizedToggle">Minimize</q-tooltip>
          </q-btn>
          <q-btn dense flat icon="crop_square" @click="dialogMaximizedToggle = true" v-show="!dialogMaximizedToggle">
            <q-tooltip v-if="!dialoghelpMaximizedToggle">Maximize</q-tooltip>
          </q-btn>          
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>     

        <q-card-section>
          <q-select 
            rounded 
            outlined 
            bottom-slots 
            v-model="worklist" 
            :options="worklists" 
            label="Worklist" 
            dense 
            options-dense 
            options-selected-class="text-deep-blue" 
            style="min-width: 400px;"
            title="Select one image list."
          >
            <template v-slot:prepend>
              <q-icon name="playlist_play" />
            </template>
            <template v-slot:option="scope">
              <q-item v-bind="scope.itemProps">
                {{scope.opt.id}}  <span style="color: grey; padding-left: 10px;">{{scope.opt.count}}</span>
              </q-item>
            </template>
            <template v-slot:selected-item="scope">
              {{scope.opt.id}}
            </template>
          </q-select>
        </q-card-section>                 

        <q-separator />

        <q-card-actions align="right">
          <q-btn v-close-popup flat color="primary" label="Apply" @click="onApply" />
        </q-card-actions>
      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'audio-settings',
  setup () {
    const show = ref(false);
    return {
      show,
    };
  },
  data() {
    return {
      dialoghelpShow: false,
      dialoghelpMaximizedToggle: false,
      dialogMaximizedToggle: false,
      actionLoading: false,
      actionError: undefined,
      worklists: [], 
      worklist: undefined,                 
    };
  },
  computed: {
    ...mapState({
    }),
  },
  methods: {
    setActionStatus(loading, error) {
      this.actionLoading = loading;
      this.actionError = error;
    },
    async refresh() {
      try {
        this.setActionStatus(true, undefined);
        var urlPath = 'worklists';
        var params = {};
        var response = await this.$api.get(urlPath, {params});
        this.worklists = response.data.worklists;
        this.setActionStatus(false, undefined);
      } catch(e) {
        this.setActionStatus(false, e.response && e.response.data ? (e.response.data.error ? e.response.data.error : e.response.data) : 'error');
        //console.log(e);
      }
    },
    onApply() {
      this.$emit('set_worklist', this.worklist);
    },  
  },
  watch: {
    show() {
      if(this.show) {
        this.refresh();
      }
    },
  },
  async mounted() {
  },
});
</script>

<style scoped>

</style>
