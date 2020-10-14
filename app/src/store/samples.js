import axios from 'axios'

export default {
namespaced: true,
state: {
  data: [],
  loading: false,
  error: undefined,
},
getters: {
  isError: state => {
    return state.error !== undefined;
  },
  previous: state => sample => {
    if(sample === undefined) {
      return undefined;
    }
    var index = state.data.findIndex(s => s.id === sample.id);
    if(index < 0) {
      return undefined;
    }
    if(index < 1) {
      return undefined;
    }
    return state.data[index - 1];
  },
  next: state => sample => {
    if(sample === undefined) {
      return undefined;
    }    
    var index = state.data.findIndex(s => s.id === sample.id);
    if(index < 0) {
      return undefined;
    }
    if(index > state.data.length - 2) {
      return undefined;
    }
    return state.data[index + 1];
  },
},
mutations: {
  setLoading(state) {
    state.loading = true;
    state.error = undefined;
  },
  setData(state, data) {
    data.forEach(sample => sample.datetime = new Date(sample.timestamp * 1000));
    state.data = data;
    state.loading = false;
    state.error = undefined;
  },
  setError(state, error) {
    state.loading = false;
    state.error = error;
  }
},
actions: {
  async query({commit, rootState}) {
    commit('setLoading');
    try {
      var response = await axios.post(rootState.apiBase + 'samples');
      commit('setData', response.data.samples);
    } catch(error) {
      commit('setError', error);
    }
  }
},
}
