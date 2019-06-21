import Vue from 'vue'
import Router from 'vue-router'

import audioView from './components/audio-view'
const accountView = () => import('./components/account-view.vue')

Vue.use(Router)

export default new Router({
  routes: [
    { path: '/audio', component: audioView },
    { path: '/account', component: accountView },

    { path: '/', redirect: '/audio' },
    { path: '*', redirect: '/' },
  ]
})
