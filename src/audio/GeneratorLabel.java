package audio;

import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class GeneratorLabel {
	public final String name;	
	public final double reliability; // optional NaN	
	public final String generator;	// optional ""
	public final String model_version;	// optional ""
	public final String generation_date; // optional ""	
	
	public GeneratorLabel(String name, double reliability, String generator, String model_version, String generation_date) {
		this.name = name;
		this.reliability = reliability;
		this.generator = generator;
		this.model_version = model_version;
		this.generation_date = generation_date;
	}

	public static GeneratorLabel ofJSON(JSONObject jsonLabel) {		
		String name = JsonUtil.getString(jsonLabel, "name");
		double reliability = jsonLabel.optDouble("reliability");
		String generator = JsonUtil.optString(jsonLabel, "generator", "");
		String model_version = JsonUtil.optString(jsonLabel, "model_version", "");
		String generation_date = JsonUtil.optString(jsonLabel, "generation_date", "");
		return new GeneratorLabel(name, reliability, generator, model_version, generation_date);
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("name");
		json.value(name);
		JsonUtil.writeOpt(json, "reliability", reliability);
		JsonUtil.writeOpt(json, "generator", generator);
		JsonUtil.writeOpt(json, "model_version", model_version);
		JsonUtil.writeOpt(json, "generation_date", generation_date);
		json.endObject();		
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", name);
		YamlUtil.optPut(map, "reliability", reliability);
		YamlUtil.optPut(map, "generator", generator);
		YamlUtil.optPut(map, "model_version", model_version);
		YamlUtil.optPut(map, "generation_date", generation_date);
		return map;
	}

	public static GeneratorLabel ofYAML(YamlMap yamlMap) {
		String name = yamlMap.getString("name");
		double reliability = yamlMap.optDouble("reliability");
		String generator = yamlMap.optString("generator", "");
		String model_version = yamlMap.optString("model_version", "");
		String generation_date = yamlMap.optString("generation_date", "");
		return new GeneratorLabel(name, reliability, generator, model_version, generation_date);
	}
	
	public String name() {
		return name;
	}
}
