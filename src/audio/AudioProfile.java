package audio;

import util.yaml.YamlMap;

public final class AudioProfile {

	public static final AudioProfile DEFAULT = new AudioProfile(new Builder());

	public final double player_spectrum_threshold;
	public final double player_fft_intensity_max;
	public final double player_playbackRate; // obsolete
	public final boolean player_preservesPitch;
	public final boolean player_overwriteSamplingRate;
	public final double player_samplingRate;
	public final int player_fft_window;
	public final double player_fft_window_step_factor;	
	public final double player_spectrum_shrink_Factor;
	public final int player_time_expansion_factor;
	public final float[] player_static_lines_frequency;
	public final double player_fft_cutoff_lower_frequency;
	public final double player_fft_cutoff_upper_frequency;
	public final double player_mouse_move_factor;
	public final double detail_fft_window_overlap_percent;
	
	public static class Builder {		
		public double player_spectrum_threshold = 13.5;
		public double player_fft_intensity_max = 23;
		public double player_playbackRate = 1;
		public boolean player_preservesPitch = true;
		public boolean player_overwriteSamplingRate = false;
		public double player_samplingRate = 32000;
		public int player_fft_window = 1024; //16384,//8192,//4096,//2048,//1024,
		public double player_fft_window_step_factor = 1d;
		public double player_spectrum_shrink_Factor = 1d;
		public int player_time_expansion_factor = 1;
		public float[] player_static_lines_frequency = null;
		public double player_fft_cutoff_lower_frequency = 0;
		public double player_fft_cutoff_upper_frequency = 192000;		
		public double player_mouse_move_factor = 8d;
		public double detail_fft_window_overlap_percent = 75d;
		
		public Builder() {}

		public Builder(YamlMap yamlMap) {			
			player_spectrum_threshold = yamlMap.optDouble("player_spectrum_threshold", player_spectrum_threshold);
			player_fft_intensity_max = yamlMap.optDouble("player_fft_intensity_max", player_fft_intensity_max);
			player_playbackRate = yamlMap.optDouble("player_playbackRate", player_playbackRate);
			player_overwriteSamplingRate = yamlMap.optBoolean("player_overwriteSamplingRate", player_overwriteSamplingRate);
			player_samplingRate = yamlMap.optDouble("player_samplingRate", player_samplingRate);
			player_fft_window = yamlMap.optInt("player_fft_window", player_fft_window);
			player_fft_window_step_factor = yamlMap.optDouble("player_fft_window_step_factor", player_fft_window_step_factor);
			player_spectrum_shrink_Factor = yamlMap.optDouble("player_spectrum_shrink_Factor", player_spectrum_shrink_Factor);
			player_time_expansion_factor = yamlMap.optInt("player_time_expansion_factor", player_time_expansion_factor);
			yamlMap.optFunList("player_static_lines_frequency", yamlList -> player_static_lines_frequency = yamlList.asFloatArray());
			player_fft_cutoff_lower_frequency = yamlMap.optDouble("player_fft_cutoff_lower_frequency", player_fft_cutoff_lower_frequency);
			player_fft_cutoff_upper_frequency = yamlMap.optDouble("player_fft_cutoff_upper_frequency", player_fft_cutoff_upper_frequency);		
			player_mouse_move_factor = yamlMap.optDouble("player_mouse_move_factor", player_mouse_move_factor);
			detail_fft_window_overlap_percent = yamlMap.optDouble("detail_fft_window_overlap_percent", detail_fft_window_overlap_percent);
		}
	}

	public AudioProfile(Builder builder) {
		player_spectrum_threshold = builder.player_spectrum_threshold;
		player_fft_intensity_max = builder.player_fft_intensity_max;
		player_playbackRate = builder.player_playbackRate;
		player_preservesPitch = builder.player_preservesPitch;
		player_overwriteSamplingRate = builder.player_overwriteSamplingRate;
		player_samplingRate = builder.player_samplingRate;
		player_fft_window = builder.player_fft_window;
		player_fft_window_step_factor = builder.player_fft_window_step_factor;
		player_spectrum_shrink_Factor = builder.player_spectrum_shrink_Factor;
		player_time_expansion_factor = builder.player_time_expansion_factor;
		player_static_lines_frequency = builder.player_static_lines_frequency;
		player_fft_cutoff_lower_frequency = builder.player_fft_cutoff_lower_frequency;
		player_fft_cutoff_upper_frequency = builder.player_fft_cutoff_upper_frequency;		
		player_mouse_move_factor = builder.player_mouse_move_factor;
		detail_fft_window_overlap_percent = builder.detail_fft_window_overlap_percent;
	}
}
