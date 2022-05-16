package audio;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.tinylog.Logger;

import util.yaml.YamlMap;

public final class AudioProjectConfig {

	public static final AudioProjectConfig DEFAULT = new AudioProjectConfig(new Builder());

	public final String project;
	public final Path root_path;
	public final Path root_data_path;
	public final Path device_inventory_file; // nullable
	public final double player_spectrum_threshold;
	public final double player_fft_intensity_max;
	public final double player_playbackRate; // obsolete
	public final boolean player_preservesPitch;
	public final boolean player_overwriteSamplingRate;
	public final double player_samplingRate;
	public final int player_fft_window;
	public final double player_fft_window_step_factor;	
	public final int player_time_expansion_factor;
	public final float[] player_static_lines_frequency;
	public final double player_fft_cutoff_lower_frequency;
	public final double player_fft_cutoff_upper_frequency;
	public final int audio_cache_max_files;

	public static class Builder {
		public String project = "default_project"; // project name is needed
		public Path root_path = Paths.get("data"); // root_path is needed
		public Path root_data_path = null; // optional; if null -> root_path is used as data file directory
		public Path device_inventory_file = null;  // nullable
		public double player_spectrum_threshold = 13.5;
		public double player_fft_intensity_max = 23;
		public double player_playbackRate = 1;
		public boolean player_preservesPitch = true;
		public boolean player_overwriteSamplingRate = false;
		public double player_samplingRate = 32000;
		public int player_fft_window = 1024; //16384,//8192,//4096,//2048,//1024,
		public double player_fft_window_step_factor = 1d;
		public int player_time_expansion_factor = 1;
		public float[] player_static_lines_frequency = null;
		public double player_fft_cutoff_lower_frequency = 0;
		public double player_fft_cutoff_upper_frequency = 192000;
		public int audio_cache_max_files = 20;

		public Builder() {}

		public Builder(YamlMap yamlMap) {
			project = yamlMap.optString("project", project);
			yamlMap.optFunString("root_path", s -> root_path = Paths.get(s));
			yamlMap.optFunString("root_data_path", s -> root_data_path = Paths.get(s));
			yamlMap.optFunString("device_inventory_file", s -> device_inventory_file = Paths.get(s));
			player_spectrum_threshold = yamlMap.optDouble("player_spectrum_threshold", player_spectrum_threshold);
			player_fft_intensity_max = yamlMap.optDouble("player_fft_intensity_max", player_fft_intensity_max);
			player_playbackRate = yamlMap.optDouble("player_playbackRate", player_playbackRate);
			player_overwriteSamplingRate = yamlMap.optBoolean("player_overwriteSamplingRate", player_overwriteSamplingRate);
			player_samplingRate = yamlMap.optDouble("player_samplingRate", player_samplingRate);
			player_fft_window = yamlMap.optInt("player_fft_window", player_fft_window);
			player_fft_window_step_factor = yamlMap.optDouble("player_fft_window_step_factor", player_fft_window_step_factor);
			player_time_expansion_factor = yamlMap.optInt("player_time_expansion_factor", player_time_expansion_factor);
			yamlMap.optFunList("player_static_lines_frequency", yamlList -> player_static_lines_frequency = yamlList.asFloatArray());
			player_fft_cutoff_lower_frequency = yamlMap.optDouble("player_fft_cutoff_lower_frequency", player_fft_cutoff_lower_frequency);
			player_fft_cutoff_upper_frequency = yamlMap.optDouble("player_fft_cutoff_upper_frequency", player_fft_cutoff_upper_frequency);			
			audio_cache_max_files = yamlMap.optInt("audio_cache_max_files", audio_cache_max_files);
		}
	}

	public AudioProjectConfig(Builder builder) {
		project = builder.project;
		root_path = builder.root_path;
		root_data_path = builder.root_data_path == null ? builder.root_path : builder.root_data_path;
		device_inventory_file = builder.device_inventory_file;
		player_spectrum_threshold = builder.player_spectrum_threshold;
		player_fft_intensity_max = builder.player_fft_intensity_max;
		player_playbackRate = builder.player_playbackRate;
		player_preservesPitch = builder.player_preservesPitch;
		player_overwriteSamplingRate = builder.player_overwriteSamplingRate;
		player_samplingRate = builder.player_samplingRate;
		player_fft_window = builder.player_fft_window;
		player_fft_window_step_factor = builder.player_fft_window_step_factor;
		player_time_expansion_factor = builder.player_time_expansion_factor;
		player_static_lines_frequency = builder.player_static_lines_frequency;
		player_fft_cutoff_lower_frequency = builder.player_fft_cutoff_lower_frequency;
		player_fft_cutoff_upper_frequency = builder.player_fft_cutoff_upper_frequency;
		audio_cache_max_files = builder.audio_cache_max_files;
		Logger.info(this);
	}

	@Override
	public String toString() {
		return "AudioProjectConfig [project=" + project + ", root_path=" + root_path + ", root_data_path="
				+ root_data_path + ", device_inventory_file=" + device_inventory_file + ", player_spectrum_threshold="
				+ player_spectrum_threshold + ", player_fft_intensity_max=" + player_fft_intensity_max
				+ ", player_playbackRate=" + player_playbackRate + ", player_preservesPitch=" + player_preservesPitch
				+ ", player_overwriteSamplingRate=" + player_overwriteSamplingRate + ", player_samplingRate="
				+ player_samplingRate + ", player_fft_window=" + player_fft_window + ", player_fft_window_step_factor="
				+ player_fft_window_step_factor + ", player_time_expansion_factor=" + player_time_expansion_factor
				+ ", player_static_lines_frequency=" + Arrays.toString(player_static_lines_frequency)
				+ ", player_fft_cutoff_lower_frequency=" + player_fft_cutoff_lower_frequency
				+ ", player_fft_cutoff_upper_frequency=" + player_fft_cutoff_upper_frequency
				+ ", audio_cache_max_files=" + audio_cache_max_files + "]";
	}
}
