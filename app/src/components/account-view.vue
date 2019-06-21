<template>
<v-app>
  <v-toolbar app>
    <v-toolbar-title class="headline text-uppercase">
      Account
    </v-toolbar-title> 
  </v-toolbar>

  <v-content v-if="account !== undefined">
    <v-text-field
      v-model="inputUser"
      ref="inputUser"
      label="User Name"
      :rules="[rules.required]"
      style="max-width: 300px;"      
    />
    <v-text-field
      v-model="inputPassword"
      ref="inputPassword"
      :append-icon="showPassword ? 'visibility' : 'visibility_off'"
      :rules="[rules.required, rules.min]"
      :type="showPassword ? 'text' : 'password'"
      name="input-10-2"
      label="Password"
      hint="At least 8 characters"
      value=""
      @click:append="showPassword = !showPassword"
      style="max-width: 300px; display: inline-block;"
    />
    <v-btn @click="generate_password">generate password</v-btn>
    <br>
    <v-btn @click="create_account" :loading="createAccountDialog" :disabled="$refs.inputUser === undefined || $refs.inputPassword === undefined || !$refs.inputUser.valid || !$refs.inputPassword.valid">create account</v-btn>
  </v-content>

  <v-content v-show="accountLoading">
    Loading...
  </v-content>

  <v-content v-show="accountError !== undefined">
    Error
  </v-content>

  <v-dialog
      v-model="createAccountDialog"
      persistent
      width="300"
  >
  <v-card>
    <v-card-title><span class="headline">creating account</span></v-card-title>
      <v-card-text>
        <div v-show="createAccountDialogMessage !== undefined">
        Done.
        <br>
        <br>
        <b>Account</b>
        <br>
        <br><b>User:</b>
        <br>{{inputUser}}
        <br>
        <br><b>Password:</b>
        <br>{{inputPassword}}
        <br>
        <br>
        </div>
        <div v-show="createAccountDialogError !== undefined">
        Error: {{createAccountDialogError}}
        </div>
        <v-btn @click="createAccountDialog = false" :loading="createAccountDialogMessage === undefined && createAccountDialogError === undefined">close</v-btn>
      </v-card-text>
  </v-card>    
  </v-dialog>

</v-app>
</template>

<script>
import { sha3_512 } from 'js-sha3'
import axios from 'axios'

import { mapState, mapGetters, mapActions } from 'vuex'

export default {
name: 'account-view',
components: {
},
data () {
  return {
    showPassword: false,
    rules: {
      required: v => !!v || 'Required.',
      min: v => v.length >= 8 || 'Min 8 characters',
    },
    inputUser: '',
    inputPassword: '',
    createAccountDialog: false,
    createAccountDialogMessage: undefined,
    createAccountDialogError: undefined,
  }
},
computed: {
  ...mapState({
    apiBase: state => state.apiBase,
    account: state => state.account.data,
    accountLoading: state => state.account.loading,
    accountError: state => state.account.error,
  }),
  ...mapGetters({
  }),
  hash() {
    if(this.account === undefined) {
      return undefined;
    }
    var salt = this.account.salt;
    return sha3_512(salt + this.inputUser + salt + this.inputPassword + salt);
  },
},
methods: {
  ...mapActions({
    accountInit: 'account/init',
  }),
  sh() {
    return sha3_512('');
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
    this.inputPassword = this.getNonce(8);
    this.showPassword = true;
    console.log(this.$refs.inputUser.valid);
  },
  create_account() {
    var self = this;
    self.createAccountDialog = true;
    self.createAccountDialogMessage = undefined;
    self.createAccountDialogError = undefined;
    axios.post(this.apiBase + 'accounts', {actions: [{action: 'create_account', user: self.inputUser, hash: self.hash}]})
    .then(function() {
      self.createAccountDialogMessage = "done";
      //self.createAccountDialogMessage = response.message;
    })
    .catch(function(error) {
      self.createAccountDialogError = "could not create account";
      if(error !== undefined) {
          self.createAccountDialogError = error;
          if(error.response !== undefined && error.response.data !== undefined) {
            self.createAccountDialogError = error.response.data;
            if(error.response.data.error !== undefined) {
              self.createAccountDialogError = error.response.data.error;
            }
          }
      }      
    });
  },
},
mounted() {
  this.accountInit();
},
}
</script>
