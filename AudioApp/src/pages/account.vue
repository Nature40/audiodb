<template>
  <q-page class="flex justify-center" style="color: black;">
    <div>
    Account
    <div v-if="identityLoading" style="color: blue;">
      Loading identity...
    </div>
      <div v-if="identity === undefined">
        Identity not loaded.
        <div v-if="identityError !== undefined" style="color: red;">
          Error loading identity
          <q-btn @click="refresh" v-if="!identityLoading">try again</q-btn>
        </div>
      </div>

      <q-list padding bordered separator v-if="identity">
        <q-item>
          <q-item-section>
            <q-item-label overline>Authentication</q-item-label>
            <q-item-label>{{identity.authentication}}</q-item-label>
          </q-item-section>
        </q-item>
        <q-item>
          <q-item-section>
            <q-item-label overline>User name</q-item-label>
            <q-item-label>{{identity.user}}</q-item-label>
          </q-item-section>
        </q-item>
        <q-item>
          <q-item-section>
            <q-item-label overline>Roles</q-item-label>
            <q-item-label><q-badge v-for="role in identity.roles" :key="role" class="role">{{role}}</q-badge></q-item-label>
          </q-item-section>
        </q-item>
      </q-list>

      <q-list padding bordered separator v-if="identity">
        <q-item clickable @click="$refs.ChangePassword.user_name = identity.user; $refs.ChangePassword.show = true;">
          <q-item-section avatar>
            <q-icon name="edit"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Change password</q-item-label>
            <change-password ref="ChangePassword" :salt="identity.salt" @changed="refresh"/>
          </q-item-section>
        </q-item>
        <q-item clickable @click="webauthn_register">
          <q-item-section avatar>
            <q-icon name="fingerprint"/>
          </q-item-section>
          <q-item-section>
            <q-item-label overline>FIDO2 (WebAuthn, CTAP2)</q-item-label>
            <q-item-label>Register</q-item-label>
          </q-item-section>
        </q-item>
        <q-item clickable @click="webauthn_validate">
          <q-item-section avatar>
            <q-icon name="done"/>
          </q-item-section>
          <q-item-section>
            <q-item-label overline>FIDO2 (WebAuthn, CTAP2)</q-item-label>
            <q-item-label>Validate</q-item-label>
          </q-item-section>
        </q-item>
        <q-item clickable tag="a" :href="$store.getters['api']('logout')">
          <q-item-section avatar>
            <q-icon name="logout"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Log out</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>

    </div>

  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';
import cbor from 'cbor-js';

import ChangePassword from 'components/change_password';

function arrayBufferToBase64(arrayBuffer) {
  let byteBuffer = new Uint8Array(arrayBuffer);
  let text = String.fromCharCode.apply(null, byteBuffer);
  let base64 = btoa(text);
  return(base64)
}

function base64ToUint8Array(base64) {
    var binary = window.atob(base64);
    var len = binary.length;
    var bytes = new Uint8Array(len);
    for (var i = 0; i < len; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

let rpId = window.location.hostname;

export default defineComponent({
  name: 'Account',

  components: {
    ChangePassword,
  },

  computed: {
    ...mapState({
      identity: state => state.identity.data,
      identityLoading: state => state.identity.loading,
      identityError: state => state.identity.error,
    }),
  },

  methods: {

    refresh() {
      this.$store.dispatch('identity/refresh');
    },

    async webauthn_register() {
      try {
        var textEncoder = new TextEncoder("utf-8");
        var textDecoder = new TextDecoder("utf-8");

        var userIdBuffer = textEncoder.encode(this.identity.user);
        console.log(this.identity.user);
        console.log(userIdBuffer);
        console.log(textDecoder.decode(userIdBuffer));
        console.log(arrayBufferToBase64(userIdBuffer));

        var challengeResponse = await fetch(this.$store.getters['api']('loginWebAuthn'), {
              method: 'GET',
              headers: {
                'Accept': 'application/json',
              },
        });
        var challengeResponseJson = await challengeResponse.json();
        var challengeText = challengeResponseJson.challenge;
        var challenge = base64ToUint8Array(challengeText);

        var publicKey = {
          authenticatorSelection:{
            authenticatorAttachment: "cross-platform",
            requireResidentKey: true,
            userVerification: "required"
          },
          challenge: challenge,
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

        var response = await this.$api.post('WebAuthn/register', request);
        if(response.headers.authentication === 'required') {
          alert("You are not logged in. Reload the page and log in.");
        } else {
          alert(response.data);
        }
        //this.$q.notify({message: 'Registered.', type: 'positive'});
      } catch(err) {
        alert(err);
        //this.$q.notify({message: err, type: 'negative'});
      }
    },

    async webauthn_validate() {
      try {
        var challengeResponse = await fetch(this.$store.getters['api']('loginWebAuthn'), {
            method: 'GET',
            headers: {
              'Accept': 'application/json',
            },
        });
        var challengeResponseJson = await challengeResponse.json();
        var challengeText = challengeResponseJson.challenge;
        var challenge = base64ToUint8Array(challengeText);

        var publicKey = {
          challenge: challenge,
          rpId: rpId,
        };

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

        var verifyResponse = await this.$api.post('WebAuthn/verify', request);
        if(verifyResponse.headers.authentication === 'required') {
          alert("You are not logged in. Reload the page and log in.");
        } else {
          alert(verifyResponse.data);
        }
        //this.$q.notify({message: verifyResponse.data, type: 'positive'});
      } catch(err) {
        if(err !== undefined) {
          if(err.response !== undefined) {
            if(err.response.data !== undefined) {
              alert(err.response.data);
            } else {
              alert(err);
            }
          } else {
            alert(err);
          }
        } else {
          alert("error");
        }
        //this.$q.notify({message: err, type: 'negative'});
      }
    },
  },

  watch: {

  },

  async mounted() {
    this.$store.dispatch('identity/init');
  },
})
</script>

<style scoped>

.role {
  margin-right: 10px;
}

</style>
