import { store } from 'quasar/wrappers'
import { createStore } from 'vuex'

import axios from 'axios'

import projects from './projects'

const isDev = process.env.DEV;
const api = axios.create({ baseURL: isDev ? 'http://localhost:8080/' : '/' })

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
      api: api,
    },

    getters: {
      api: (state) => (...parts) => {
        var path = parts.join('/');
        return isDev ? ('http://localhost:8080/' + path) : ('/' + path);
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
      setProject(state, payload) {
        state.project = payload;
      }
    }    
  });
  return Store;
})
