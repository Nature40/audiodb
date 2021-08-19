<template>
  <q-layout view="hHh lpR fFf" class="fit">
    <q-header elevated>
      <q-toolbar>
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          @click="leftDrawerOpen = !leftDrawerOpen"
        />

        <q-toolbar-title>
          PhotoApp  
          <span class="text-blue-1 q-pa-xl">
            <span v-if="project === undefined">no project selected</span>
            <span v-else><span class="text-blue-10">Project:</span> {{project}}</span> 
          </span>
        </q-toolbar-title>

        <div>v1.0</div>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"      
      bordered
      content-class="bg-grey-1"
    >
      <q-scroll-area class="fit">
      <q-item clickable :active="true" v-ripple to="/query">
        <q-item-section avatar><q-icon name="search" /></q-item-section><q-item-section>Query</q-item-section>
      </q-item>
      <q-item clickable :active="true" v-ripple to="/browser">
        <q-item-section avatar><q-icon name="view_module" /></q-item-section><q-item-section>Browser</q-item-section>
      </q-item>
      <q-item clickable :active="true" v-ripple to="/viewer">
        <q-item-section avatar><q-icon name="crop_original" /></q-item-section><q-item-section>Viewer</q-item-section>
      </q-item>
      </q-scroll-area>
    </q-drawer>

    <q-page-container class="fit">
      <keep-alive>
        <router-view />
      </keep-alive>
    </q-page-container>
  </q-layout>
</template>

<script>

import {mapState} from 'vuex'

export default {
  name: 'MainLayout',
  components: { },
  data () {
    return {
      leftDrawerOpen: true,
    }
  },
  computed: {
    ...mapState({
      project: state => state.project,
    }),
  },   
  mounted () {
  },
}
</script>
