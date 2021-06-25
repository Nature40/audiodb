export default {
namespaced: true,
state: {
  player_spectrum_threshold: 13.5,
  player_spectrum_threshold_default: 13.5,
  player_playbackRate: 1,
  player_preservesPitch: true,
  player_overwriteSamplingRate: false,
  player_samplingRate: 32000,
},
getters: {
},
mutations: {
  set(state, settings) {
    if(settings.player_spectrum_threshold !== undefined) {
      state.player_spectrum_threshold = settings.player_spectrum_threshold;
    }
    if(settings.player_spectrum_threshold_default !== undefined) {
      state.player_spectrum_threshold_default = settings.player_spectrum_threshold_default;
    }
    if(settings.player_playbackRate !== undefined) {
      state.player_playbackRate = settings.player_playbackRate;
    }
    if(settings.player_preservesPitch !== undefined) {
      state.player_preservesPitch = settings.player_preservesPitch;
    }
    if(settings.player_overwriteSamplingRate !== undefined) {
      state.player_overwriteSamplingRate = settings.player_overwriteSamplingRate;
    }
    if(settings.player_samplingRate !== undefined) {
      state.player_samplingRate = settings.player_samplingRate;
    }    
  },
},
actions: {
},
}
