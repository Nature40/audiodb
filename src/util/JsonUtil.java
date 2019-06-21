package util;

import org.json.JSONObject;

public class JsonUtil {
	
	public static String getString(JSONObject json, String name) {
		return json.get(name).toString();
	}
	
	public static String optString(JSONObject json, String name, String def) {	
		Object value = json.opt(name);		
		return value == null ? def : value.toString();
	}

}
