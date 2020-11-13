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
    async query({commit, rootGetters}, params) {
      commit('setLoading')
      try {
          var response =  await rootGetters.apiGET(['PhotoDB','photos'], {params});
          commit('setData', response.data.photos);
        } catch {
          commit('setError', 'error');
        }
    },
  },
}