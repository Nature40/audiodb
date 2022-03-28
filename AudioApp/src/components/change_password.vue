<template>
  <q-dialog v-model="show" style="width: 1000px;">
      <q-card style="width: 1000px;">
        <q-bar>
          <q-icon name="edit"/>
          <div>Change password</div>
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
            v-model="current_password" 
            label="Current password." 
            error-message="Please use at least 8 characters." 
            :error="!valid_current_password" 
          >
          </q-input>
          <q-input 
            square 
            outlined 
            v-model="new_password" 
            label="New password." 
            error-message="Please use at least 8 characters." 
            :error="!valid_new_password" 
          >
            <template v-slot:after>
              <q-btn flat round color="primary" icon="create" @click="generate_new_password" title="Generate new random password" />
            </template>
          </q-input>
        </q-card-section>

        <q-separator />

        <q-card-actions align="right">
          <q-spinner-radio v-show="busy"/>
          <q-btn flat color="primary" label="Submit" @click="onApply" :disable="busy || !valid_current_password || !valid_new_password" />
        </q-card-actions>
      </q-card>
    </q-dialog>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';
import { sha3_512 } from 'js-sha3';

export default defineComponent({
  name: 'change_password',
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
      current_password: '',
      new_password: '',
      busy: false,
    };
  },
  computed: {
    ...mapState({

    }), 
    valid_current_password() {
      return this.current_password && this.current_password.length >= 8;
    },
    valid_new_password() {
      return this.new_password && this.new_password.length >= 8;
    },           
  },
  methods: {
    async onApply() {
      try {
        this.busy = true;       
        var action = {action: 'change_password'};
        if(this.current_password !== undefined && this.current_password !== null && this.current_password.length >= 8 && this.new_password !== undefined && this.new_password !== null && this.new_password.length >= 8) {
          //console.log(this.salt);
          //console.log(this.user_name);
          //console.log(this.current_password);
          var current_hash = sha3_512(this.salt + this.user_name + this.salt + this.current_password + this.salt);
          //console.log(current_hash);
          action.current_hash = current_hash;
          //console.log(this.new_password);
          var new_hash = sha3_512(this.salt + this.user_name + this.salt + this.new_password + this.salt);
          //console.log(new_hash);
          action.new_hash = new_hash;
        }
        var response = await this.$api.post('accounts', {actions: [action]})
        this.show = false;
        this.$q.notify({message: 'Password changed.', type: 'positive'});
      } catch(e) {
        console.log(e);
        this.$q.notify({message: 'Error changing password.', type: 'negative'});
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
    generate_new_password() {
      this.new_password = this.getNonce(12);
    },
  },
  watch: {
    show() {
      this.current_password = '';
      this.new_password = '';
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
