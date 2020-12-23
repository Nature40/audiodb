package audio;

import java.util.LinkedHashMap;

import util.yaml.YamlMap;

public class ReviewListEntry {
	public final String sample_id;
	public final double sample_start;
	public final double sample_end;
	public final String label_name;
	
	public ReviewListEntry(String sample_id, double sample_start, double sample_end, String label_name) {
		this.sample_id = sample_id;
		this.sample_start = sample_start;
		this.sample_end = sample_end;
		this.label_name = label_name;
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("sample_id", sample_id);
		map.put("sample_start", sample_start);
		map.put("sample_end", sample_end);
		map.put("label_name", label_name);
		return map;
	}

	public static ReviewListEntry ofYAML(YamlMap yamlMap) {
		String sample_id = yamlMap.getString("sample_id");
		double start = yamlMap.getDouble("sample_start");
		double end = yamlMap.getDouble("sample_end");
		String label_name = yamlMap.getString("label_name");
		return new ReviewListEntry(sample_id, start, end, label_name);
	}
}