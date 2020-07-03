import Vue from 'vue'

import Vuetify from 'vuetify/lib'
import 'vuetify/src/stylus/app.styl'
Vue.use(Vuetify, {iconfont: 'md'})

import Multiselect from 'vue-multiselect'
Vue.component('multiselect', Multiselect)
import 'vue-multiselect/dist/vue-multiselect.min.css'

import RingLoader from 'vue-spinner/src/RingLoader.vue';
Vue.component('ring-loader', RingLoader);
import PulseLoader from 'vue-spinner/src/PulseLoader.vue';
Vue.component('pulse-loader', PulseLoader);

import './fonts/fonts.css'

import App from './App.vue'

import router from './router'

import store from './store'

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
