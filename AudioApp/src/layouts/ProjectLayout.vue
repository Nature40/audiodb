<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          @click="toggleLeftDrawer"
        />

        <q-toolbar-title>
          AudioApp - {{project}}
        </q-toolbar-title>

        <div>AudioApp v2</div>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      bordered
    >
      <q-list>
        <q-item clickable :to="'/projects'">
          <q-item-section avatar>
            <q-icon name="school" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Projects</q-item-label>
            <q-item-label caption>Go to entry page with project selection</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable :to="'/projects/' + project + '/main'">
          <q-item-section avatar>
            <q-icon name="school" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Main</q-item-label>
            <q-item-label caption>Go to main page</q-item-label>
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

  setup () {
    const leftDrawerOpen = ref(false)

    return {
      leftDrawerOpen,
      toggleLeftDrawer () {
        leftDrawerOpen.value = !leftDrawerOpen.value
      }
    }
  },

  computed: {
    project() {
      return this.$route.params.project;
    },
  },
  
  watch: {
    project: {
      handler(val, oldVal) {
        console.log('e changed');
        this.$store.commit('setProject', this.project);
      },
      immediate: true,
    },
  },
})
</script>
