import Vue from 'vue'
import Router from 'vue-router'

import audioView from './components/audio-view'
const accountView = () => import('./components/account-view.vue')
const exportView = () => import('./components/export-view.vue')
const classificationView = () => import('./components/classification-view.vue')

Vue.use(Router)

export default new Router({
  routes: [
    { path: '/audio', component: audioView },
    { path: '/account', component: accountView },
    { path: '/export', component: exportView },
    { path: '/classification', component: classificationView },

    { path: '/', redirect: '/audio' },
    { path: '*', redirect: '/' },
  ]
})
