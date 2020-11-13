import Vue from 'vue'
import Vuex from 'vuex'

import photos from './photos.js'
import photo from './photo.js'
import meta from './meta.js'

Vue.use(Vuex)

const isDev = process.env.DEV;

export default function (/* { ssrContext } */) {
  const Store = new Vuex.Store({
    modules: {
      photos,
      photo,
      meta
    },

    strict: process.env.DEBUGGING,

    getters: {
      api: (state) => (...parts) => {
        var path = parts.join('/');
        return isDev ? ('http://localhost:8080/' + path) : ('/' + path);
      },
      apiGET: (state, getters) => (parts, params) => {
        //console.log(parts);
        var path = getters.api(...parts);
        //console.log(path);
        //console.log(Vue.prototype.$axios);
        return Vue.prototype.$axios.get(path, params);
      },
    },
  });

  return Store;
}
