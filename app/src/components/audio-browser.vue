<template>
<v-dialog v-model="dialog" fullscreen>
      <template v-slot:activator="{on}">
        <v-btn v-on="on"><v-icon>storage</v-icon> Browse</v-btn>
      </template>
      <div class="grid-container">
        <v-toolbar>
          <v-btn icon @click="dialog = false">
            <v-icon>close</v-icon>
          </v-btn>
          <v-toolbar-title>Browse Audio Samples</v-toolbar-title>
          <v-spacer></v-spacer>
          <clip-loader :loading="samplesLoading" color="#000000" size="20px" /> <span v-show="samplesLoading"> Loading audio samples</span>
          <span v-show="samplesIsError">{{samplesError}}</span>
          <v-btn @click="refresh()" icon v-show="!samplesLoading"><v-icon>refresh</v-icon></v-btn>
          <v-spacer></v-spacer>
          <v-toolbar-items>
            <v-btn flat @click="dialog = false">Close</v-btn>
          </v-toolbar-items>
        </v-toolbar>
        <div class="innergrid-container">
          <div dense class="innergrid-item-nav">
            
          </div>
          <div class="innergrid-item-main">
            <!--<div style="display: flex; justify-content: center;">-->
  
            <div><b>Click on a table row to select that audio sample.</b></div>
            <div>
              <table class="table-meta">
              <thead>
                <th>Location</th>
                <th>Date</th>
                <th>Time</th>
              </thead>
              <tbody>
                <tr v-for="(sample, index) in samples" :key="sample.id" @click="onClickSample(sample, index)"  :class="{'selected-sample': (selectedSample !== undefined && selectedSample.id == sample.id)}">
                  <td><b>{{sample.location}}</b></td> 
                  <td>{{toDate(sample.datetime)}}</td>
                  <td>{{toTime(sample.datetime)}}</td>
                </tr>
              </tbody>
            </table>
            </div>
         
          </div>
        </div>
        <div class="status">
          {{samples.length === 0 ? 'no samples' : samples.length + ' samples'}}
        </div>
      </div>
    </v-dialog>
</template>

<script>

import { mapState, mapGetters, mapActions } from 'vuex'

const yearFormat = new Intl.DateTimeFormat('en', { year: 'numeric' });
const monthFormat = new Intl.DateTimeFormat('en', { month: '2-digit' });
const dayFormat = new Intl.DateTimeFormat('en', { day: '2-digit' });
const hourFormat = new Intl.DateTimeFormat('en', { hour: '2-digit', hour12: false });
//const minuteFormat = new Intl.DateTimeFormat('en', { minute: '2-digit' }); // no leading zero
//const secondFormat = new Intl.DateTimeFormat('en', { second: '2-digit' }); // no leading zero

export default {
props: ['selected-sample'],
components: {
},
data: () => ({
  dialog: false,
}),
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    samples: state => state.samples.data,
    samplesLoading: state => state.samples.loading,
    samplesError: state => state.samples.error,
  }),
  ...mapGetters({
      samplesIsError: 'samples/isError',
  })  
},  
methods: {
  ...mapActions({
    samplesQuery: 'samples/query',
  }),
  toDate(date) {
    const year = yearFormat.format(date);
    const month = monthFormat.format(date);
    const day = dayFormat.format(date);
    return `${year}-${month}-${day}`;
  },
  toTime(date) {
    const hour = hourFormat.format(date);
    const minute = date.getMinutes().toString().padStart(2,'0');
    const second = date.getSeconds().toString().padStart(2,'0');
    return `${hour}:${minute}:${second}`;
  },
  onClickSample(sample, index) {
    console.log(index);
    console.log(sample);
    this.$emit('select-sample', sample);
    this.dialog = false;
  },
  refresh() {
    this.samplesQuery();
  }  
},
watch: {
  dialog() {
  },
},
mounted() {
  this.refresh();  
},    
}
</script>

<style scoped>

.grid-container {
  height: 100vh;
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: max-content minmax(100px, 1fr) max-content;
  background-color: white;
}

.innergrid-container {
  display: grid;
  grid-template-columns: max-content auto;
  grid-template-rows: auto;
}

.innergrid-item-nav {
  background-color: rgba(0, 0, 0, 0.02);
  padding-right: 0px;
  overflow-y: auto;
  width: 300px;
  border-right-color: #0000001a;
  border-right-width: 1px;
  border-right-style: solid;
}

.innergrid-item-main {
  padding: 15px;
  overflow-y: auto;

  display: flex; 
  flex-direction: column; 
  align-items: center;  
}

.status {
  background-color: #e6e6e6;
}

.table-meta {
  padding: 3px;
  margin: 10px;
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bd9;
  border-radius: 6px;
  box-shadow: 5px 5px 7px #443c3145;
}

.table-meta tbody tr td {
  padding: 3px;
}

.table-meta tbody tr:nth-child(odd) td {
  background-color: #ececec50;
}

.table-meta tbody tr:nth-child(even) td {
  background-color: #dbdbdb50;
}

.selected-sample {
  background-color: #dfdfdf;
  box-shadow: 0px 0px 0px 1px rgb(142, 137, 126);
}

</style>

<style scoped>

html {
  overflow: auto;
}

</style>


