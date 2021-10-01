package audio;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.yaml.YamlMap;

public final class AudioProjectConfig {
	private static final Logger log = LogManager.getLogger();
	
	public static final AudioProjectConfig DEFAULT = new AudioProjectConfig(new Builder());
	
	public final String project;
	public final Path root_path;
	public final Path device_inventory_file; // nullable
	public final double player_spectrum_threshold;
	public final double player_playbackRate;
	public final boolean player_preservesPitch;
	public final boolean player_overwriteSamplingRate;
	public final double player_samplingRate;
	
	public static class Builder {
		public String project = "default_project"; // project name is needed
		public Path root_path = Paths.get("data"); // root_path is needed
		public Path device_inventory_file = null;  // nullable
		public double player_spectrum_threshold = 13.5;
		public double player_playbackRate = 1;
		public boolean player_preservesPitch = true;
		public boolean player_overwriteSamplingRate = false;
		public double player_samplingRate = 32000;
		
		public Builder() {}
		
		public Builder(YamlMap yamlMap) {
			project = yamlMap.optString("project", project);
			yamlMap.optFunString("root_path", s -> root_path = Paths.get(s));
			yamlMap.optFunString("device_inventory_file", s -> device_inventory_file = Paths.get(s));
			player_spectrum_threshold = yamlMap.optDouble("player_spectrum_threshold", player_spectrum_threshold);
			player_playbackRate = yamlMap.optDouble("player_playbackRate", player_playbackRate);
			player_overwriteSamplingRate = yamlMap.optBoolean("player_overwriteSamplingRate", player_overwriteSamplingRate);
			player_samplingRate = yamlMap.optDouble("player_samplingRate", player_samplingRate);
		}
	}
	
	public AudioProjectConfig(Builder builder) {
		project = builder.project;
		root_path = builder.root_path;
		device_inventory_file = builder.device_inventory_file;
		player_spectrum_threshold = builder.player_spectrum_threshold;
		player_playbackRate = builder.player_playbackRate;
		player_preservesPitch = builder.player_preservesPitch;
		player_overwriteSamplingRate = builder.player_overwriteSamplingRate;
		player_samplingRate = builder.player_samplingRate;
		log.info(this);
	}

	@Override
	public String toString() {
		return "AudioProjectConfig [project=" + project + ", root_path=" + root_path + ", device_inventory_file="
				+ device_inventory_file + ", player_spectrum_threshold=" + player_spectrum_threshold
				+ ", player_playbackRate=" + player_playbackRate + ", player_preservesPitch=" + player_preservesPitch
				+ ", player_overwriteSamplingRate=" + player_overwriteSamplingRate + ", player_samplingRate="
				+ player_samplingRate + "]";
	}
}
