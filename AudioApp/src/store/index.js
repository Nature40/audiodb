import { store } from 'quasar/wrappers'
import { createStore } from 'vuex'

import axios from 'axios'

import identity from './identity'
import projects from './projects'
import project from './project'

const isDev = process.env.DEV;
const api = axios.create({ baseURL: isDev ? 'http://localhost:8080/' : '/' })

export default store(function (/* { ssrContext } */) {
  const Store = createStore({
    modules: {
      identity,
      projects,
      project,
    },

    // enable strict mode (adds overhead!)
    // for dev mode and --debug builds only
    strict: process.env.DEBUGGING,

    state: {
      projectId: undefined,
      api: api,
    },

    getters: {
      api: (state) => (...parts) => {
        var path = parts.join('/');
        return isDev ? ('http://localhost:8080/' + path) : (window.location.origin + '/' + path);   //  window.location.origin as workaround for usage in  new URL(...)
      },
      apiGET: (state, getters) => (parts, config) => {
        var path = parts.join('/');
        return api.get(path, config);
      },
      apiPOST: (state, getters) => (parts, data, config) => {
        var path = getters.api(...parts);
        console.log("POST " + path);
        return Vue.prototype.$axios.post(path, data, config);
      },
    },    

    mutations: {
      setProject(state, productId) {
        state.projectId = productId;
      }
    },
    
    actions: {
      setProject({ commit, dispatch }, productId) {
        commit('setProject', productId);
        dispatch('project/refresh');   
      }
    },
  });
  return Store;
})
