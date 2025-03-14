package photo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.tinylog.Logger;

import util.yaml.YamlMap;

public final class PhotoProjectConfig {
	
	public static final PhotoProjectConfig DEFAULT = new PhotoProjectConfig(new Builder());
	
	public final String project;
	public final Path root_path;
	public final Path root_data_path;
	public final Path classification_definition_csv; // nullable
	public final Path review_list_path; // nullable
	public final String[] original_path_keys; // nullable
	
	public static class Builder {
		public String project = "default_project"; // project name is needed
		public Path root_path = Paths.get("data"); // root_path is needed
		public Path root_data_path = null; // optional; if null -> root_path is used as data file directory
		public Path classification_definition_csv = null;  // nullable
		public Path review_list_path = null;  // nullable
		public String[] original_path_keys = null; // nullable
		
		public Builder() {}
		
		public Builder(YamlMap yamlMap) {
			project = yamlMap.optString("project", project);
			yamlMap.optFunString("root_path", s -> root_path = Paths.get(s));
			yamlMap.optFunString("root_data_path", s -> root_data_path = Paths.get(s));
			yamlMap.optFunString("classification_definition_csv", s -> classification_definition_csv = Paths.get(s));
			yamlMap.optFunString("review_list_path", s -> review_list_path = Paths.get(s));
			original_path_keys = yamlMap.optList("original_path_keys").asStringArray();
		}
	}
	
	public PhotoProjectConfig(Builder builder) {
		project = builder.project;
		root_path = builder.root_path;
		root_data_path = builder.root_data_path == null ? builder.root_path : builder.root_data_path;
		classification_definition_csv = builder.classification_definition_csv;
		review_list_path = builder.review_list_path;
		original_path_keys = builder.original_path_keys;
		Logger.info(this);
	}

	@Override
	public String toString() {
		return "PhotoProjectConfig [project=" + project + ", root_path=" + root_path + ", root_data_path="
				+ root_data_path + ", classification_definition_csv=" + classification_definition_csv
				+ ", review_list_path=" + review_list_path + ", original_path_keys="
				+ Arrays.toString(original_path_keys) + "]";
	}
}
