package audio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Label {
	public final double start;
	public final double end;	
	public final Vec<GeneratorLabel> generatorLabels;
	public final Vec<UserLabel> userLabels;
	
	public Label(double start, double end, Vec<GeneratorLabel> generatorLabels, Vec<UserLabel> userLabels) {
		this.start = start;
		this.end = end;
		this.generatorLabels = generatorLabels;
		this.userLabels = userLabels;
	}

	public static Label ofJSON(JSONObject jsonLabel) {		
		double start = jsonLabel.getDouble("start");
		double end = jsonLabel.getDouble("end");		
		Vec<GeneratorLabel> generatorLabels = JsonUtil.optVec(jsonLabel, "generated_labels", GeneratorLabel::ofJSON);
		Vec<UserLabel> userLabels = JsonUtil.optVec(jsonLabel, "labels", UserLabel::ofJSON);
		return new Label(start, end, generatorLabels, userLabels);
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("start");
		json.value(start);
		json.key("end");
		json.value(end);		
		JsonUtil.writeArray(json, "generated_labels", generatorLabels, GeneratorLabel::toJSON);
		JsonUtil.writeArray(json, "labels", userLabels, UserLabel::toJSON);
		json.endObject();		
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("start", start);
		map.put("end", end);
		YamlUtil.putArray(map, "generated_labels", generatorLabels, GeneratorLabel::toMap);
		YamlUtil.putArray(map, "labels", userLabels, UserLabel::toMap);
		return map;
	}

	public static Label ofYAML(YamlMap yamlMap) {
		double start = yamlMap.getDouble("start");
		double end = yamlMap.getDouble("end");		
		Vec<GeneratorLabel> generatorLabels = YamlUtil.optVec(yamlMap, "generated_labels", GeneratorLabel::ofYAML);
		Vec<UserLabel> userLabels = YamlUtil.optVec(yamlMap, "labels", UserLabel::ofYAML);
		return new Label(start, end, generatorLabels, userLabels);
	}

	public String[] getGeneratorLabelNames() {
		return generatorLabels.mapArray(String[]::new, GeneratorLabel::name);
	}
	
	public String[] getUserLabelNames() {
		return userLabels.mapArray(String[]::new, UserLabel::name);
	}

	public Label withCreator(String username, String date) {
		Object ul = this.userLabels.map(userLabel -> userLabel.withCreator(username, date));
		return new Label(this.start, this.end, this.generatorLabels, this.userLabels);
	}
}
