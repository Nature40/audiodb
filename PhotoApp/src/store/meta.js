export default {
  namespaced: true,

  state: () => ({
    data: undefined,
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
    init({state, dispatch}) {
      if(state.data === undefined) {
        dispatch('refresh');
      }
    },
    async query({rootState, commit, rootGetters}, params) {
      commit('setLoading')
      if(params === undefined) {
        params = {};
      }
      params.locations = true;
      params.classification_definitions = true;
      params.review_lists = true;
      try {
        var response =  await rootGetters.apiGET(['photodb2'], {params});
          commit('setData', response.data);
        } catch(e) {
          console.log(e);
          commit('setError', 'error');
        }
    },
  },
}