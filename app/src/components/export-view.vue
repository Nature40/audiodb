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
      </v-list>
    </v-menu> 
    <v-toolbar-title class="headline text-uppercase">
      Export
    </v-toolbar-title> 
    <identity-dialog></identity-dialog>
  </v-toolbar>

  <v-content>
    <v-btn @click="getData">query</v-btn>
    <a v-if="dataUrl !== undefined" :href="dataUrl" download="data.csv">download CSV</a>
    <v-data-table
      :headers="headers"
      :items="table"
      hide-actions
      v-if="table !== undefined"
    >
      <template v-slot:items="props">
        <td>{{ props.item.sample }}</td>      
        <td style="text-align: right;">{{ parseFloat(props.item.start).toFixed(3) }}</td> 
        <td style="text-align: right;">{{ parseFloat(props.item.end).toFixed(3) }}</td> 
        <td>{{ props.item.label }}</td> 
        <td>{{ props.item.comment }}</td> 
      </template>
    </v-data-table>
    <div v-if="data === undefined">
      !! no data loaded !!
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
    headers: [{ text: 'sample', value: 'sample', align: "center" },
    { text: 'start', value: 'start', align: "center" },
    { text: 'end', value: 'end', align: "center" },
    { text: 'generated_label', value: 'label', align: "center" },
    { text: 'label', value: 'label', align: "center" },
    { text: 'comment', value: 'comment', align: "center" },
    ],
    data: undefined,
    dataUrl: undefined,    
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
  getData() {
    axios.get(this.queryUrl)
    .then((response) => {
      this.data = response.data;
    })
    .catch((error) => {
      console.log(error.response);
    });
  },
},
mounted() {
},
}
</script>
