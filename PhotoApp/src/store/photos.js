export default {
  namespaced: true,

  state: () => ({
    data: [],
    loading: false,
    error: undefined,
  }),

  getters: {
  },

  mutations: {
    setLoading(state) {
      state.loading = true;
      state.error = undefined;
    },

    setData(state, data) {
      state.loading = false;
      state.data = data;
    },

    setError(state, error) {
      state.loading = false;
      state.error = error;
    },
  },
  
  actions: {
    async query({commit, rootGetters, dispatch}, params) {
      commit('setLoading')
      commit('setData', []);
      dispatch('photo/setIndex', 0, { root: true })
      try {
        var response = await rootGetters.apiGET(['photodb2','photos'], {params});
        commit('setData', response.data.photos);
        dispatch('photo/setIndex', 0, { root: true })
      } catch(e) {
        commit('setError', 'error');
      }
    },
  },
}