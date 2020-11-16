export default {
  namespaced: true,

  state: () => ({
    index: undefined,
    photo: undefined,
  }),

  getters: {
    hasPrev: (state, getters, rootState) => {
      return state.index > 0 && state.index < rootState.photos.data.length; 
    },
    hasNext: (state, getters, rootState) => {
      return state.index >= 0 && state.index + 1 < rootState.photos.data.length; 
    },
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
    move({state, rootState, dispatch}, relative) {
      var index = state.index + relative;
      if(index > 0 && index < rootState.photos.data.length) {
        dispatch('setIndex', index);
      }
    }    
  },
}