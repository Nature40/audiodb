package audio.labeling;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.function.Predicate;

import util.yaml.YamlMap;

public class LabelingListEntry {
	
	public final String sample_id;
	public final double label_start;
	public final double label_end;
	public final boolean labeled;
	
	public static final Comparator<LabelingListEntry> COMPARATOR = (a,b) -> {
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
		return Boolean.compare(a.labeled, b.labeled);
	};
	
	public static Predicate<LabelingListEntry> getKeyFunc(String sample_id, double label_start, double label_end) {
		return e -> e.sample_id.equals(sample_id) && e.isInterval(label_start, label_end);
	}
	
	public LabelingListEntry(String sample_id, double label_start, double label_end, boolean labeled) {
		this.sample_id = sample_id;
		this.label_start = label_start;
		this.label_end = label_end;
		this.labeled = labeled;
	}
	
	public LabelingListEntry withLabeled(boolean labeled) {
		return new LabelingListEntry(sample_id, label_start, label_end, labeled);
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("sample_id", sample_id);
		map.put("label_start", label_start);
		map.put("label_end", label_end);
		map.put("labeled", labeled);
		return map;
	}

	public static LabelingListEntry ofYAML(YamlMap yamlMap) {
		String sample_id = yamlMap.getString("sample_id");
		double start = yamlMap.getDouble("label_start");
		double end = yamlMap.getDouble("label_end");
		boolean labeled = yamlMap.optBoolean("labeled", false);
		return new LabelingListEntry(sample_id, start, end, labeled);
	}
	
	public boolean isInterval(double start, double end) {
		return (start - 0.001d) <= label_start && label_start <= (start + 0.001d) && (end - 0.001d) <= label_end && label_end <= (end + 0.001d);
	}

	@Override
	public String toString() {
		return "LabelingListEntry [sample_id=" + sample_id + ", label_start=" + label_start + ", label_end=" + label_end
				+ ", labeled=" + labeled + "]";
	}
}