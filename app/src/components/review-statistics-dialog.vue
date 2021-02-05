<template>



<div style="width: 200px;">
<v-dialog v-model="dialog" fullscreen hide-overlay transition="dialog-bottom-transition">
      <template v-slot:activator="{ on }">
        <v-btn v-on="on" outline>statistics</v-btn>
      </template>
      <v-card>
        <v-toolbar dark color="primary">
          <v-btn icon dark @click="dialog = false">
            <v-icon>keyboard_backspace</v-icon>
          </v-btn>
          <v-toolbar-title>Review Statistics</v-toolbar-title>
          <v-spacer></v-spacer>
          <v-toolbar-items>
            <v-btn dark flat @click="dialog = false"><v-icon>close</v-icon></v-btn>
          </v-toolbar-items>
        </v-toolbar>
        <v-list three-line subheader v-if="request_message !== undefined">
          {{request_message}}
        </v-list>

        <br><br><b>Reviews per reviewer</b>
        <v-data-table
            :headers="reviewerheaders"
            :items="stats.reviewer_stats"
            class="elevation-1"
            v-if="request_message === undefined && stats !== undefined"
            hide-actions
            style="width: 400px;"
          >
          <template v-slot:items="props">
            <td>{{props.item.name}}</td>
            <td class="text-xs-right">{{props.item.count}}</td>
          </template>
        </v-data-table>

        <br><br><b>Review statistics per species</b>
        <v-data-table
            :headers="headers"
            :items="stats.name_stats"
            class="elevation-1"
            v-if="request_message === undefined && stats !== undefined"
            hide-actions
          >
          <template v-slot:items="props">
            <td>{{props.item.name}}</td>
            <td class="text-xs-right">{{props.item.total}}</td>
            <td class="text-xs-right">{{props.item.no}}</td>
            <td class="text-xs-right">{{props.item.unsure}}</td>
            <td class="text-xs-right">{{props.item.yes}}</td>
            <td class="text-xs-right">{{props.item.nopp}}</td>
            <td class="text-xs-right">{{props.item.unsurepp}}</td>
            <td class="text-xs-right">{{props.item.yespp}}</td>
          </template>
        </v-data-table>

        <br><br><b>Review statistics per species and model</b>
        <br> Columns definition:
        <br> 1N: Number of samples from Model 1 birdsong_classification
        <br> 1+: Correct % samples from Model 1 birdsong_classification
        <br> 1-: False positive % samples from Model 1 birdsong_classification
        <br> 2N: Number of samples from Model 2 birdsong_classification_ds
        <br> 2+: Correct % samples from Model 2 birdsong_classification_ds
        <br> 2-: False positive % samples from Model 2 birdsong_classification_ds
        <br> 3N: Number of samples from Model 3 birdNET
        <br> 3+: Correct % samples from Model 3 birdNET
        <br> 3-: False positive % samples from Model 3 birdNET
        <br><br>
        <i>The filter threshold of model reliability. Setting this to less than review_list threshold (currently 0.8) produces incorrect statistics because of missing review data.</i>
        <v-text-field
            v-model="threshold"
            label="Threshold"
            placeholder="Placeholder"
            style="width: 100px;"
          ></v-text-field>
          
        <br>              
        <v-data-table
            :headers="Modelheaders"
            :items="stats.name_stats"
            class="elevation-1"
            v-if="request_message === undefined && stats !== undefined"
            hide-actions
          >
          <template v-slot:items="props">
            <td>{{props.item.name}}</td>
            <td class="text-xs-right">{{props.item.total_birdsong_classification}}</td>
            <td class="text-xs-right">{{props.item.yespp_birdsong_classification}}</td>            
            <td class="text-xs-right">{{props.item.nopp_birdsong_classification}}</td> 
            <td class="text-xs-right">{{props.item.total_birdsong_classification_ds}}</td>
            <td class="text-xs-right">{{props.item.yespp_birdsong_classification_ds}}</td>            
            <td class="text-xs-right">{{props.item.nopp_birdsong_classification_ds}}</td>
            <td class="text-xs-right">{{props.item.total_birdNET}}</td>
            <td class="text-xs-right">{{props.item.yespp_birdNET}}</td>            
            <td class="text-xs-right">{{props.item.nopp_birdNET}}</td>                                                       
          </template>
        </v-data-table>

      </v-card>
    </v-dialog>
</div>

</template>

<script>

import { mapState, mapActions } from 'vuex'
import axios from 'axios'

export default {
name: 'review-statistics-dialog',
components: {
},
data () {
  return {
    dialog: false,
    stats: undefined,
    request_message: undefined,
    threshold: '0.8',

    reviewerheaders: [
      {
        text: 'Name',
        align: 'center', 
        sortable: true,
        value: 'name'
      },
      { text: 'count', value: 'count', align: 'center', },
    ],

    headers: [
      {
        text: 'Species',
        align: 'center', 
        sortable: true,
        value: 'name'
      },
      { text: 'N', value: 'total', align: 'center', },
      { text: 'no', value: 'no', align: 'center', },
      { text: 'unsure', value: 'unsure', align: 'center', },
      { text: 'yes', value: 'yes', align: 'center', },
      { text: 'no%', value: 'nopp', align: 'center', },
      { text: 'unsure%', value: 'unsurepp', align: 'center', },
      { text: 'yes%', value: 'yespp', align: 'center', }
    ],  
    
    Modelheaders: [
      {
        text: 'Species',
        align: 'center', 
        sortable: true,
        value: 'name'
      },
      { text: '1N', value: 'total_birdsong_classification', align: 'center', },
      { text: '1+%', value: 'yespp_birdsong_classification', align: 'center', },
      { text: '1-%', value: 'nopp_birdsong_classification', align: 'center', },
      { text: '2N', value: 'total_birdsong_classification_ds', align: 'center', },
      { text: '2+%', value: 'yespp_birdsong_classification_ds', align: 'center', },
      { text: '2-%', value: 'nopp_birdsong_classification_ds', align: 'center', },  
      { text: '3N', value: 'total_birdNET', align: 'center', },
      { text: '3+%', value: 'yespp_birdNET', align: 'center', },
      { text: '3-%', value: 'nopp_birdNET', align: 'center', },          
    ],      
  }
},
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
  }),
},
methods: {
  ...mapActions({
  }),
  async refresh() {
    this.request_message = 'loading statistics...';
    try {
      var response = await axios.get(this.apiBase + 'review_statistics_detailed', {params: {threshold: this.threshold}});
      this.stats = response.data;
      this.request_message = undefined;
    } catch {
      this.request_message = 'error loading statistics';
    }
  },
},
watch: {
  dialog() {
    if(this.dialog) {
      this.refresh();
    }
  },
  threshold() {
    this.refresh();
  }
},
mounted() {
  if(this.dialog) {
      this.refresh();
  }
},
}
</script>
