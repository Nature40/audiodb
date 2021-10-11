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
    async refresh({commit, rootState, rootGetters}) {
      commit('setLoading')
      try {
        var response = await rootState.api.get('projects');
        commit('setData', response.data);
      } catch(e) {
        console.log(e);
        commit('setError', 'error');
      }
    },
  },
}