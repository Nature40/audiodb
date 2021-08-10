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
      state.date = new Date(state.data.date);
    },

    setError(state, error) {
      state.loading = false;
      state.error = error;
    },
  },
  
  actions: {
    async refresh({rootState, commit, rootGetters}) {
      commit('setLoading')
      commit('setData', []);
      if(rootState.photo.photo !== undefined) {
        var params = {};
        params.detections = true;
        console.log(rootState.photo.photo);
        try {
            var response =  await rootGetters.apiGET(['photodb2','photos', rootState.photo.photo], {params});
            commit('setData', response.data.photo);
        } catch(e) {
          console.log(e);
          commit('setError', 'error');
        }
      }
    },
  },
}