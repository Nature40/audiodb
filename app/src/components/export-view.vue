<template>
<v-app>
  <v-toolbar app>
    <v-menu offset-y>
      <template v-slot:activator="{ on }">
        <v-toolbar-side-icon  v-on="on"></v-toolbar-side-icon>        
      </template>
      <v-list>
        <v-list-tile>
          <v-list-tile-title><b>Navigate</b></v-list-tile-title>
        </v-list-tile>
        <v-list-tile>
          <v-list-tile-title><a href="#/audio"><v-icon>arrow_forward</v-icon>audio</a></v-list-tile-title>
        </v-list-tile>      
        <v-list-tile>
          <v-list-tile-title><a href="#/review"><v-icon>arrow_forward</v-icon>review lists</a></v-list-tile-title>
        </v-list-tile>
        <v-list-tile>
        <v-list-tile-title><a href="#/labeling"><v-icon>arrow_forward</v-icon>labeling lists</a></v-list-tile-title>
        </v-list-tile>                   
      </v-list>
    </v-menu> 
    <v-toolbar-title class="headline text-uppercase">
      Export
    </v-toolbar-title> 
    <div style="display: flex; position: absolute; right: 2px;" >
      <identity-dialog />
    </div>
  </v-toolbar>

  <v-content>
    <h2>Metrics export</h2>
    Click on below items to select metrics for audio processing.
    <div style="width: 200px;">
      <div style="display: flex; justify-content: flex-end; align-items: center;">
          <span style="font-size: 0.7em;">
            {{selectedMetricsCount}} / {{metrics.length}}
          </span>
      </div>      
      <div v-for="metric in metrics" :key="metric.name" :class="{'metric-selected': metric.selected, 'metric-unselected': !metric.selected}" @click="metric.selected = metric.selected === undefined ? true : !metric.selected">
        {{metric.name}}
      </div>
      <div style="display: flex; justify-content: flex-end; align-items: center;">
        <span class="small-button" @click="onMetricsAll"><v-icon>select_all</v-icon> all</span>
        <span class="small-button" @click="onMetricsClear"><v-icon>remove</v-icon> clear</span>
      </div>
    </div>
    <br>
    <v-btn @click="getData" :disabled="selectedMetricsCount === 0 || process_request_busy">Process</v-btn>
    <pulse-loader :loading="process_request_busy" color="#000000" size="10px" />
    <div v-if="process_request_last === 'done'">
      <a v-if="dataUrl !== undefined" :href="dataUrl" download="data.csv">download CSV</a>
      <v-data-table
        :headers="headers"
        :items="table"
        hide-actions
        v-if="table !== undefined"
      >
        <template v-slot:items="props">
          <tr>
            <td v-for="(column, index) in columns" :key="column" :class="columnPresenters[index].class">
              {{columnPresenters[index].toText(props.item[column])}}
            </td> 
            <!--<td>{{ props.item.sample }}</td>      
            <td style="text-align: right;">{{ parseFloat(props.item.start).toFixed(3) }}</td> 
            <td style="text-align: right;">{{ parseFloat(props.item.end).toFixed(3) }}</td> 
            <td>{{ props.item.generated_label }}</td> 
            <td>{{ props.item.label }}</td> 
            <td>{{ props.item.comment }}</td>-->
          </tr> 
        </template>
      </v-data-table>
      <div v-if="data === undefined">
        !! no data loaded !!
      </div>
    </div>
    <div v-if="process_request_last === 'error'">
      Processing resulted in an error.
    </div>

    <hr>
    <div>
      <br>
      <h2>Timeseries export</h2>
      <a :href="this.apiBase + 'timeseries'" download="timeseries.csv">export download</a>
    </div>

  </v-content>


</v-app>
</template>

<script>

import axios from 'axios'
import parse from 'csv-parse/lib/sync'
import { mapState, mapGetters, mapActions } from 'vuex'

