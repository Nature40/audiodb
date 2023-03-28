<template>
  <q-dialog v-model="show" :maximized="dialogMaximizedToggle">
    <q-layout view="Lhh lpR fff" container class="bg-white" style="min-width: 1000px;">
      <q-header class="bg-white text-black">                
      <q-bar>
        <q-icon name="menu_book"/>
        <div>Browser</div>
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
            <br>
            <q-card-section class="q-pt-none">
              <div class="text-h6">Browse audio samples</div>
              <i>The collection of audio samples is filtered by location and by time.</i>
              <br><i>The browser table lists all audio samples of the current subset of location and time.</i>
              <br><i>On large subsets click the prev-page/next-page-buttons to browse all audio samples.</i>
              <br><i>It is recommended to select both, one location and one time, to get small subsets in the table.</i>
            </q-card-section>

            <q-card-section class="q-pt-none">
              <div class="text-h6">Location</div>
              <i>Select one location from the list or none for all locations.</i>
            </q-card-section>
            
            <q-card-section class="q-pt-none">
              <div class="text-h6">Time</div>
              <i>Select one date from the list or none for all dates.</i>
            </q-card-section>

            <q-card-section class="q-pt-none">
              <div class="text-h6">Select audio sample</div>
              <i>Click on a table row to view that audio sample in the audio viewer.</i>
              <br><i>In audio viewer, there are prev-sample/next-sample-buttons which refer to the sample position in this current subset table.</i>
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

      <q-separator/>        

      <q-card-section class="text-h5 row">
        <q-icon name="place" size="lg" color="grey-4" />
        <q-select outlined v-model="selectedLocation" :options="filteredLocations" :label="selectedLocation ? 'Location' : '(All locations)'" class="col" dense clearable use-input @filter="locationfilterFn" input-debounce="0" :loading="requestMetaLoading">
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps" dense>
              <q-item-section>
                <q-item-label>{{scope.opt.label}}</q-item-label>
              </q-item-section>
            </q-item>              
          </template>
        </q-select>
        <q-btn @click="$refs.inventory.show = true;" icon="view_timeline" title="Show audio devices inventory."  push round style="margin-left: 20px;" />
        <audio-inventory ref="inventory"/>
      </q-card-section>

      <q-separator/>

      <q-card-section class="text-h5 row">
        <q-icon name="schedule" size="lg" color="grey-4" />
        <q-badge v-if="requestMetaError" color="red">Error loading timestamps<q-btn color="grey" @click="requestRefreshMeta">refresh</q-btn></q-badge>
        <div v-if="years.length === 1">{{years[0]}}</div>
        <q-select v-else-if="years.length > 1" 
          outlined 
          v-model="selectedYear" 
          :options="years" 
          :label="selectedYear ? 'Year' : '(All years)'" 
          class="col" 
          dense 
          clearable 
          :loading="requestMetaLoading"
        >
        </q-select>
        <q-select 
          outlined 
          v-model="selectedTimestamp" 
          :options="filteredTimestamps" 
          :label="selectedTimestamp ? 'Date' : '(All dates)'" 
          class="col" 
          dense 
          clearable 
          use-input 
          @filter="timestampfilterFn" 
          input-debounce="0" 
          :loading="requestMetaLoading"
        >
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps" dense>
              <q-item-section>
                <q-item-label v-if="years.length === 1 || selectedYear"><b>{{scope.opt.month}}</b>-{{scope.opt.day}}</q-item-label>
                <q-item-label v-else><b>{{scope.opt.year}}</b>-{{scope.opt.month}}-<i>{{scope.opt.day}}</i></q-item-label>
              </q-item-section>
            </q-item>              
          </template>
          <template v-slot:selected>
            <span v-if="selectedTimestamp && (years.length === 1 || selectedYear)">{{selectedTimestamp.month}}-{{selectedTimestamp.day}}</span>
            <span v-else-if="selectedTimestamp">{{selectedTimestamp.year}}-{{selectedTimestamp.month}}-{{selectedTimestamp.day}}</span>
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
        <q-btn :disabled="!hasPrevPageSamples" @click="onPrevPage" icon="navigate_before" no-caps push>Prev page</q-btn>
        {{samplesOffset}}
        <q-btn :disabled="!hasNextPageSamples"  @click="onNextPage" icon-right="navigate_next" no-caps push>Next page</q-btn>
        
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

