package audio;

import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class SampleUserLocked {
	public static final String CREATOR_UNKNOWN = "unknown";
	public static final long TIMESTAMP_UNKNOWN = 0;

	public final String creator;
	public final long timestamp;

	public SampleUserLocked(String creator, long timestamp) {
		this.creator = creator;
		this.timestamp = timestamp;
	}

	public static SampleUserLocked ofJSON(JSONObject json) {		
		String creator = JsonUtil.optString(json, "creator", CREATOR_UNKNOWN);
		long timestamp = JsonUtil.optLong(json, "timestamp", TIMESTAMP_UNKNOWN);
		return new SampleUserLocked(creator, timestamp);
	}

	public static SampleUserLocked ofYAML(YamlMap yamlMap) {
		String creator = yamlMap.optString("creator", CREATOR_UNKNOWN);		
		long timestamp = yamlMap.optLong("timestamp", TIMESTAMP_UNKNOWN);
		return new SampleUserLocked(creator, timestamp);
	}

	public void toJSON(JSONWriter json) {
		json.object();
		JsonUtil.write(json, "creator", creator);
		JsonUtil.write(json, "timestamp", timestamp);
		json.endObject();		
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		YamlUtil.put(map, "creator", creator);
		YamlUtil.put(map, "timestamp", timestamp);
		return map;
	}
}
