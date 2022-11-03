<template>
  <q-page class="fit column content-center">
    <q-toolbar class="bg-grey-3">
      <q-select rounded outlined v-model="descriptorName" :options="descriptorNames" label="Selected task" style="min-width: 200px;">
        <template v-slot:option="scope">
          <q-item v-bind="scope.itemProps">
            <q-item-section>
              <q-item-label><b>{{scope.opt}}</b> <span style="color: grey; padding-left: 20px;">{{descriptors[scope.opt].description}}</span></q-item-label>
            </q-item-section>
          </q-item>
        </template>
      </q-select>
      <span style="font-size: 2em; padding-left: 100px; color: #000000b3;">{{descriptorName}}</span>
    </q-toolbar>
    <div v-if="descriptor !== undefined" style="margin-top: 5px; margin-left: 20px;">
      <div v-if="descriptor.description !== undefined">
        {{descriptor.description}}
      </div>
      <div v-if="args.length > 0" style="margin-top: 20px;">
        <!--<hr>-->
        <span style="color: #000000d1; font-size: 1.5em;"><q-btn icon="undo" round size="xs" unelevated padding="xs" dense title="Set all parameters to preset." @click="resetParameters"></q-btn>Task parameters</span>
        <table style="margin-top: 10px;">
          <thead>
            <tr>
              <th>Parameter</th> 
              <th style="min-width: 700px;">Value</th> 
            </tr>
          </thead>
          <tbody>
            <tr v-for="arg in args" :key="arg.name" :title="arg.description">
              <td><q-btn icon="undo" round size="xs" unelevated padding="xs" dense :title="'Set to parameter preset: ' + arg.preset" @click="arg.value = arg.preset"></q-btn>{{arg.name}}</td>
              <td v-if="arg.type === 'BOOLEAN'">
                <q-checkbox v-model="arg.value" dense />
              </td>
              <td v-else-if="arg.type === 'STRING'">
                <q-input v-model="arg.value" dense outlined bg-color="white"/>
              </td>
              <td v-else>
                (unknown Type: {{arg.type}})
              </td>
            </tr>
          </tbody>
        </table>
        <!--<hr>-->
      </div>
      <q-btn @click="execute" icon="rocket_launch" style="margin-left: 200px; margin-top: 20px;" rounded title="Submit currently selected task with current parameters to execution on the server.">
        Submit task
      </q-btn>
      <div v-if="args.length > 0" style="margin-top: 20px;">
        <!--<hr>-->
        <span style="color: #000000d1; font-size: 1.5em;">Parameter description</span>
        <table style="margin-top: 10px;">
          <thead>
            <tr>
              <th>Parameter</th> 
              <th>Description</th> 
            </tr>
          </thead>
          <tbody>
            <tr v-for="arg in args" :key="arg.name">
              <td>{{arg.name}}</td>
              <td>{{arg.description}}</td>
            </tr>
          </tbody>
        </table>
        <!--<hr>-->
      </div>      
    </div>

    

    <task-console ref="TaskConsole"/>

  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';

import TaskConsole from 'components/task-console';

export default defineComponent({
  name: 'Task',

  components: {
    TaskConsole,
  },

  data() {
    return {
      descriptors: {},
      descriptorName: undefined,
      args: [],
    };
  },
  
  computed: {
    ...mapState({
      project: state => state.projectId,
    }),
    descriptorNames() {
      return Object.keys(this.descriptors);
    },
    descriptor() {
      if(this.descriptorName === undefined || this.descriptorName === null) {
        return undefined;
      }
      return this.descriptors[this.descriptorName];
    }
  },

  methods: {
    async refresh() {
      try {
        var urlPath = 'tasks';
        var response = await this.$api.get(urlPath, {params: {descriptors: true,}});
        this.descriptors = response.data.descriptors;
       } catch(e) {
        this.descriptors = {};
        console.log(e);
      }
    },
    resetParameters() {
      this.args.forEach(arg => {
        arg.value = arg.preset;
      });
    },
    execute() {
      var task = { task: this.descriptor.name};
      if(this.args.length > 0) {
        task.params = this.args.map(arg => {
          var param = {param: arg.name};
          if(arg.type === 'BOOLEAN') {
            if(arg.value) {
              param.value = 'TRUE';
            } else {
              param.value = 'FALSE';
            }
          } else if(arg.type === 'STRING') {
            param.value = arg.value;           
          } else {
            console.log('unknown parameter type' + arg.type + ' of ' + this.descriptorName + ' -> ' + arg.name + ' : ' + arg.value);
          }          
          return param;
        });
      }
      this.$refs.TaskConsole.submit(task);
    },
  },

  watch: {
    descriptors() {
      if(this.descriptors.length == 0) {
        return;
      }
      if(this.descriptor === undefined) {
        this.descriptorName = this.descriptorNames[0];
      }
    },
    descriptor() {
      if(this.descriptor === undefined || this.descriptor.params === undefined) {
        this.args = [];
      } else {
        this.args = this.descriptor.params.map(param => {
          var arg = {
            name: param.name, 
            type: param.type,
            description: param.description, 
          };          
          if(arg.type === 'BOOLEAN') {
            if(param.preset === 'TRUE') {
              arg.preset = true;
            } else if(param.preset === 'FALSE') {
              arg.preset = false;
            } else {
              console.log('unknown BOOLEAN preset of ' + this.descriptorName + ' -> ' + param.name + ' : ' + param.preset);
              arg.preset = false;
            }
          } else if(arg.type === 'STRING') {
            arg.preset = param.preset;           
          } else {
            console.log('unknown parameter type' + arg.type + ' of ' + this.descriptorName + ' -> ' + param.name + ' : ' + param.preset);
          }
          arg.value = arg.preset;
          return arg;
        });
      }
    }
  },
  async mounted() {
    this.refresh();
   },  
})
</script>

<style scoped>

table {
  border-collapse: collapse;
  /*border: 2px solid;
  border-color: #0000003d;*/
}
thead tr{ 
  background-color: #0000000f;
  border-bottom: solid;
  border-width: 1px 0;
  border-color: #00000080;
}
tbody tr { 
  border: solid;
  border-width: 1px 0;
  border-color: #0000001c;
  background-color: #00000003;
}
tbody tr:nth-child(even) { 
  background-color: #00000007;
}
tbody tr:first-child {
  border-top: none;
}
tbody tr:last-child {
  border-bottom: none;
}
tbody tr:hover {
  background-color: #00000020;
}

</style>
