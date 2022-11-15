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
          <span v-if="$route.path === '/projects/' + project + '/main'">
            Audio view
          </span>
          <span v-if="$route.path === '/projects/' + project + '/list'">
            Work lists
          </span>          
          <span v-if="$route.path === '/projects/' + project + '/task'">
            Task submission
          </span>     
          <span v-if="$route.path === '/projects/' + project + '/tasks'">
            Task status
          </span>                
        </q-toolbar-title>

        <div class="text-amber-4">AudioApp v2</div>
        <div class="text-grey-5" style="margin-left: 10px;">[{{project}}]</div>
        <q-btn icon="logout" title="Log out" padding="xs" flat round color="grey" :href="$store.getters['api']('logout')"></q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      bordered
    >
      <q-list separator>
        <q-item clickable :to="'/projects'" active-class="active-item">
          <q-item-section avatar>
            <q-icon name="arrow_back" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Project selection</q-item-label>
            <q-item-label caption>Go back to entry page with project selection</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable :to="'/projects/' + project + '/main'" active-class="active-item">
          <q-item-section avatar>
            <q-icon name="campaign" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Audio view</q-item-label>
            <q-item-label caption>Go to main page</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable :to="'/projects/' + project + '/list?list=all_generator_labels'" active-class="active-item">
          <q-item-section avatar>
            <q-icon name="format_list_numbered" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Work lists</q-item-label>
            <q-item-label caption>Lists of audio samples</q-item-label>
          </q-item-section>
        </q-item>         

        <q-item clickable :to="'/projects/' + project + '/task'" active-class="active-item">
          <q-item-section avatar>
            <q-icon name="rocket_launch" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Task submission</q-item-label>
            <q-item-label caption>Execute tasks</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable :to="'/projects/' + project + '/tasks'" active-class="active-item">
          <q-item-section avatar>
            <q-icon name="list_alt" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Task status</q-item-label>
            <q-item-label caption>List status of submitted tasks</q-item-label>
          </q-item-section>
        </q-item>       
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view v-if="project !== undefined" />
      <span v-else>no project selected</span>
    </q-page-container>
  </q-layout>
</template>

<script>
import { defineComponent, ref } from 'vue'

export default defineComponent({
  name: 'ProjectLayout',

  components: {
  },

  data() {
    return {
      leftDrawerOpen: false,
    };
  },

  computed: {
    project() {
      return this.$route.params.project;
    },
  },

  methods: {
    toggleLeftDrawer () {
      this.leftDrawerOpen = !this.leftDrawerOpen;
    }
  },
  
  watch: {
    project: {
      handler(val, oldVal) {
        this.$store.dispatch('setProject', this.project);
      },
      immediate: true,
    },
  },

  mounted() {
    this.leftDrawerOpen = false
  },
})
</script>

<style scoped>

.active-item {
  background-color: aliceblue;
  border-right: 2px solid black;
}

</style>