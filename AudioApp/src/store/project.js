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
  player_fft_step: undefined,
  //player_fft_cutoff: undefined,
  player_fft_cutoff_lower_frequency: 0,
  default_player_fft_cutoff_lower_frequency: 0,
  player_fft_cutoff_upper_frequency: 10000,
  default_player_fft_cutoff_upper_frequency: 10000,
  player_fft_intensity_max: undefined,
  default_player_fft_intensity_max: undefined,
  player_spectrum_shrink_Factor: undefined,
  default_player_spectrum_shrink_Factor: undefined,
  player_time_expansion_factor: undefined,
  default_player_time_expansion_factor: undefined,
  player_static_lines_frequency: [4000, 12000],
  default_player_static_lines_frequency: undefined,
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
    state.player_fft_window = project.player_fft_window;
    state.default_player_fft_window = state.player_fft_window;
    //state.player_fft_step = state.player_fft_window / 4;
    state.player_fft_step = state.player_fft_window;
    var half = state.player_fft_window / 2;
    state.player_fft_cutoff = half > 800 ? 800 : half;
    state.player_fft_intensity_max = project.player_fft_intensity_max;
    state.default_player_fft_intensity_max = state.player_fft_intensity_max;
    state.player_spectrum_shrink_Factor = 1;
    state.default_player_spectrum_shrink_Factor = state.player_spectrum_shrink_Factor;
    state.player_time_expansion_factor = project.player_time_expansion_factor;
    state.default_player_time_expansion_factor = state.player_time_expansion_factor;
    state.player_static_lines_frequency = project.player_static_lines_frequency;
    state.default_player_static_lines_frequency = state.player_static_lines_frequency;
    state.player_fft_cutoff_lower_frequency = project.player_fft_cutoff_lower_frequency;
    state.default_player_fft_cutoff_lower_frequency = state.player_fft_cutoff_lower_frequency;
    state.player_fft_cutoff_upper_frequency = project.player_fft_cutoff_upper_frequency;
    state.default_player_fft_cutoff_upper_frequency = state.player_fft_cutoff_upper_frequency;
  },
  setError(state, error) {
    state.loading = false;
    state.error = error;
  },
  set(state, settings) {
    if(settings.player_fft_window !== undefined) {
      state.player_fft_window = settings.player_fft_window;
      //state.player_fft_step = state.player_fft_window / 4;
      state.player_fft_step = state.player_fft_window;
      //var half = state.player_fft_window / 2;
      //state.player_fft_cutoff = half > 800 ? 800 : half;  
    }
    if(settings.player_fft_cutoff_lower_frequency !== undefined) {
      state.player_fft_cutoff_lower_frequency = settings.player_fft_cutoff_lower_frequency;
    }
    if(settings.player_fft_cutoff_upper_frequency !== undefined) {
      state.player_fft_cutoff_upper_frequency = settings.player_fft_cutoff_upper_frequency;
    }
    if(settings.player_spectrum_threshold !== undefined) {
      state.player_spectrum_threshold = settings.player_spectrum_threshold;
    }
    if(settings.player_fft_intensity_max !== undefined) {
      state.player_fft_intensity_max = settings.player_fft_intensity_max;    
    }
    if(settings.player_spectrum_shrink_Factor !== undefined) {
      state.player_spectrum_shrink_Factor = settings.player_spectrum_shrink_Factor;    
    }
    if(settings.player_time_expansion_factor !== undefined) {
      state.player_time_expansion_factor = settings.player_time_expansion_factor;    
    }
    if(settings.player_static_lines_frequency !== undefined) {
      state.player_static_lines_frequency = settings.player_static_lines_frequency.length === 0 ? undefined : settings.player_static_lines_frequency;    
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
      //params.timestamps = true;
      params.dates = true;
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
