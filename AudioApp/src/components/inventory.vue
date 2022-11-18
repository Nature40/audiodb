<template>
  <q-dialog v-model="show" :maximized="dialogMaximizedToggle">
    <q-layout view="Lhh lpR fff" container class="bg-white" style="min-width: 1000px;">
      <q-header class="bg-white text-black">                
      <q-bar>
        <q-icon name="view_timeline"/>
        <div>Audio devices inventory</div>
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

            <q-card-section class="q-pt-none">
              <div class="text-h6">View audio devices inventory</div>              
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
      
      </q-header>

      <q-page-container>

      <q-card-section>
        
        <q-table 
          :rows="inventory"
          :columns="columns"
          dense
          :rows-per-page-options="[0]"
          :filter="filterValue"
          :pagination="tabelPagination"
        >
          <template v-slot:top-right>
            <q-input dense debounce="300" v-model="filterValue" placeholder="Search" rounded outlined>
            <template v-slot:append>
              <q-icon name="search" />
            </template>
            </q-input>
          </template>
          <template v-slot:body-cell-device="props">
            <q-td key="device" :class="{'existing-device': deviceSet.has(props.row.device)}">
              {{props.row.device}}
            </q-td>
          </template>                            
        </q-table>
        
      </q-card-section>

      </q-page-container>

      <q-footer class="bg-white text-black">
        <q-separator/>
        <q-toolbar>
        <q-space />

        
        <q-space />
        </q-toolbar>
        <q-separator/>

      </q-footer>

    </q-layout>
    <!--</q-card>-->
  </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'audio-inventory',
  setup () {
    const show = ref(false);
    return {
      show,      
    };
  },
  data() {
    return {
      dialoghelpShow: false,
      dialoghelpMaximizedToggle: false,
      dialogMaximizedToggle: false,
      inventoryFile: [],
      columns: [
        {name: 'location', field: 'location', label: 'Location', sortable: true, align: 'left', },        
        {name: 'device', field: 'device', label: 'Device', sortable: true, align: 'left', },
        {name: 'start', field: row => row.start.date + ' ' + row.start.time, label: 'Start', sortable: true, align: 'left', },
        {name: 'end', field: row => row.end.date + ' ' + row.end.time, label: 'End', sortable: true, align: 'left', },
      ],
      filterValue: undefined,
      tabelPagination: {
        sortBy: 'location',
      },
    };
  },
  computed: {
    ...mapState({
      devices: state => state.project.data?.devices,
    }),
    deviceSet() {
      if(this.devices !== undefined && this.devices !== null && this.devices.length > 0) {
        return new Set(this.devices);
      } else {
        return new Set();
      }
    },
    inventoryFileDeviceSet() {
      const ds = this.inventoryFile.map(e => e.device);
      return new Set(ds);
    },
    inventoryFileMissingDevices() {
      return [...this.deviceSet].filter(e => !this.inventoryFileDeviceSet.has(e));
    },
    inventory() {
      var a = [...this.inventoryFile];
      this.inventoryFileMissingDevices.forEach(e => {
        a.push({device: e, location: '(not in inventory)', start: {date: '', time: ''}, end: {date: '', time: ''}});
      });
      return a;
    }           
  },
  watch: {    
  },
  methods: {
    async refresh() {
      try {
        var params = {inventory: true};
        var response = await this.$api.get('projects/' + this.$store.state.projectId, {params});
        this.inventoryFile = response.data.project.inventory;
      } catch(e) {
        console.log(e);
      }      
    },
  },
  mounted() {
    this.refresh();
  },
});
</script>

<style scoped>

.existing-device {
  font-weight: bold;
}

</style>
