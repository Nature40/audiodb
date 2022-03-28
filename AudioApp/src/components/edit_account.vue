<template>
  <q-dialog v-model="show" style="width: 1000px;">
      <q-card style="width: 1000px;">
        <q-bar>
          <q-icon name="person_add"/>
          <div>Edit account</div>
          <q-space />
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </q-bar>     

        <q-card-section>
          User name: {{user_name}}
          <q-input 
            square 
            outlined 
            v-model="password" 
            label="Leave empty to keep current password." 
            error-message="Please use at least 8 characters." 
            :error="!valid_password" 
          >
            <template v-slot:after>
              <q-btn flat round color="primary" icon="create" @click="generate_password" title="Generate new random password" />
            </template>
          </q-input>
          <q-select
            filled
            v-model="selectedRoles"
            :options="roles"
            label="Roles"
            style="width: 250px"
            dense
            multiple
            option-label="name"
            option-value="name"
            emit-value
            clearable
          />
        </q-card-section>

        <q-separator />

        <q-card-actions align="right">
          <q-spinner-radio v-show="busy"/>
          <q-btn flat color="primary" label="Submit" @click="onApply" :disable="busy" />
        </q-card-actions>
      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';
import { sha3_512 } from 'js-sha3';

export default defineComponent({
  name: 'edit_account',
  props: ['salt'],
  setup () {
    const show = ref(false);
    return {
      show,
    };
  },
  data() {
    return {  
      user_name: '',
      password: '',
      roles: ['admin', 'manage_account', 'create_account', 'readOnly', 'reviewedOnly'],
      selectedRoles: [],
      busy: false,
    };
  },
  computed: {
    ...mapState({

    }), 
    valid_password() {
      return this.password && this.password.length >= 8;
    },           
  },
  methods: {
    async onApply() {
      try {
        this.busy = true;       
        var action = {action: 'edit_account', user: this.user_name};
        if(this.password !== undefined && this.password !== null && this.password.length >= 8) {
          var hash = sha3_512(this.salt + this.user_name + this.salt + this.password + this.salt);
          console.log(hash);
          action.hash = hash;
        }
        if(this.selectedRoles !== undefined && this.selectedRoles !== null && this.selectedRoles.length > 0) {
          action.roles = this.selectedRoles;
        } else {
          action.roles = [];
        }
        var response = await this.$api.post('accounts', {actions: [action]})
        this.show = false;
        this.$q.notify({message: 'Account edited.', type: 'positive'});
      } catch(e) {
        console.log(e);
        this.$q.notify({message: 'Error editing account.', type: 'negative'});
      } finally {
        this.busy = false;
        this.$emit('changed');
      }
    },
    refresh() {
    },
    getNonce(len) {
      var rnd = new Uint32Array(len);
      window.crypto.getRandomValues(rnd);
      var nonce = "";
      var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
      var charsLen = chars.length;
      for(var i = 0; i < len; i++) {
        nonce += chars[rnd[i] % charsLen];
      }
      return nonce;
    },
    generate_password() {
      this.password = this.getNonce(12);
    },
  },
  watch: {
    show() {
      this.password = '';
      if(this.show) {
        this.refresh();
      }
    },
  },
  async mounted() {
  },
});
</script>

<style scoped>
.selected-sample {
  background-color: rgba(0, 0, 0, 0.021);
  font-weight: bold;
}
</style>
