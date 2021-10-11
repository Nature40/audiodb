<template>
  <q-dialog v-model="show">
      <q-card style="width: 500px;">
        <q-bar>
          <div>Browser</div>
          <q-space />
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>

        <q-separator/>

        <q-card-section class="text-h5 row">
          <q-btn-toggle
            v-model="toggleLocation"
            class="custom-toggle"
            no-caps
            rounded
            unelevated
            toggle-color="primary"
            color="white"
            text-color="primary"
            :options="[
              {label: 'All', value: 'all'},
              {label: 'Selected', value: 'one'},
            ]"
          />
          <span v-if="toggleLocation === 'all'">Location</span>
          <q-select v-if="toggleLocation === 'one'" outlined v-model="selectedLocation" :options="locations" label="Location" class="col" dense />
        </q-card-section>



        <q-separator/>

        <q-card-section>
          <q-markup-table>
            <thead>
              <tr>
                <th class="text-left">Sample</th>
               </tr>
            </thead>
            <tbody>
              <tr v-for="sample in samples" :key="sample.id" @click="onSelectSample(sample.id)">
                <td class="text-left" :class="{'selected-sample': sample.id === selectedSampleId}">{{sample.id}}</td>
              </tr>
            </tbody>
          </q-markup-table>
          <q-btn :disabled="!hasPrevPageSamples" @click="onPrevPage">prev page</q-btn>
          {{samplesOffset}}
          <q-btn :disabled="!hasNextPageSamples"  @click="onNextPage">next page</q-btn>
        </q-card-section>

        <q-card-section>
          <span v-if="totalSamplesCount !== undefined">{{totalSamplesCount}} samples received</span>
        </q-card-section>

      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'audio-browser',
  setup () {
    const show = ref(false);
    return {
      show,      
    };
  },
  data() {
    return {
      totalSamplesCount: undefined,
      samples: [],
      samplesOffset: 0,
      samplesLimit: 100,    
      toggleLocation: 'all',
      selectedLocation: undefined,
      refreshRequested: false,
    };
  },
  computed: {
    ...mapState({
    }),     
    selectedSampleId() {
      return this.$route.query.sample;
    },
    hasPrevPageSamples() {
      return this.totalSamplesCount !== undefined && this.samplesOffset > 0;
    },
    hasNextPageSamples() {
      return this.totalSamplesCount !== undefined && this.samplesOffset + this.samples.length < this.totalSamplesCount;
    },
    locations() {
      const d = this.$store.state.project.data;
      if(d === undefined || d.locations === undefined || d.locations.length === 0) {
        return {label: '(unknown)', value: undefined};
      }
      return d.locations.map(location => {
        return location === null ? {label: '(unknown)', value: null} : {label: location, value: location};
      });
    },
  },
  watch: {
    refreshRequested() {
      if(this.refreshRequested) {
        this.$nextTick(() => this.querySamples());
        this.refreshRequested = false;
      }
    },
    toggleLocation() {
      this.requestRefresh();
    },
    selectedLocation() {
      this.requestRefresh();
    },
  },
  methods: {
    async querySamples() {
      console.log("querySamples");
      try {
        let params = { samples: true, count: true, limit: this.samplesLimit, offset: this.samplesOffset,};
        if(this.toggleLocation === 'one' && this.selectedLocation !== undefined) {
          params.location = this.selectedLocation.value === null ? 'null' : this.selectedLocation.value;
        }
        var response = await this.$api.get('samples2', { params });
        var samples = response.data?.samples;
        this.samples = samples === undefined ? [] : samples;
        this.totalSamplesCount = response.data?.count;
      } catch(e) {
        console.log(e);
      }
    },
    async onSelectSample(sampleId) {
      const query = Object.assign({}, this.$route.query);
      query.sample = sampleId;
      console.log(query);
      await this.$router.replace({ query });
    },
    onPrevPage() {
      this.samplesOffset -= this.samplesLimit;
      if(this.samplesOffset < 0) {
        this.samplesOffset = 0;
      }
      this.requestRefresh();
    },
    onNextPage() {
      this.samplesOffset += this.samplesLimit;
      if(this.samplesOffset + this.samplesLimit > this.totalSamplesCount) {
        this.samplesOffset = this.totalSamplesCount - this.samplesLimit;
      }
      if(this.samplesOffset < 0) {
        this.samplesOffset = 0;
      }
      this.requestRefresh();
    },
    requestRefresh() {
      this.refreshRequested = true;
    }
  },
  mounted() {
    this.requestRefresh();
  },
});
</script>

<style scoped>
.selected-sample {
  background-color: rgba(0, 0, 0, 0.021);
  font-weight: bold;
}

.custom-toggle {
  border: 1px solid #027be3;
}
</style>
