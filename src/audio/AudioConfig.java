package audio;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import util.yaml.YamlMap;

public class AudioConfig {

	public final Map<String, AudioProjectConfig> projectMap;

	private AudioConfig(HashMap<String, AudioProjectConfig> projectMap) {
		this.projectMap = Collections.unmodifiableMap(projectMap);
	}

	public static AudioConfig ofYAML(YamlMap yamlMap) {
		HashMap<String, AudioProjectConfig> pm = new LinkedHashMap<String, AudioProjectConfig>();
		for(YamlMap m : yamlMap.optList("projects").asMaps()) {
			AudioProjectConfig audioProjectConfig = new AudioProjectConfig(new AudioProjectConfig.Builder(m));
			if(audioProjectConfig.project == null) {
				throw new RuntimeException("missing project name in audio config");
			}
			if(pm.containsKey(audioProjectConfig.project)) {
				throw new RuntimeException("dublicate project name");
			}
			pm.put(audioProjectConfig.project, audioProjectConfig);
		}
		return new AudioConfig(pm);
	}
}
