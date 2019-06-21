<template>
<v-dialog v-model="dialog" fullscreen hide-overlay transition="dialog-bottom-transition">
      <template v-slot:activator="{on}">
        <v-btn v-on="on" color="grey"><v-icon>description</v-icon> Label Definitions</v-btn>
      </template>
      <v-card>
        <v-toolbar dark color="primary">
          <v-btn icon dark @click="dialog = false">
            <v-icon>close</v-icon>
          </v-btn>
          <v-toolbar-title>Label Definitions</v-toolbar-title>
          <v-spacer></v-spacer>
          <v-toolbar-items>
            <v-btn dark flat @click="save" v-if="user_label_definitions !== undefined">Save</v-btn>
          </v-toolbar-items>
        </v-toolbar>
        <div v-if="savePending">
          saving...
        </div>
        <div v-if="saveError !== undefined" style="color: red;">
          ERROR saving: {{saveError}}
        </div>
        <div v-if="user_label_definitions !== undefined">
          Changes are not saved until button <b>save</b> is pressed. Button <b>x</b> or <b>Esc</b>-key discards changes.
          <br>
          <label-definitions-add @addLabelDefinition="addLabelDefinition($event)" :user_label_definitions="user_label_definitions"/>
          <br>
          <v-data-table
            :headers="headers"
            :items="user_label_definitions"
            item-key="name"
            hide-actions
            class="table-label-definitions"            
          >
            <template v-slot:no-data>
              <v-alert :value="true" color="error" icon="warning">
                no label definitions
              </v-alert>
            </template>
            <template v-slot:items="props">
              <td><v-btn icon title="remove label"><v-icon @click="removeLabel" :index="props.item.original_index">delete_forever</v-icon></v-btn></td>
              <td>{{props.item.name}}</td>
              <td>
                <v-edit-dialog
                  :return-value.sync="props.item.desc"
                  lazy
                > 
                  {{ props.item.desc }}
                  <template v-slot:input>
                    <v-text-field
                      v-model="props.item.desc"
                      label="Edit"
                      single-line
                    ></v-text-field>
                  </template>
                </v-edit-dialog>
              </td>
            </template>
          </v-data-table>
        </div>
        <div v-if="label_definitionsLoading">
          loading...
        </div>
        <div v-if="label_definitionsError" style="color: red;">
          ERROR: {{label_definitionsError}}
        </div>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios'
import { mapState, mapActions } from 'vuex'

import labelDefinitionsAdd from './label-definitions-add'

export default {
props: [],
components: {
  labelDefinitionsAdd
},
data: () => ({
  dialog: false,
  savePending: false,
  saveError: undefined,
  user_label_definitions: undefined,
  headers: [
    {text: ' ', sortable: false, class: "table-head", width: "10px"},
    {text: 'Name', value: 'name', width: "300px", class: "table-head"},
    {text: 'Description', value: 'desc', sortable: false, class: "table-head"},
  ],
}),
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    label_definitions: state => state.label_definitions === undefined ? undefined : state.label_definitions.data,
    label_definitionsLoading: state => state.label_definitions.loading,
    label_definitionsError: state => state.label_definitions.error,
  }),
},  
methods: {
  ...mapActions({
    label_definitions_init: 'label_definitions/init',
    label_definitions_refresh: 'label_definitions/refresh',
  }),
  save() {
    var self = this;
    self.savePending = true;
    self.saveError = undefined;
    axios.post(self.apiBase + 'label_definitions', {label_definitions: self.user_label_definitions})
    .then(function() {
      self.savePending = false;
      self.dialog = false;
      self.label_definitions_refresh();
    })
    .catch(function() {
       self.savePending = false;
       self.saveError = "unknown";
    });
  },
  addLabelDefinition(label_definition) {
    this.user_label_definitions.push(label_definition);
  },
  removeLabel(event) {
    var index = event.target.getAttribute('index');
    this.user_label_definitions.splice(index, 1);
    this.user_label_definitions.forEach((e, i) => e.original_index = i);  
  },
  create_user_label_definitions() {
    var x = JSON.parse(JSON.stringify(this.label_definitions));
    x.forEach((e, i) => e.original_index = i);      
    return x;
  }
},
watch: {
  label_definitions() {
    this.user_label_definitions = this.create_user_label_definitions();
  },
  dialog() {
      this.user_label_definitions = this.create_user_label_definitions();
      this.$emit('onDialog', this.dialog);
  },
},
mounted() {
  this.label_definitions_init();
},    
}
</script>

<style scoped>

.table-label-definitions {
  padding: 3px;
  margin: 10px;
  border-style: solid;
  border-width: 1px;
  border-color: #a5957bd9;
  border-radius: 6px;
  box-shadow: 5px 5px 7px #443c3145;
}

.table-label-definitions tbody tr:nth-child(odd) td {
  background-color: #ececec50;
}

.table-label-definitions tbody tr:nth-child(even) td {
  background-color: #dbdbdb50;
}

</style>


