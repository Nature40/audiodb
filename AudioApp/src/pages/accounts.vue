<template>
  <q-page class="flex justify-center" style="color: black;">
    <div>
      <table class="accounts-table" v-if="role_list_account">
        <thead>
          <tr>
            <th>Account</th>
            <th>Roles</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="account in accounts" :key="account.name">
            <td>{{account.name}}</td>
            <td><q-badge v-for="role in account.roles" :key="role" class="role">{{role}}</q-badge></td>
            <td>
              <q-btn v-if="role_manage_account" flat round color="primary" icon="edit" @click="edit_account(account)" title="Edit account." />
              <q-btn v-if="role_manage_account" flat round color="red" icon="delete_forever" @click="delete_account(account)" title="Delete account." />
            </td>
          </tr>
        </tbody>
      </table>
      <div style="padding: 20px;"></div>
      <q-btn v-if="role_create_account" @click="$refs.CreateAccount.show = true;" icon="person_add" title="Open dialog box to create a new account." padding="xs">Create new Account</q-btn>
      <create-account ref="CreateAccount" :salt="salt" @changed="refresh" />
    </div>   

    <edit-account ref="EditAccount" :salt="salt" @changed="refresh"/> 
  </q-page>
</template>

<script>
import { defineComponent } from 'vue';
import {mapState} from 'vuex';
import CreateAccount from 'components/create_account';
import EditAccount from 'components/edit_account';

export default defineComponent({
  name: 'Accounts',

  components: {
    CreateAccount,
    EditAccount,
  },  
  
  data() {
    return {
      accounts: undefined,
      salt: undefined,
    };  
  },

  computed: {      
    ...mapState({
      identity: state => state.identity.data,
      role_create_account: state => state.identity.create_account,      
      role_list_account: state => state.identity.list_account, 
      role_manage_account: state => state.identity.manage_account,
    }),  
  },

  methods: {
    async refresh() {
      if(this.role_list_account) {
        try {
          var response = await this.$api.get('accounts');
          this.accounts = response.data.accounts;
          this.salt = response.data.salt; 
        } catch(e) {
          console.log(e);
          this.$q.notify({message: 'Error loading data.', type: 'negative'});
        }
      }
    },
    async delete_account(account) {
      this.$q.dialog({
        title: 'Confirm',
        message: 'Do you warnt to delete the account?   ' + account.name,
        cancel: true,
        persistent: true
      }).onOk(async () => {
        try {
          var action = {action: 'delete_account', user: account.name};
          var response = await this.$api.post('accounts', {actions: [action]});
          this.$q.notify({message: 'Account deleted.', type: 'positive'});
        } catch(e) {
          console.log(e);
          this.$q.notify({message: 'Error deleting account.', type: 'negative'});
        } finally {
          this.refresh();
        }
      });
    },
    async edit_account(account) {
      let roles = account.roles === undefined ? [] : account.roles.slice();
      console.log(roles);
      this.$refs.EditAccount.user_name = account.name;      
      this.$refs.EditAccount.selectedRoles = roles;
      this.$refs.EditAccount.show = true;

    },
  },
  
  watch: {
    role_list_account() {
      this.refresh();
    },
  },

  async mounted() {
    this.$store.dispatch('identity/init');    
    this.refresh();  
  },
})
</script>

<style scoped>

.role {
  margin-right: 10px;
}

.accounts-table {
    border-collapse: collapse;
    margin: 20px 0;
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.185);
}

.accounts-table thead tr {
    background-color: #929292;
    color: #ffffff;
}

.accounts-table th,
.accounts-table td {
    padding: 0px 15px;
}

accounts-table tbody tr {
    border-bottom: 1px solid #dddddd;
}

.accounts-table tbody tr:nth-of-type(even) {
    background-color: #f3f3f3;
}

.accounts-table tbody tr:last-of-type {
    border-bottom: 2px solid #999999;
}

.accounts-table th + th, .accounts-table td + td { border-left:2px solid rgba(0, 0, 0, 0.253); }

.accounts-table tbody tr:hover {
    color: #757575;
    background-color: rgba(0, 0, 0, 0.103);
}





</style>