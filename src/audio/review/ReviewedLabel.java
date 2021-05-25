package audio.review;

import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class ReviewedLabel {
	public static enum Reviewed {
		no,
		unsure,
		yes;

		public static Reviewed parse(String text) {
			String s = text.toLowerCase().strip();
			switch(s) {
			case "no":
				return no;
			case "unsure":
				return unsure;
			case "yes":
				return yes;				
			default:
				throw new RuntimeException("unknown reviewed type: " + s);
			}
		}

		@Override
		public String toString() {
			switch(this) {
			case no:
				return "no";
			case unsure:
				return "unsure";
			case yes:
				return "yes";			
			default:
				throw new RuntimeException("unknown reviewed type: " + this);
			}
		}
	}

	public static final String REVIEWER_UNKNOWN = "unknown";
	public static final long TIMESTAMP_UNKNOWN = 0;

	public final String name;
	public final Reviewed reviewed;
	public final String reviewer;
	public final long timestamp;

	public ReviewedLabel(String name, Reviewed reviewed, String reviewer, long timestamp) {
		this.name = name;
		this.reviewed = reviewed;
		this.reviewer = reviewer;
		this.timestamp = timestamp;
	}

	public static ReviewedLabel ofJSON(JSONObject json) {		
		String name = JsonUtil.getString(json, "name");
		Reviewed reviewed = JsonUtil.getString(json, "reviewed", Reviewed::parse);
		String reviewer = JsonUtil.optString(json, "reviewer", REVIEWER_UNKNOWN);
		long timestamp = JsonUtil.optLong(json, "timestamp", TIMESTAMP_UNKNOWN);
		return new ReviewedLabel(name, reviewed, reviewer, timestamp);
	}

	public static ReviewedLabel ofYAML(YamlMap yamlMap) {
		String name = yamlMap.getString("name");
		Reviewed reviewed = yamlMap.get("reviewed", Reviewed::parse);
		String reviewer = yamlMap.optString("reviewer", REVIEWER_UNKNOWN);		
		long timestamp = yamlMap.optLong("timestamp", TIMESTAMP_UNKNOWN);
		return new ReviewedLabel(name, reviewed, reviewer, timestamp);
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("name");
		json.value(name);
		JsonUtil.write(json, "reviewed", reviewed.toString());
		JsonUtil.write(json, "reviewer", reviewer);
		JsonUtil.write(json, "timestamp", timestamp);
		json.endObject();		
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", name);
		YamlUtil.put(map, "reviewed", reviewed.toString());
		YamlUtil.put(map, "reviewer", reviewer);
		YamlUtil.put(map, "timestamp", timestamp);
		return map;
	}

	public ReviewedLabel withReviewer(String reviewer, long timestamp) {
		return new ReviewedLabel(this.name, this.reviewed, reviewer, timestamp);
	}
	
	public boolean isYes() {
		return this.reviewed == Reviewed.yes;
	}
}
