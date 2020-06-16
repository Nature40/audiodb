export default {
namespaced: true,
state: {
  player_spectrum_threshold: 13.5,
  player_spectrum_threshold_default: 13.5,
},
getters: {
},
mutations: {
  set(state, settings) {
    if(settings.player_spectrum_threshold !== undefined) {
      state.player_spectrum_threshold = settings.player_spectrum_threshold;
    }
  },
},
actions: {
},
}
