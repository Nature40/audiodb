<template>
<v-dialog v-model="dialog" fullscreen hide-overlay transition="dialog-bottom-transition">
      <template v-slot:activator="{on}">
        <v-btn v-on="on"><v-icon>post_add</v-icon> Metadata</v-btn>
      </template>
      <v-card>
        <v-toolbar dark color="primary">
          <v-btn icon dark @click="dialog = false">
            <v-icon>close</v-icon>
          </v-btn>
          <v-toolbar-title>Audio File Metadata</v-toolbar-title>
          <v-spacer></v-spacer>
          <v-toolbar-items>
            <v-btn dark flat @click="dialog = false">Close</v-btn>
          </v-toolbar-items>
        </v-toolbar>
        <div>
          <table class="table-meta">
            <tbody>
              <tr v-for="(value, key) in sampleMeta" :key="key">
                <td><b>{{key}}</b></td> 
                <td>{{value}}</td>
              </tr>
            </tbody>
          </table>

          <table class="table-meta">
            <tbody>
              <tr v-for="(value, key) in meta" :key="key">
                <td><b>{{key}}</b></td> 
                <td>{{value}}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios'
import YAML from 'yaml'
import { mapState, mapActions } from 'vuex'

export default {
props: ['sample'],
components: {
},
data: () => ({
  dialog: false,
  sampleMeta: undefined,
  meta: undefined,
}),
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
  }),
},  
methods: {
  ...mapActions({

  }),
  refeshMeta() {
    this.meta = undefined;
    if(this.dialog) {
      axios.get(this.apiBase + 'samples' + '/' + this.sample.id + '/' + 'meta')
      .then(response => {
        var data = response.data;
        var parsed = YAML.parse(data);
        this.sampleMeta = parsed.sample;
        this.meta = parsed.meta;
      })
      .catch(() => {
        this.meta = undefined;
      });
    }
  }
},
watch: {
  sample() {
    this.refeshMeta();
  },
  dialog() {
    this.refeshMeta();
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


