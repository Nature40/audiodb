<template>
  <q-layout view="hHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          @click="toggleLeftDrawer"
          title="Toggle sidebar."
        />

        <q-toolbar-title>
          <span v-if="$route.path === '/projects'">
            Project selection
          </span>    
          <span v-if="$route.path === '/account'">
            Account
          </span>  
          <span v-if="$route.path === '/accounts'">
            Accounts
          </span>               
        </q-toolbar-title>

        <div class="text-amber-4">AudioApp v2</div>
        <q-btn icon="logout" title="Log out" padding="xs" flat round color="grey" :href="$store.getters['api']('logout')"></q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      bordered
    >
      <q-list>
        <q-item clickable tag="a" :to="'/projects'" active-class="active-item">
          <q-item-section avatar>
            <q-icon name="view_week" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Projects</q-item-label>
            <q-item-label caption>Project selection</q-item-label>
          </q-item-section>         
        </q-item>
        <q-item clickable tag="a" :to="'/account'" active-class="active-item">
          <q-item-section avatar>
            <q-icon name="person" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Account</q-item-label>
            <q-item-label caption>User account details</q-item-label>
          </q-item-section>          
        </q-item>
        <q-item clickable tag="a" :to="'/accounts'" active-class="active-item" v-if="role_create_account || role_manage_account || role_list_account">
          <q-item-section avatar>
            <q-icon name="people" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Accounts</q-item-label>
            <q-item-label caption>Accounts management</q-item-label>
          </q-item-section>          
        </q-item>        
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script>
import { defineComponent, ref } from 'vue';
import {mapState} from 'vuex';

export default defineComponent({
  name: 'MainLayout',

  data() {
    return {
      leftDrawerOpen: true,
    };
  },

  computed: {
    ...mapState({
      identity: state => state.identity.data,
      role_create_account: state => state.identity.create_account,      
      role_manage_account: state => state.identity.manage_account, 
      role_list_account: state => state.identity.list_account, 
    }),  
  },  

  components: {
  },

  methods: {
    toggleLeftDrawer () {
      this.leftDrawerOpen = !this.leftDrawerOpen;
    }
  },

  async mounted() {
    this.$store.dispatch('identity/init'); 
    //this.$nextTick(() => this.toggleLeftDrawer());
  },
})
</script>

<style scoped>

.active-item {
  background-color: aliceblue;
  border-right: 2px solid black;
}

</style>
