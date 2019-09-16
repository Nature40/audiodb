package audio;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Label {	
	public final double start;
	public final double end;
	public final String[] labels;
	public final String comment;
	public final String creator;
	public final LocalDateTime creation_date;

	public Label(double start, double end, String[] labels, String comment, String creator, LocalDateTime creation_date) {
		this.start = start;
		this.end = end;
		this.labels = labels;
		this.comment = comment;
		this.creator = creator;
		this.creation_date = creation_date;
	}

	public static Label ofJSON(JSONObject jsonLabel) {		
		double start = jsonLabel.getDouble("start");
		double end = jsonLabel.getDouble("end");
		String[] labels = JsonUtil.getStrings(jsonLabel, "labels");	
		String comment = JsonUtil.optString(jsonLabel, "comment", "");
		String creator = JsonUtil.optString(jsonLabel, "creator", "");
		LocalDateTime creation_date = JsonUtil.optLocalDateTime(jsonLabel, "creation_date", LocalDateTime.MAX);
		return new Label(start, end, labels, comment, creator, creation_date);
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("start");
		json.value(start);
		json.key("end");
		json.value(end);
		json.key("labels");
		json.value(labels);
		JsonUtil.writeOpt(json, "comment", comment);
		JsonUtil.writeOpt(json, "creator", creator);
		JsonUtil.writeOpt(json, "creation_date", creation_date);
		json.endObject();		
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("start", start);
		map.put("end", end);
		map.put("labels", labels);
		YamlUtil.optPut(map, "comment", comment);
		YamlUtil.optPut(map, "creator", creator);
		Date date = Date.from( creation_date.atZone( ZoneId.systemDefault()).toInstant());
		map.put("creation_date", date);
		return map;
	}
	
	private static final LocalDateTime DEFAULT_TIMESTAMP = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault()).toLocalDateTime();
	
	public static Label ofYAML(YamlMap yamlMap) {
		double start = yamlMap.getDouble("start");
		double end = yamlMap.getDouble("end");
		String[] labels = yamlMap.getList("labels").asStringArray();
		String comment = yamlMap.optString("comment", "");
		String creator = yamlMap.optString("creator", "");
		Object creation_date = yamlMap.optObject("creation_date", DEFAULT_TIMESTAMP);
		LocalDateTime ldt = dateConvert(creation_date);
		return new Label(start, end, labels, comment, creator, ldt);
	}
	
	private static LocalDateTime dateConvert(Object o) {
		if(o instanceof LocalDateTime) {
			return (LocalDateTime) o;
		}
		if(o instanceof Date) {
			long timeMillis = ((Date) o).getTime();
			LocalDateTime ldt = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
			return ldt;
		}
		throw new RuntimeException("unknown timestamp: " + o.getClass().toString());
	}

	public Label withCreator(String creator, LocalDateTime creation_date) {
		return new Label(this.start, this.end, this.labels, this.comment, creator, creation_date);
	}

}
