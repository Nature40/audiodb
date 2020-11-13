export default {
  namespaced: true,

  state: () => ({
    index: undefined,
    photo: undefined,
  }),

  getters: {
  },

  mutations: {
    setIndexAndPhoto(state, {index, photo}) {
      state.index = index;
      state.photo = photo;
    },
  },
  
  actions: {
    setIndex({commit, rootState}, index) {
      var photo = rootState.photos.data[index];
      commit('setIndexAndPhoto', {index, photo});
    },
  },
}