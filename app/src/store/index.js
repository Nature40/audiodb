import Vue from 'vue'
import Vuex from 'vuex'
import identity from './identity'
import account from './account'
import label_definitions from './label_definitions'

Vue.use(Vuex)

const isDev = process.env.NODE_ENV !== 'production'

export default new Vuex.Store({
  modules: {
    identity,
    account,
    label_definitions,
  },
  strict: isDev,
  state: {
    apiBase: isDev ? 'http://127.0.0.1:8080/' : '../../',
  },
  mutations: {

  },
  actions: {

  }
})
