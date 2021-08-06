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
	public final Path classification_definition_csv; // nullable
	public final Path review_list_path; // nullable
	
	private PhotoProjectConfig() {
		project = null; // project name is needed
		root_path = null; // root_path is needed
		classification_definition_csv = null; // optional
		review_list_path = null; // optional
	}
	
	public PhotoProjectConfig(String project, Path root_path, Path classification_definition_csv, Path review_list_path) {
		this.project = project;
		this.root_path = root_path;
		this.classification_definition_csv = classification_definition_csv;
		this.review_list_path = review_list_path;
	}
	
	public static PhotoProjectConfig ofYAML(YamlMap yamlMap) {
		String project = yamlMap.getString("project");
		Path root_path = Paths.get(yamlMap.getString("root_path"));
		Path classification_definition_csv = yamlMap.contains("classification_definition_csv") ? Paths.get(yamlMap.getString("classification_definition_csv")) : null;
		Path review_list_path = yamlMap.contains("review_list_path") ? Paths.get(yamlMap.getString("review_list_path")) : null;

		return new PhotoProjectConfig(project, root_path, classification_definition_csv, review_list_path);
	}
}
