import axios from 'axios'

export default {
namespaced: true,
state: {
  data: undefined,
  loading: false,
  error: undefined,

  samples_table_count: undefined,
  
  player_spectrum_threshold: undefined,
  default_player_spectrum_threshold: undefined,
  
  player_fft_window: undefined,
  default_player_fft_window: undefined,
  
  player_fft_window_step_factor: 1,
  default_player_fft_window_step_factor: 1,
  player_fft_step: undefined, // calculated by player_fft_window and player_fft_window_step_factor
  
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
  
  detail_fft_window_overlap_percent: 0.75,
  default_detail_fft_window_overlap_percent: 0.75,
  
  profiles: {},
  profileIds: [''],
  profileDefaultId: '(Project defaults)',
  profileID: undefined,
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

    state.samples_table_count = project.samples_table_count;   

    state.profiles = project.profiles;
    var profileIDs = Object.keys(project.profiles);
    state.profileIds = [state.profileDefaultId, ...profileIDs];
    this.commit('project/setDefaultProfile');
  },
  setDefaultProfile(state) {
    state.profileID = state.profileDefaultId;
    var project = state.data;

    this.commit('project/setDefaults', project);
    this.commit('project/set', project);
  },
  setError(state, error) {
    state.loading = false;
    state.error = error;
  },
  setDefaults(state, settings) {
    if(settings.player_fft_window !== undefined) {
      state.default_player_fft_window = settings.player_fft_window;
    }
    if(settings.player_fft_window_step_factor !== undefined) {
      state.default_player_fft_window_step_factor = settings.player_fft_window_step_factor;
    }
    if(settings.player_fft_cutoff_lower_frequency !== undefined) {
      state.default_player_fft_cutoff_lower_frequency = settings.player_fft_cutoff_lower_frequency;
    }
    if(settings.player_fft_cutoff_upper_frequency !== undefined) {
      state.default_player_fft_cutoff_upper_frequency = settings.player_fft_cutoff_upper_frequency;
    }
    if(settings.player_spectrum_threshold !== undefined) {
      state.default_player_spectrum_threshold = settings.player_spectrum_threshold;
    }
    if(settings.player_fft_intensity_max !== undefined) {
      state.default_player_fft_intensity_max = settings.player_fft_intensity_max;    
    }
    if(settings.player_spectrum_shrink_Factor !== undefined) {
      state.default_player_spectrum_shrink_Factor = settings.player_spectrum_shrink_Factor;    
    }
    if(settings.player_time_expansion_factor !== undefined) {
      state.default_player_time_expansion_factor = settings.player_time_expansion_factor;    
    }
    if(settings.player_static_lines_frequency !== undefined) {
      state.default_player_static_lines_frequency = settings.player_static_lines_frequency.length === 0 ? undefined : settings.player_static_lines_frequency;    
    }

    if(settings.detail_fft_window_overlap_percent !== undefined) {
      state.default_detail_fft_window_overlap_percent = settings.detail_fft_window_overlap_percent;    
    }
  },
  set(state, settings) {
    if(settings.player_fft_window !== undefined) {
      state.player_fft_window = settings.player_fft_window;
      state.player_fft_step = state.player_fft_window * state.player_fft_window_step_factor;  
    }
    if(settings.player_fft_window_step_factor !== undefined) {
      state.player_fft_window_step_factor = settings.player_fft_window_step_factor;
      state.player_fft_step = state.player_fft_window * state.player_fft_window_step_factor;
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

    if(settings.detail_fft_window_overlap_percent !== undefined) {
      state.detail_fft_window_overlap_percent = settings.detail_fft_window_overlap_percent;    
    }
  },  
  setProfile(state, profileID) {
    this.commit('project/setDefaultProfile');
    const profile = state.profiles[profileID];
    if(profile !== undefined) {
      this.commit('project/setDefaults', profile);
      this.commit('project/set', profile);
      state.profileID = profileID; 
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
    commit('setLoading');
    try {
      var params = {};
      params.locations = true;
      params.devices = true;
      //params.timestamps = true;
      params.dates = true;
      params.samples_table_count = true;
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
  setProfile({commit}, profileID) {
    commit('setProfile', profileID);
  }  
},
}
