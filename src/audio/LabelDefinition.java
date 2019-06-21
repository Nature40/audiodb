package audio;

import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;

public class LabelDefinition {
	public final String name;
	public final String desc;
	
	public LabelDefinition(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}
	
	public static LabelDefinition ofYAML(YamlMap yamlMap) {
		String name = yamlMap.getString("name");
		String desc = yamlMap.optString("desc", "");
		return new LabelDefinition(name, desc);
	}
	
	public static LabelDefinition ofJSON(JSONObject jsonLabelDefinition) {		
		String name = JsonUtil.getString(jsonLabelDefinition, "name");
		String desc = JsonUtil.optString(jsonLabelDefinition, "desc", "");
		return new LabelDefinition(name, desc);
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", name);
		map.put("desc", desc);
		return map;
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("name");
		json.value(name);
		json.key("desc");
		json.value(desc);
		json.endObject();		
	}
}
