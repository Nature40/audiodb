import Vue from 'vue'
import Vuex from 'vuex'
import identity from './identity'
import account from './account'
import label_definitions from './label_definitions'
import settings from './settings'
import samples from './samples'
import review_statistics from './review_statistics'

Vue.use(Vuex)

const isDev = process.env.NODE_ENV !== 'production'

export default new Vuex.Store({
  modules: {
    identity,
    account,
    label_definitions,
    settings,
    samples,
    review_statistics,
  },
  strict: isDev,
  state: {
    //apiBase: isDev ? 'http://127.0.0.1:8080/' : '../../',  // HTTP
    //apiBase: isDev ? 'https://localhost:8000/' : '../../', // HTTPS
    apiBase: isDev ? 'http://127.0.0.1:8081/' : '../../', // HTTP proxy
  },
  mutations: {

  },
  actions: {

  }
})
