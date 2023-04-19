<template>
  <q-dialog v-model="show">
      <q-card v-if="show" id="card" style="min-width: 1000px;">
        <q-bar>
          <q-icon name="image"/>
          <div>Meta view</div>
          <div v-if="loading" class="text-primary">
            <q-spinner color="primary" />
            Processing ...
          </div>
          <div v-if="error" class="text-red">
            Error at processing or network connection.
          </div>          
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
                The stored YAML meta data of the audio file is shown.
              </q-card-section>                
            </q-card>
          </q-dialog>          
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>
        <pre style="padding: 10px;">
{{data}}
        </pre>

        <q-separator />

      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'meta-view',

  props: [],

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
      sample: undefined,
      loading: false,
      error: false,
      data: undefined,      
    };
  },
  computed: {
    ...mapState({
    }),            
  },
  methods: {
    async refresh() {
      this.data = undefined;
      if(this.sample === undefined) {
        return;
      }
      try {
        var urlPath = 'samples2/' + this.sample + '/meta.yaml';
        var params = {};
        this.loading = true;
        this.error = false;
        var response = await this.$api.get(urlPath, {params});
        this.loading = false;
        this.error = false;
        this.data = response.data;
      } catch(e) {
        this.loading = false;
        this.error = true;
        console.log(e);
      }
    },
  },
  watch: {
    show() {
      this.refresh();
    },
  },
  async mounted() {
  },
});
</script>

<style scoped>

</style>
