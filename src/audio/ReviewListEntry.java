package audio;

import java.util.LinkedHashMap;
import java.util.function.Predicate;

import util.yaml.YamlMap;

public class ReviewListEntry {
	
	public final String sample_id;
	public final double label_start;
	public final double label_end;
	public final String label_name;
	public final boolean classified;
	
	public static Predicate<ReviewListEntry> getKeyFunc(String sample_id, String label_name, double label_start, double label_end) {
		return e -> e.sample_id.equals(sample_id) && e.label_name.equals(label_name) && e.isInterval(label_start, label_end);
	}
	
	public ReviewListEntry(String sample_id, double label_start, double label_end, String label_name, boolean classified) {
		this.sample_id = sample_id;
		this.label_start = label_start;
		this.label_end = label_end;
		this.label_name = label_name;
		this.classified = classified;
	}
	
	public ReviewListEntry withClassified(boolean classified) {
		return new ReviewListEntry(sample_id, label_start, label_end, label_name, classified);
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("sample_id", sample_id);
		map.put("label_start", label_start);
		map.put("label_end", label_end);
		map.put("label_name", label_name);
		if(classified) {
			map.put("classified", true);	
		}
		return map;
	}

	public static ReviewListEntry ofYAML(YamlMap yamlMap) {
		String sample_id = yamlMap.getString("sample_id");
		double start = yamlMap.contains("label_start") ? yamlMap.getDouble("label_start") : yamlMap.getDouble("sample_start"); // check for old version names sample_start
		double end = yamlMap.contains("label_end") ? yamlMap.getDouble("label_end") : yamlMap.getDouble("sample_end"); // check for old version names sample_end
		String label_name = yamlMap.getString("label_name");
		boolean classified = yamlMap.optBoolean("classified", false);
		return new ReviewListEntry(sample_id, start, end, label_name, classified);
	}
	
	public boolean isInterval(double start, double end) {
		return (start - 0.001d) <= label_start && label_start <= (start + 0.001d) && (end - 0.001d) <= label_end && label_end <= (end + 0.001d);
	}
}