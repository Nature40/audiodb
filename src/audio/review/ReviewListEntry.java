package audio.review;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.function.Predicate;

import audio.review.ReviewedLabel.Reviewed;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

@Deprecated
public class ReviewListEntry {
	
	public final String sample_id;
	public final double label_start;
	public final double label_end;
	public final String label_name;
	public final boolean classified;
	public final Reviewed latest_review; // nullable
	public final boolean missing_sample;
	
	public static final Comparator<ReviewListEntry> COMPARATOR = (a,b) -> {
		int c = a.sample_id.compareTo(b.sample_id);
		if(c != 0) {
			return c;
		}		
		c = Double.compare(a.label_start, b.label_start);
		if(c != 0) {
			return c;
		}
		c = Double.compare(a.label_end, b.label_end);
		if(c != 0) {
			return c;
		}
		c = Boolean.compare(a.classified, b.classified);
		if(c != 0) {
			return c;
		}
		if(a.latest_review == null) {
			return b.latest_review == null ? 0 : 1;
		} else {
			return b.latest_review == null ? -1 : a.latest_review.compareTo(b.latest_review);
		}
	};
	
	public static Predicate<ReviewListEntry> getKeyFunc(String sample_id, String label_name, double label_start, double label_end) {
		return e -> e.sample_id.equals(sample_id) && e.label_name.equals(label_name) && e.isInterval(label_start, label_end);
	}
	
	public ReviewListEntry(String sample_id, double label_start, double label_end, String label_name, boolean classified, Reviewed latest_review, boolean missing_sample) {
		this.sample_id = sample_id;
		this.label_start = label_start;
		this.label_end = label_end;
		this.label_name = label_name;
		this.classified = classified;
		this.latest_review = latest_review;
		this.missing_sample = missing_sample;
	}
	
	public ReviewListEntry withClassifiedAndReviewed(boolean classified, Reviewed latest_review) {
		return new ReviewListEntry(sample_id, label_start, label_end, label_name, classified, latest_review, missing_sample);
	}
	
	public ReviewListEntry withMissingSample(boolean missing_sample) {
		return new ReviewListEntry(sample_id, label_start, label_end, label_name, classified, latest_review, missing_sample);
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
		if(latest_review != null) {
			YamlUtil.put(map, "latest_review", latest_review.toString());
		}
		return map;
	}

	public static ReviewListEntry ofYAML(YamlMap yamlMap) {
		String sample_id = yamlMap.getString("sample_id");
		double start = yamlMap.contains("label_start") ? yamlMap.getDouble("label_start") : yamlMap.getDouble("sample_start"); // check for old version names sample_start
		double end = yamlMap.contains("label_end") ? yamlMap.getDouble("label_end") : yamlMap.getDouble("sample_end"); // check for old version names sample_end
		String label_name = yamlMap.getString("label_name");
		boolean classified = yamlMap.optBoolean("classified", false);
		Reviewed latest_review = yamlMap.opt("latest_review", Reviewed::parse);
		boolean missing_sample = false;
		return new ReviewListEntry(sample_id, start, end, label_name, classified, latest_review, missing_sample);
	}
	
	public boolean isInterval(double start, double end) {
		return (start - 0.001d) <= label_start && label_start <= (start + 0.001d) && (end - 0.001d) <= label_end && label_end <= (end + 0.001d);
	}

	@Override
	public String toString() {
		return "ReviewListEntry [sample_id=" + sample_id + ", label_start=" + label_start + ", label_end=" + label_end
				+ ", label_name=" + label_name + ", classified=" + classified + ", latest_review=" + latest_review
				+ "]";
	}
}