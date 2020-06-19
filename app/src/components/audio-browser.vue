<template>
<v-dialog v-model="dialog" fullscreen hide-overlay transition="dialog-bottom-transition">
      <template v-slot:activator="{on}">
        <v-btn v-on="on"><v-icon>folder_open</v-icon> Browse</v-btn>
      </template>
      <v-card>
        <v-toolbar dark color="primary">
          <v-btn icon dark @click="dialog = false">
            <v-icon>close</v-icon>
          </v-btn>
          <v-toolbar-title>Browse Audio Files</v-toolbar-title>
          <v-spacer></v-spacer>
          <v-toolbar-items>
            <v-btn dark flat @click="dialog = false">Close</v-btn>
          </v-toolbar-items>
        </v-toolbar>
        <div>
          <br>
          <b>Click on a table row to select that audio file.</b>
          <br>
          <table class="table-meta">
            <thead>
              <th>Location</th>
              <th>Date</th>
              <th>Time</th>
            </thead>
            <tbody>
              <tr v-for="(sample, key) in samples" :key="key" @click="onClickSample(sample)">
                <td><b>{{sample.location}}</b></td> 
                <td>{{toDate(sample.datetime)}}</td>
                <td>{{toTime(sample.datetime)}}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </v-card>
    </v-dialog>
</template>

<script>

import { mapState, mapActions } from 'vuex'

const yearFormat = new Intl.DateTimeFormat('en', { year: 'numeric' });
const monthFormat = new Intl.DateTimeFormat('en', { month: '2-digit' });
const dayFormat = new Intl.DateTimeFormat('en', { day: '2-digit' });
const hourFormat = new Intl.DateTimeFormat('en', { hour: '2-digit', hour12: false });
//const minuteFormat = new Intl.DateTimeFormat('en', { minute: '2-digit' }); // no leading zero
//const secondFormat = new Intl.DateTimeFormat('en', { second: '2-digit' }); // no leading zero

export default {
props: ['samples'],
components: {
},
data: () => ({
  dialog: false,
}),
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
  }),
},  
methods: {
  ...mapActions({

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
  onClickSample(sample) {
    console.log(sample);
    this.$emit('select-sample', sample);
    this.dialog = false;
  }  
},
watch: {
  dialog() {
  },
},
mounted() {
},    
}
</script>

<style scoped>

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

</style>


