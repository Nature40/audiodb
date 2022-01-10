<template>
  <q-dialog v-model="show">
    <q-layout view="Lhh lpR fff" container class="bg-white" style="min-width: 1000px;">
      <q-header class="bg-white text-black">                
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
            {label: 'At', value: 'one'},
          ]"
        />
        <span v-if="toggleLocation === 'all'">Location</span>
        <q-select v-if="toggleLocation === 'one'" outlined v-model="selectedLocation" :options="filteredLocations" label="Location" class="col" dense clearable use-input @filter="locationfilterFn" input-debounce="0" :loading="requestMetaLoading">
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps" dense>
              <q-item-section>
                <q-item-label>{{scope.opt.label}}</q-item-label>
              </q-item-section>
            </q-item>              
          </template>
        </q-select>
      </q-card-section>

      <q-separator/>

      <q-card-section class="text-h5 row">
        <q-btn-toggle
          v-model="toggleTime"
          class="custom-toggle"
          no-caps
          rounded
          unelevated
          toggle-color="primary"
          color="white"
          text-color="primary"
          :options="[
            {label: 'All', value: 'all'},
            {label: 'At', value: 'one'},
          ]"
        />
        <span v-if="toggleTime === 'all'">Time</span>
        <q-badge v-if="toggleTime === 'one' && requestMetaError" color="red">Error loading timestamps<q-btn color="grey" @click="requestRefreshMeta">refresh</q-btn></q-badge>
        <q-select v-if="toggleTime === 'one'" outlined v-model="selectedTimestamp" :options="filteredTimestamps" label="Time" class="col" dense clearable use-input @filter="timestampfilterFn" input-debounce="0" :loading="requestMetaLoading">
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps" dense>
              <q-item-section>
                <q-item-label>{{scope.opt.date}}<!-- <span style="color: grey;">{{scope.opt.time}}</span>--></q-item-label>
              </q-item-section>
            </q-item>              
          </template>
          <template v-slot:selected>
            <span v-if="selectedTimestamp">{{selectedTimestamp.date}} <span style="color: grey;">{{selectedTimestamp.time}}</span></span>
          </template>
        </q-select>
      </q-card-section>

      <q-separator/>
        <q-inner-loading :showing="$store.state.project.loading">
          <q-spinner-gears size="50px" color="primary" />
          Loading metadata....
        </q-inner-loading>
        <q-inner-loading :showing="$store.state.project.error !== undefined">
          <q-badge color="red">Error loading metadata </q-badge><q-btn color="grey" @click="$store.dispatch('project/refresh');">refresh</q-btn>
        </q-inner-loading> 
      
      </q-header>

      <q-page-container>

      <q-card-section>
        
        <q-markup-table separator="cell" dense bordered>
          <thead>
            <tr>
              <th class="text-left">Location</th>
              <th class="text-left">Time</th>
              <th class="text-left">Device</th>
              <th class="text-left">Id</th>
              </tr>
          </thead>
          <tbody>
            <tr v-for="sample, index in samples" :key="sample.id" @click="onSelectSample(sample.id)" :class="{'selected-sample': index === indexOfSelectedSampleId}">
              <td class="text-left">{{sample.location}}</td>
              <td class="text-left">{{sample.date}} <span style="color: grey;">{{sample.time}}</span></td>
              <td class="text-left">{{sample.device}}</td>
              <td class="text-left">{{sample.id}}</td>
            </tr>
          </tbody>
        </q-markup-table> 
      </q-card-section>

      </q-page-container>

      <q-footer class="bg-white text-black">
        <q-separator/>
        <q-toolbar>
        <q-space />
        <q-btn :disabled="!hasPrevPageSamples" @click="onPrevPage">prev page</q-btn>
        {{samplesOffset}}
        <q-btn :disabled="!hasNextPageSamples"  @click="onNextPage">next page</q-btn>
        
        <q-space />
        </q-toolbar>
        <q-separator/>
        <span v-if="totalSamplesCount !== undefined">{{totalSamplesCount}} samples received</span>

        <q-inner-loading :showing="requestLoading">
          <q-spinner-gears size="50px" color="primary" />
        </q-inner-loading>

        <q-inner-loading :showing="requestError">            
          <q-badge color="red">Error loading request</q-badge><q-btn color="grey" @click="requestRefresh">refresh</q-btn>
        </q-inner-loading>

        <q-inner-loading :showing="refreshConfirmRequested">            
          <q-badge color="red">Querying all data at once may take several minutes. Try to narrow the query by selecting location and/or time.</q-badge><q-btn color="grey" @click="refreshConfirmRequested = false; refreshConfirmed = true; requestRefresh()">Nevertheless, execute this query.</q-btn>
        </q-inner-loading>
      </q-footer>

    </q-layout>
    <!--</q-card>-->
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
      samplesLimit: 1000,    
      toggleLocation: 'all',
      toggleTime: 'all',
      selectedLocation: undefined,
      selectedTimestamp: undefined,
      refreshRequested: false,
      refreshConfirmRequested: false,
      refreshConfirmed: false,
      refreshRequestedMeta: false,
      requestLoading: false,
      requestError: false,
      filteredLocations: [],
      filteredTimestamps: [],
      timestamps_of_location: undefined,
      requestMetaLoading: false,
      requestMetaError: false,
      movePrevSelectedSampleRequested: false,
      moveNextSelectedSampleRequested: false,
    };
  },
  computed: {
    ...mapState({
    }),     
    selectedSampleId() {
      return this.$route.query.sample;
    },
    indexOfSelectedSampleId() {
      if(this.selectedSampleId === undefined || this.samples === undefined) {
        return -1;
      }
      return this.samples.findIndex(sample => sample.id === this.selectedSampleId);
    },
    offsetOfSelectedSampleId() {
      if(this.indexOfSelectedSampleId < 0) {
        return -1;
      }
      return this.samplesOffset + this.indexOfSelectedSampleId;
    },    
    hasSelectedSamplePrev() {
      return this.offsetOfSelectedSampleId < 0 ? false : this.offsetOfSelectedSampleId > 0;
    },
    hasSelectedSampleNext() {
      return this.offsetOfSelectedSampleId < 0  || this.totalSamplesCount === undefined ? false : this.offsetOfSelectedSampleId < (this.totalSamplesCount - 1);
    },
    hasPrevPageSamples() {
      return this.totalSamplesCount !== undefined && this.samplesOffset > 0;
    },
    hasNextPageSamples() {
      return this.totalSamplesCount !== undefined && this.samplesOffset + this.samplesLimit < this.totalSamplesCount;
    },
    locations() {
      const d = this.$store.state.project.data;
      if(d === undefined || d.locations === undefined || d.locations.length === 0) {
        return [{label: '(no locations)', value: undefined}];
      }
      return d.locations.map(location => {
        return location === null ? {label: '(unknown)', value: 'null'} : {label: location, value: location};
      });
    },
    timestamps() {
      if(this.toggleLocation === 'one' && this.selectedLocation && this.timestamps_of_location && this.timestamps_of_location.location === this.selectedLocation.value) {
        const timestamps = this.timestamps_of_location.timestamps;
        if(timestamps.length === 0) {
          return [{date: '(no timestamps)', value: undefined}];
        }
        return timestamps.map(t => {
          return t.timestamp <= 0 ? {date: '(unknown)', value: 0} : {date: t.date, time: t.time, value: t.timestamp};
        });
      } else {
        const d = this.$store.state.project.data;
        /*if(d === undefined || d.timestamps === undefined || d.timestamps.length === 0) {
          return [{date: '(no timestamps)', value: undefined}];
        }
        return d.timestamps.map(t => {
          return t.timestamp <= 0 ? {date: '(unknown)', value: 0} : {date: t.date, time: t.time, value: t.timestamp};
        });*/
        if(d === undefined || d.dates === undefined || d.dates.length === 0) {
          return [{date: '(no timestamps)', value: undefined}];
        }
        return d.dates.map(t => {
          return t.timestamp <= 0 ? {date: '(unknown)', value: 0} : {date: t.date, value: t.timestamp};
        });
      }
    },    
  },
  watch: {
    refreshRequested() {
      if(this.refreshRequested) {
        if(
          !this.refreshConfirmed
          && (this.toggleLocation === 'all' || (this.toggleLocation === 'one' && !this.selectedLocation)) 
          && (this.toggleTime === 'all' || (this.toggleTime === 'one' && !this.selectedTimestamp))
        ) {
          this.refreshConfirmRequested = true;
        } else {
          this.refreshConfirmRequested = false;
          this.$nextTick(() => this.querySamples());
        }
        this.refreshRequested = false;
        this.refreshConfirmed = false;
      }
    },
    refreshRequestedMeta() {
      if(this.refreshRequestedMeta) {
        this.$nextTick(() => this.queryMeta());
        this.refreshRequestedMeta = false;
      }
    },
    toggleLocation() {
      this.samplesOffset = 0;
      this.requestRefresh();
      this.requestRefreshMeta();
    },
    selectedLocation() {
      this.samplesOffset = 0;
      this.requestRefresh();
      this.requestRefreshMeta();
    },
    toggleTime() {
      this.samplesOffset = 0;
      this.requestRefresh();
    },
    selectedTimestamp() {
      this.samplesOffset = 0;
      this.requestRefresh();
    },
    movePrevSelectedSampleRequested() {
      if(this.movePrevSelectedSampleRequested) {
        if(this.hasSelectedSamplePrev) {
          if(this.indexOfSelectedSampleId > 0) {
            this.onSelectSample(this.samples[this.indexOfSelectedSampleId - 1].id);
            this.movePrevSelectedSampleRequested = false;
          } else {
            if(this.hasPrevPageSamples) {
              this.onPrevPage(true);
            } else {
              this.movePrevSelectedSampleRequested = false; 
            }
          }
        } else {
          this.movePrevSelectedSampleRequested = false;
        }
      }
    },
    moveNextSelectedSampleRequested() {
      if(this.moveNextSelectedSampleRequested) {
        if(this.hasSelectedSampleNext) {
          if(this.indexOfSelectedSampleId < (this.samples.length - 1)) {
            this.onSelectSample(this.samples[this.indexOfSelectedSampleId + 1].id);
            this.moveNextSelectedSampleRequested = false;
          } else {
            if(this.hasNextPageSamples) {
              this.onNextPage(true);
            } else {
              this.moveNextSelectedSampleRequested = false; 
            }
          }
        } else {
          this.moveNextSelectedSampleRequested = false;
        }
      }
    },    
  },
  methods: {
    async querySamples() {
      console.log("querySamples");
      try {        
        let params = { samples: true, count: true, limit: this.samplesLimit, offset: this.samplesOffset,};
        if(this.toggleLocation === 'one' && this.selectedLocation) {
          params.location = this.selectedLocation.value;
        }
        if(this.toggleTime === 'one' && this.selectedTimestamp) {
          //params.timestamp = this.selectedTimestamp.value;
          var t = this.selectedTimestamp.value;
          if(t === 0) {
            params.end = 0;
          } else {
            params.start = t;
            params.end = t + (86400 - 1);
          }          
        }
        this.requestError = false;
        this.requestLoading = true;
        var response = await this.$api.get('samples2', { params });
        this.requestLoading = false;
        var samples = response.data?.samples;
        this.samples = samples === undefined ? [] : samples;
        if(this.moveNextSelectedSampleRequested) {
          if(this.samples.length > 0) {
            this.onSelectSample(this.samples[0].id);
          }
        } else if(this.movePrevSelectedSampleRequested) {
          if(this.samples.length > 0) {
            this.onSelectSample(this.samples[this.samples.length - 1].id);
          }
        }
        this.movePrevSelectedSampleRequested = false;
        this.moveNextSelectedSampleRequested = false;
        this.totalSamplesCount = response.data?.count;
      } catch(e) {
        this.requestError = true;
        this.requestLoading = false;
        console.log(e);
      }
    },
    async onSelectSample(sampleId) {
      const query = Object.assign({}, this.$route.query);
      query.sample = sampleId;
      console.log(query);
      await this.$router.replace({ query });
    },
    onPrevPage(force) {
      this.samplesOffset -= this.samplesLimit;
      if(this.samplesOffset < 0) {
        this.samplesOffset = 0;
      }
      this.requestRefresh(force);
    },
    onNextPage(force) {
      this.samplesOffset += this.samplesLimit;
      if(this.samplesOffset + this.samplesLimit > this.totalSamplesCount) {
        this.samplesOffset = this.totalSamplesCount - this.samplesLimit;
      }
      if(this.samplesOffset < 0) {
        this.samplesOffset = 0;
      }
      this.requestRefresh(force);
    },
    requestRefresh(force) {
      if(force) {
        this.refreshConfirmed = true;
      }
      this.refreshRequested = true;
    },
    requestRefreshMeta() {
      this.refreshRequestedMeta = true;
    },
    async queryMeta() {
      try {
        var params = {};
        if(this.selectedLocation) {
          //params.timestamps_of_location = this.selectedLocation.value;
          params.dates_of_location = this.selectedLocation.value;
        }
        this.timestamps_of_location = undefined;
        this.requestMetaError = false;
        this.requestMetaLoading = true;
        var response = await this.$api.get('projects/' + this.$store.state.projectId, {params});
        this.requestMetaLoading = false;
        //var tol = response.data.project.timestamps_of_location;
        var tol = response.data.project.dates_of_location;
        if(tol !== undefined) {
          if(tol.location === null) {
            tol.location = 'null';
          }
          this.timestamps_of_location = tol;
        }
      } catch(e) {
        this.requestMetaError = true;
        this.requestMetaLoading = false;
        console.log(e);
      }
    },
    locationfilterFn(val, update, abort) {
      update(() => {
        const needle = val.toLowerCase()
        this.filteredLocations = this.locations.filter(v => v.label.toLowerCase().indexOf(needle) > -1)
      });
    },
    timestampfilterFn(val, update, abort) {
      update(() => {
        const needle = val.toLowerCase()
        this.filteredTimestamps = this.timestamps.filter(v => v.date.toLowerCase().indexOf(needle) > -1)
      });
    },
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