import AudioInventory from 'components/inventory';

export default defineComponent({
  name: 'audio-browser',

  components: {
    AudioInventory,
  },  

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
      dialoghelpShow: false,
      dialoghelpMaximizedToggle: false,
      dialogMaximizedToggle: false,
      selectedYear: undefined,
    };
  },
  computed: {
    ...mapState({
      samples_table_count: state => state.project.samples_table_count,
    }),
    refreshConfirmNeeded() {
      const count = this.samples_table_count;
      return count === undefined || count > 10000;
    },     
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
    timestampsUnfiltered() {
      if(this.selectedLocation && this.timestamps_of_location && this.timestamps_of_location.location === this.selectedLocation.value) {
        const timestamps = this.timestamps_of_location.timestamps;
        if(timestamps.length === 0) {
          return [{date: '(no timestamps)', value: undefined, year: undefined}];
        }
        return timestamps.map(t => {
          return t.timestamp <= 0 ? {date: '(unknown)', value: 0, year: '(unknown)', month: '(unknown)', day: '(unknown)'} : {date: t.date, time: t.time, value: t.timestamp, year: t.year, month: t.month<10 ? '0'+t.month : ''+t.month, day: t.day<10 ? '0'+t.day : ''+t.day};
        });
      } else {
        const d = this.$store.state.project.data;
        if(d === undefined || d.dates === undefined || d.dates.length === 0) {
          return [{date: '(no timestamps)', value: undefined}];
        }
        return d.dates.map(t => {
          return t.timestamp <= 0 ? {date: '(unknown)', value: 0, year: '(unknown)', month: '(unknown)', day: '(unknown)'} : {date: t.date, value: t.timestamp, year: t.year, month: t.month<10 ? '0'+t.month : ''+t.month, day: t.day<10 ? '0'+t.day : ''+t.day};
        });
      }
    },
    timestamps() {
      if(!this.selectedYear) {
        return this.timestampsUnfiltered;
      }
      return this.timestampsUnfiltered.filter(e => e.year === this.selectedYear);
    },
    years() {
      const coll = new Set();
      this.timestampsUnfiltered.forEach(e => coll.add(e.year));
      return [...coll];
    }    
  },
  watch: {
    refreshRequested() {
      if(this.refreshRequested) {
        if(
          this.refreshConfirmNeeded
          && !this.refreshConfirmed
          && !this.selectedLocation 
          && !this.selectedTimestamp
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
    refreshConfirmNeeded() {
      if(!this.refreshConfirmNeeded) {
        if(this.refreshConfirmRequested) {
          this.refreshConfirmRequested = false;
          this.refreshConfirmed = true;
          this.requestRefresh();
        }
      }
    },
    refreshRequestedMeta() {
      if(this.refreshRequestedMeta) {
        this.$nextTick(() => this.queryMeta());
        this.refreshRequestedMeta = false;
      }
    },
    selectedLocation() {
      this.samplesOffset = 0;
      this.requestRefresh();
      this.requestRefreshMeta();
    },
    selectedTimestamp() {
      this.samplesOffset = 0;
      this.requestRefresh();
    },
    selectedYear() {
      this.selectedTimestamp = undefined;
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
      //console.log("querySamples");
      try {        
        let params = { samples: true, count: true, limit: this.samplesLimit, offset: this.samplesOffset,};
        if(this.selectedLocation) {
          params.location = this.selectedLocation.value;
        }
        if(this.selectedTimestamp) {
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

</style>
