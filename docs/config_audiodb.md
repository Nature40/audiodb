# AudioDB Configuration

Application settings are in YAML file `config.yaml`

`config.yaml` example:
```yaml
login: true
http_port: 8080
    
audio:
  project: my_audio_project
  time_zone: UTC+1
  root_data_path: ./audio
  root_path: ./audio_meta
  device_inventory_file: audio_recorder_inventory.csv
  label_definitions_file: audio_label_definitions.yaml    
  
  player_fft_window: 1024
  player_fft_window_step_factor: 1
  player_time_expansion_factor: 1
  player_spectrum_threshold: 16.4
  player_fft_intensity_max: 26
  player_static_lines_frequency: [2000, 3000, 8000, 16000, 45000, 50000, 150000]
  #player_fft_cutoff_lower_frequency: 2000
  #player_fft_cutoff_upper_frequency: 10000 
  detail_fft_window_overlap_percent: 92
  
  profiles:
    birds:
      player_fft_window: 4096
      player_fft_window_step_factor: 0.125
      player_time_expansion_factor: 1
      player_spectrum_threshold: 16.4
      player_fft_intensity_max: 26
      player_static_lines_frequency: [3000, 8000]
      player_fft_cutoff_lower_frequency: 2000
      player_fft_cutoff_upper_frequency: 10000 
      detail_fft_window_overlap_percent: 92
    birds_scan:
      player_fft_window: 4096
      player_fft_window_step_factor: 1
      player_spectrum_shrink_Factor: 4
      player_time_expansion_factor: 1
      player_spectrum_threshold: 16.4
      player_fft_intensity_max: 26
      player_static_lines_frequency: [3000, 8000]
      player_fft_cutoff_lower_frequency: 2000
      player_fft_cutoff_upper_frequency: 10000 
      detail_fft_window_overlap_percent: 92
```