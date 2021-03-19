import axios from 'axios'

export default {
namespaced: true,
state: {
  data: undefined,
  loading: false,
  error: undefined,
  readonly: true,
},
getters: {
  isLoading: state => {
    return state.data !== undefined;
  },
  isError: state => {
    return state.error !== undefined;
  },
  isRole: (state) => (role) => {
    return state.data === undefined ? false : state.data.roles.includes(role);
  },
  isReadonly: state => {
    return state.readonly;
  },
},
mutations: {
  setLoading(state) {
    state.loading = true;
    state.error = undefined;
  },
  setData(state, data) {
    state.data = data;
    state.loading = false;
    state.error = undefined;
    state.readonly = state.data.roles.includes('readonly');    
  },
  setError(state, error) {
    state.loading = false;
    state.error = error;
  }
},
actions: {
  init({state, dispatch}) {
    if(state.data === undefined) {
      dispatch('refresh');
    }
  },
  refresh({commit, rootState}) {
    commit('setLoading');
    axios.get(rootState.apiBase + 'identity')
    .then(function(response) {
      commit('setData', response.data);
    })
    .catch(function(error) {
      commit('setError', error);
    });
  }
},
}
