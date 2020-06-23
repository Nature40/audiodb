export default {
namespaced: true,
state: {
  player_spectrum_threshold: 13.5,
  player_spectrum_threshold_default: 13.5,
  player_playbackRate: 1,
  player_preservesPitch: true,
},
getters: {
},
mutations: {
  set(state, settings) {
    if(settings.player_spectrum_threshold !== undefined) {
      state.player_spectrum_threshold = settings.player_spectrum_threshold;
    }
    if(settings.player_playbackRate !== undefined) {
      state.player_playbackRate = settings.player_playbackRate;
    }
    if(settings.player_preservesPitch !== undefined) {
      state.player_preservesPitch = settings.player_preservesPitch;
    }    
  },
},
actions: {
},
}
