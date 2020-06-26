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
        <v-list-tile>
          <v-list-tile-title><a href="#/export"><v-icon>arrow_forward</v-icon>export</a></v-list-tile-title>
        </v-list-tile>       
      </v-list>
    </v-menu>
    <v-toolbar-title class="headline text-uppercase">
      Account
    </v-toolbar-title> 
    <identity-dialog></identity-dialog>
  </v-toolbar>
  <v-content v-if="identity !== undefined">
    <h2>Account details</h2>
    <br><b>authentication:</b> {{identity.authentication}}
    <br>
    <br><b>user:</b> {{identity.user}}
    <br>
    <br><b>roles:</b> <span style="background-color: #998c8c4f;"><span v-for="role in identity.roles" :key="role" class="role">{{role}}</span></span>
    <br>
    <br><v-btn @click="webauthn_register">webauthn_register (TESTING)</v-btn>
    <v-btn @click="webauthn_validate">webauthn_validate (TESTING)</v-btn>
  </v-content>
  <v-content v-if="isRole('create_account') && account !== undefined">
    <hr>
    <h2>Create new account</h2>
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
    <br>
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
import identityDialog from './identity-dialog'

import { sha3_512 } from 'js-sha3'
import axios from 'axios'
import cbor from 'cbor-js'

import { mapState, mapGetters, mapActions } from 'vuex'

function arrayBufferToBase64(arrayBuffer) {
  let byteBuffer = new Uint8Array(arrayBuffer);
  let text = String.fromCharCode.apply(null, byteBuffer);
  let base64 = btoa(text);
  return(base64)
}

let rpId = window.location.hostname;

export default {
name: 'account-view',
components: {
  identityDialog,
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
    identity: state => state.identity.data,
  }),
  ...mapGetters({
    isRole: 'identity/isRole',
  }), 
  hash() {
    if(this.account === undefined) {
      return undefined;
    }
    var salt = this.account.salt;
    return sha3_512(salt + this.inputUser + salt + this.inputPassword + salt);
  },
  /*testing() {
    return this.$store.getters['identity/isRole']('cool');
  }*/
},
methods: {
  ...mapActions({
    accountInit: 'account/init',
    identityInit: 'identity/init',
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
  async webauthn_register() {



    var textEncoder = new TextEncoder("utf-8");
    var textDecoder = new TextDecoder("utf-8");

    var userIdBuffer = textEncoder.encode(this.identity.user);
    console.log(this.identity.user);
    console.log(userIdBuffer);
    console.log(textDecoder.decode(userIdBuffer));
    console.log(arrayBufferToBase64(userIdBuffer));

    

    var publicKey = {
      authenticatorSelection:{
        authenticatorAttachment: "cross-platform",
        requireResidentKey: true,
        userVerification: "required"
      },
      challenge: new Uint8Array(26) /* this actually is given from the server */,
      rp: {
        name: "AudioDB",
        id: rpId,
      },
      user: {
        id: userIdBuffer,
        name: this.identity.user,
        displayName: this.identity.user,
      },
      pubKeyCredParams: [ {
        type: "public-key",
        alg: -7, // -7 indicates the elliptic curve algorithm ECDSA with SHA-256
      } ],
    };

    try {
      var credentialInfo = await navigator.credentials.create({ publicKey });
      console.log(credentialInfo);
      console.log("id: " + credentialInfo.id);
      console.log("id: " + arrayBufferToBase64(credentialInfo.rawId));
      console.log(credentialInfo.response);
      var clientData = JSON.parse(textDecoder.decode(credentialInfo.response.clientDataJSON));
      console.log(clientData);
      var attestationObject = cbor.decode(credentialInfo.response.attestationObject);
      console.log(attestationObject);
      console.log(attestationObject.authData);
      console.log("fmt: " + attestationObject.fmt);
      console.log(attestationObject.attStmt);
      console.log("alg: " + attestationObject.attStmt.alg);
      console.log("sig: " + attestationObject.attStmt.sig);
      console.log("x5c: " + attestationObject.attStmt.x5c);

      var request = {};
      request.rpId = rpId;
      request.clientDataJSON = arrayBufferToBase64(credentialInfo.response.clientDataJSON);
      request.attestationObject = arrayBufferToBase64(credentialInfo.response.attestationObject);
      console.log(request);
      
      var registerResponse = await axios.post(this.apiBase + 'WebAuthn/register', request);
      console.log(registerResponse);

    } catch(err) {
      alert(err);
    }

    /*navigator.credentials.create({ publicKey })
      .then( newCredentialInfo => {
        console.log(newCredentialInfo);
      }).catch(function (err) {
        alert(err);
    });*/
  },

  async webauthn_validate() {
    var publicKey = {
      challenge: new Uint8Array(26),
      rpId: rpId,
    };

    try {
      var credentialInfo = await navigator.credentials.get({ publicKey });
      console.log(credentialInfo);
      var authenticatorAssertionResponse = credentialInfo.response;
      console.log(authenticatorAssertionResponse);

      var request = {};
      request.credentialId = arrayBufferToBase64(credentialInfo.rawId);
      request.authenticatorData = arrayBufferToBase64(authenticatorAssertionResponse.authenticatorData);
      request.signature = arrayBufferToBase64(authenticatorAssertionResponse.signature);
      request.clientDataJSON = arrayBufferToBase64(authenticatorAssertionResponse.clientDataJSON);
      request.userHandle = arrayBufferToBase64(authenticatorAssertionResponse.userHandle);
      request.rpId = rpId;
      console.log(request);
      
      var verifyResponse = await axios.post(this.apiBase + 'WebAuthn/verify', request);
      alert(verifyResponse.data);


    } catch(err) {
      alert(err);
    }

  },
},
mounted() {
  this.identityInit();
  this.accountInit();
},
}
</script>

<style scoped>

.role {
  padding: 5px;
  background-color: #e8f4ff;
  margin: 5px;  
}

</style>
