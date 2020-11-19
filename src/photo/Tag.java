package photo;

import java.util.LinkedHashMap;

import org.json.JSONWriter;

import util.yaml.YamlMap;

public class Tag {	
	public final String name;

	public Tag(String name) {
		this.name = name;
	}
	
	public static Tag ofYAML(YamlMap yamlMap) {
		String name = yamlMap.getString("name");
		return new Tag(name);
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", name);		
		return map;
	}
	
	public void toJSON(JSONWriter json) {
		json.object();
		json.key("name");
		json.value(name);
		json.endObject();		
	}
}
