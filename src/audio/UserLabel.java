package audio;

import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class UserLabel {
	public final String name;	
	public final String creator;	// optional ""
	public final String creation_date; // optional ""	
	
	public UserLabel(String name, String creator, String creation_date) {
		this.name = name;
		this.creator = creator;
		this.creation_date = creation_date;
	}

	/**
	 * 
	 * @param jsonLabel
	 * @return null if name == "unknown"
	 */
	public static UserLabel ofJSON(JSONObject jsonLabel) {		
		String name = JsonUtil.getString(jsonLabel, "name");
		if(name.equals("unknown")) {
			return null;
		}
		String creator = JsonUtil.optString(jsonLabel, "creator", "");
		String creation_date = JsonUtil.optString(jsonLabel, "creation_date", "");
		return new UserLabel(name, creator, creation_date);
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("name");
		json.value(name);
		JsonUtil.writeOpt(json, "creator", creator);
		JsonUtil.writeOpt(json, "creation_date", creation_date);
		json.endObject();		
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", name);
		YamlUtil.optPut(map, "creator", creator);
		YamlUtil.optPut(map, "creation_date", creation_date);
		return map;
	}

	/**
	 * 
	 * @param yamlMap
	 * @return null if name == "unknown"
	 */
	public static UserLabel ofYAML(YamlMap yamlMap) {
		String name = yamlMap.getString("name");
		if(name.equals("unknown")) {
			return null;
		}
		String creator = yamlMap.optString("creator", "");
		String creation_date = yamlMap.optString("creation_date", "");
		return new UserLabel(name, creator, creation_date);
	}
	
	public String name() {
		return name;
	}

	public UserLabel withCreator(String username, String date) {
		return new UserLabel(this.name, username, date);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creation_date == null) ? 0 : creation_date.hashCode());
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserLabel other = (UserLabel) obj;
		if (creation_date == null) {
			if (other.creation_date != null)
				return false;
		} else if (!creation_date.equals(other.creation_date))
			return false;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
