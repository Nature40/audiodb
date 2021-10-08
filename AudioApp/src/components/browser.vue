<template>
  <q-dialog v-model="show">
      <q-card>
        <q-bar>
          <div>Browser</div>
          <q-space />
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>

        <q-card-section>
          Browse
        </q-card-section>

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
    };
  },
  computed: {
    selectedSampleId() {
      return this.$route.query.sample;
    },
    hasPrevPageSamples() {
      return this.totalSamplesCount !== undefined && this.samplesOffset > 0;
    },
    hasNextPageSamples() {
      return this.totalSamplesCount !== undefined && this.samplesOffset + this.samples.length < this.totalSamplesCount;
    },
  },
  methods: {
    async querySamples() {
      console.log("querySamples");
      try {
        var response = await this.$api.get('samples2', { params: { samples: true, count: true, limit: this.samplesLimit, offset: this.samplesOffset,} });
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
      this.querySamples();
    },
    onNextPage() {
      this.samplesOffset += this.samplesLimit;
      if(this.samplesOffset + this.samplesLimit > this.totalSamplesCount) {
        this.samplesOffset = this.totalSamplesCount - this.samplesLimit;
      }
      if(this.samplesOffset < 0) {
        this.samplesOffset = 0;
      }
      this.querySamples();
    },
  },
  mounted() {
    this.querySamples();
  },
});
</script>

<style scoped>
.selected-sample {
  background-color: rgba(0, 0, 0, 0.021);
  font-weight: bold;
}
</style>
