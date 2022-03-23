<template>
  <q-page class="flex justify-center" style="color: black;">
    <div>
    Account
    
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

    </div>
    
  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'Account',

  computed: {
    ...mapState({
      identity: state => state.identity.data,
    }),
  },

  methods: {

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
