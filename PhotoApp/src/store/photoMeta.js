export default {
  namespaced: true,

  state: () => ({
    data: [],
    loading: false,
    error: undefined,
    date: undefined,
  }),

  getters: {
  },

  mutations: {
    setLoading(state) {
      state.loading = true;
      state.error = undefined;

      state.data = undefined;
      state.date = undefined;
    },

    setData(state, data) {
      state.loading = false;
      state.data = data;
      state.date = new Date(state.data.timestamp);
    },

    setError(state, error) {
      state.loading = false;
      state.error = error;
    },
  },
  
  actions: {
    async query({rootState, commit, rootGetters}, params) {
      commit('setLoading')
      try {
          var response =  await rootGetters.apiGET(['PhotoDB','photos', rootState.photo.photo.id], {params});
          commit('setData', response.data.photo_meta);
        } catch {
          commit('setError', 'error');
        }
    },
  },
}