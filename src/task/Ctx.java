package task;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import audio.Account;
import audio.Broker;

public class Ctx {

	public final Descriptor descriptor;
	public final JSONObject json;
	public final String id;
	public final Broker broker;
	public final Account account;
	private HashMap<String, String> paramMap;

	public Ctx(Descriptor descriptor, JSONObject json, String id, Broker broker, Account account) {
		this.descriptor = descriptor;
		this.json = json;
		this.id = id;
		this.broker = broker;
		this.account = account;
		if(json.has("params")) {
			JSONArray jsonParams = json.getJSONArray("params");
			HashMap<String, String> paramMap = new HashMap<String, String>();
			for (int i = 0; i < jsonParams.length(); i++) {
				JSONObject jsonParam = jsonParams.getJSONObject(i);
				String paramName = jsonParam.getString("param");
				String paramValue = jsonParam.getString("value");
				paramMap.put(paramName, paramValue);
			}
			this.paramMap = paramMap;
		}
	}
	
	public boolean getParamBoolean(String param) {
		String value = paramMap.get(param);
		if(value == null) {
			throw new RuntimeException("Param not found: " + param);
		}
		if(value.equals("TRUE")) {
			return true;
		} else if(value.equals("FALSE")) {
			return false;
		} 
		throw new RuntimeException("Param value not boolean: " + param + "  /  " + value);
	}
	public String getParamString(String param) {
		String value = paramMap.get(param);
		if(value == null) {
			throw new RuntimeException("Param not found: " + param);
		}
		return value;
	}
}