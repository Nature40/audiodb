<template>
<v-dialog v-model="dialog" width="500">
      <template v-slot:activator="{ on }">
        <v-btn  v-on="on">
          add new label
        </v-btn>
      </template>

      <v-card>
        <v-card-title class="headline grey lighten-2" primary-title>
          add new label
        </v-card-title>

        <v-card-text>
          <v-text-field v-model="labelName" label="Label Name" placeholder="empty"></v-text-field>
          <v-text-field v-model="labelDescription" label="Description" placeholder="optional"></v-text-field>
        </v-card-text>
        <span v-show="existing" style="color: red;">Name already exists</span>
        <v-divider></v-divider>

        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" flat @click="dialog = false; $emit('addLabelDefinition', {name: labelName, desc: labelDescription})" :disabled="!labelName || !labelName.trim() || existing">
            add
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import { mapState } from 'vuex'

export default {
props: ['user_label_definitions'],
data: () => ({
  dialog: false,
  labelName: undefined,
  labelDescription: undefined,
}),
computed: {
  ...mapState({
    apiBase: 'apiBase',
  }),
  existing() {
    if(!this.labelName || !this.labelName.trim() || !this.user_label_definitions) {
      return false;
    }
    return this.user_label_definitions.some(label_definition => this.labelName === label_definition.name);
  }
},  
methods: {

},
watch: {

},
mounted() {

},    
}
</script>

<style scoped>


</style>


