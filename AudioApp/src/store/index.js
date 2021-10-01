import Vue from 'vue'
import { store } from 'quasar/wrappers'
import { createStore } from 'vuex'

import projects from './projects'

const isDev = process.env.DEV;

export default store(function (/* { ssrContext } */) {
  const Store = createStore({
    modules: {
      projects,
    },

    // enable strict mode (adds overhead!)
    // for dev mode and --debug builds only
    strict: process.env.DEBUGGING,

    state: {
      project: undefined,
    },

    getters: {
      api: (state) => (...parts) => {
        var path = parts.join('/');
        return isDev ? ('http://localhost:8080/' + path) : ('/' + path);
      },
      apiGET: (state, getters) => (parts, config) => {
        console.log(this);
        //console.log(parts);
        var path = getters.api(...parts);
        //console.log(path);
        //console.log(Vue.prototype.$axios);
        return Vue.prototype.$axios.get(path, config);
      },
      apiPOST: (state, getters) => (parts, data, config) => {
        var path = getters.api(...parts);
        console.log("POST " + path);
        return Vue.prototype.$axios.post(path, data, config);
      },
    },    

    mutations: {
      setProject(state, payload) {
        state.project = payload;
      }
    }    
  });
  return Store;
})
