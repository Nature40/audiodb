import axios from 'axios'

export default {
namespaced: true,
state: {
  data: undefined,
  loading: false,
  error: undefined,
  player_spectrum_threshold: undefined,
  default_player_spectrum_threshold: undefined,
  player_fft_window: undefined,
  default_player_fft_window: undefined,
  player_fft_step: undefined,  //4096,//2048,//1024,
  player_fft_cutoff: undefined,  //4096,//2048,//1024,
  player_fft_intensity_max: undefined,  //4096,//2048,//1024,
},
getters: {
  isLoading: state => {
    return state.data !== undefined;
  },
  isError: state => {
    return state.error !== undefined;
  },
},
mutations: {
  setLoading(state) {
    state.loading = true;
    state.error = undefined;
  },
  setData(state, data) {
    var project = data.project;
    state.data = project;
    state.loading = false;
    state.error = undefined;
    state.player_spectrum_threshold = project.player_spectrum_threshold;
    state.default_player_spectrum_threshold = state.player_spectrum_threshold;
    state.player_fft_window = 1024; //16384,//8192,//4096,//2048,//1024,
    state.default_player_fft_window = state.player_fft_window;
    state.player_fft_step = state.player_fft_window / 4;
    var half = state.player_fft_window / 2;
    state.player_fft_cutoff = half > 800 ? 800 : half;
    state.player_fft_intensity_max = 23;
  },
  setError(state, error) {
    state.loading = false;
    state.error = error;
  },
  set(state, settings) {
    console.log("setset");
    if(settings.player_spectrum_threshold !== undefined) {
      console.log("setsetset");
      state.player_spectrum_threshold = settings.player_spectrum_threshold;
      state.player_fft_window = settings.player_fft_window;
      state.player_fft_step = state.player_fft_window / 4;
      var half = state.player_fft_window / 2;
      state.player_fft_cutoff = half > 800 ? 800 : half;
      state.player_fft_intensity_max = 23;    
    }
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
      var params = {};
      params.locations = true;
      params.timestamps = true;
      var response = await rootState.api.get('projects/' + rootState.projectId, {params});
      commit('setData', response.data);
    } catch(e) {
      console.log(e);
      commit('setError', 'error');
    }
  },
  set({commit}, settings) {
    console.log("set");
    commit('set', settings);
  },
},
}
