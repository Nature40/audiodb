package photo2;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.yaml.YamlMap;

public final class PhotoProjectConfig {
	private static final Logger log = LogManager.getLogger();
	
	private static final PhotoProjectConfig DEFAULT = new PhotoProjectConfig();
	
	public final String project;
	public final Path root_path;
	
	private PhotoProjectConfig() {
		project = null; // project name is needed
		root_path = null; // root_path is needed
	}
	
	public PhotoProjectConfig(String project, Path root_path) {
		this.project = project;
		this.root_path = root_path;
	}
	
	public static PhotoProjectConfig ofYAML(YamlMap yamlMap) {
		String project = yamlMap.getString("project");
		Path root_path = Paths.get(yamlMap.getString("root_path"));
		return new PhotoProjectConfig(project, root_path);
	}
}