import identityDialog from './identity-dialog'

export default {
name: 'export-view',
components: {
  identityDialog,
},
data () {
  return {    
    data: undefined,
    dataUrl: undefined,
    metrics: [],    
    process_request_busy: false,
    process_request_last: undefined,
  }
},
computed: {
  ...mapState({
    apiBase: 'apiBase',
  }),
  ...mapGetters({
  }),
  queryUrl() {
    return this.apiBase + 'query';
  },
  table() {
    if(this.data === undefined) {
      return undefined;
    }
    return parse(this.data, {
      columns: true,
      skip_empty_lines: true
    });
  },
  columns() {
    if(this.data === undefined) {
      return [];
    }
    return this.data.substring(0, this.data.indexOf('\n')).split(',');
  },
  headers() {
    return this.columns.map(name => {
      return { text: name, value: name, align: "center" };
    });
  },
  columnPresenters() {
    return this.columns.map(name => {
      var o = {};
      switch(name) {
        case 'sample':
        case 'generated_label':
        case 'label':
          o.toText = v => v;
          break;
        case 'start':
        case 'end':
          o.toText = v => parseFloat(v).toFixed(1);
          o.class = 'number-fixed';
          break;
        default:
          o.toText = v => parseFloat(v).toFixed(2);
          o.class = 'number-fixed';
      }
      return o;
    }); 
  },
  selectedMetricsCount() {
    return this.metrics.reduce((cnt, metric) => metric.selected ? (cnt + 1) : cnt, 0);
  }
},
watch: {
  data() {
    if(this.dataUrl !== undefined) {
      URL.revokeObjectURL(this.dataUrl);
      this.dataUrl = undefined;
    }
    var blob = new Blob([this.data], {type : 'text/csv'});
    this.dataUrl = URL.createObjectURL(blob);
  }
},
methods: {
  ...mapActions({
  }),
  async getData() {
    let selectedMetrics = this.metrics.filter(metric => metric.selected);
    try {
      this.process_request_busy = true;
      this.data = (await axios.post(this.queryUrl, {metrics: selectedMetrics})).data;
      this.process_request_last = 'done';
    } catch (error) {
      this.process_request_last = 'error';
      console.log(error.response);
    }
    this.process_request_busy = false;
  },
  async refreshMetrics() {
    let response = await fetch(this.queryUrl + '/metrics');
    this.metrics = (await response.json()).metrics.map(m => {m.selected = false; return m;});
  },
  onMetricsAll() {
    this.metrics.forEach(metric => metric.selected = true);
  },
  onMetricsClear() {
    this.metrics.forEach(metric => metric.selected = false);
  },  
},
mounted() {
  this.refreshMetrics();
},
}
</script>

<style scoped>

.number-fixed {
  text-align: right;
}

.metric-unselected {
  background-color: #2d2d2d00;
  border-style: solid;
  border-width: 1px;
  border-radius: 10px;
  padding: 4px;
  margin: 3px;
  border-color: #0000007d;
}

.metric-selected {
  background-color: #0c356045;
  border-style: solid;
  border-width: 1px;
  border-radius: 10px;
  padding: 4px;
  margin: 3px;
  color: #182675;
}

.small-button {
  display: flex; 
  align-items: center;
  box-shadow:inset 0px 1px 0px 0px #ffffff;
	background:linear-gradient(to bottom, #ededed 5%, #dfdfdf 100%);
	background-color:#ededed;
	border-radius:6px;
	border:1px solid #dcdcdc;
	cursor:pointer;
	color:#777777;
  padding: 2px 10px 2px 10px;
  text-decoration: none;
  text-shadow: 0px 1px 0px #ffffff;
  margin: 2px;
}
.small-button:hover {
	background:linear-gradient(to bottom, #dfdfdf 5%, #ededed 100%);
	background-color:#dfdfdf;
}
.small-button:active {
	position:relative;
	top:1px;
}


</style>
