package photo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import util.yaml.YamlMap;

public class PhotoConfig {

	public final Map<String, PhotoProjectConfig> projectMap;

	private PhotoConfig(HashMap<String, PhotoProjectConfig> projectMap) {
		this.projectMap = Collections.unmodifiableMap(projectMap);
	}

	public static PhotoConfig ofYAML(YamlMap yamlMap) {
		HashMap<String, PhotoProjectConfig> pm = new LinkedHashMap<String, PhotoProjectConfig>();
		for(YamlMap m : yamlMap.optList("projects").asMaps()) {	
			PhotoProjectConfig photoProjectConfig = new PhotoProjectConfig(new PhotoProjectConfig.Builder(m));			
			
			if(photoProjectConfig.project == null) {
				throw new RuntimeException("missing project name in photo config");
			}
			if(pm.containsKey(photoProjectConfig.project)) {
				throw new RuntimeException("dublicate project name");
			}
			pm.put(photoProjectConfig.project, photoProjectConfig);
		}
		return new PhotoConfig(pm);
	}
}
