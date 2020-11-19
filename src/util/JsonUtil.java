package util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import audio.GeneratorLabel;
import audio.Label;
import photo.Tag;
import util.collections.ReadonlyList;
import util.collections.vec.Vec;

public class JsonUtil {

	public static String getString(JSONObject json, String name) {
		return json.get(name).toString();
	}

	public static String optString(JSONObject json, String name, String def) {	
		Object value = json.opt(name);		
		return value == null ? def : value.toString();
	}

	public static String[] getStrings(JSONObject json, String name) {
		JSONArray jsonArray = json.getJSONArray(name);
		int jsonArrayLen = jsonArray.length();
		String[] items = new String[jsonArrayLen];
		for (int i = 0; i < jsonArrayLen; i++) {
			items[i] = jsonArray.get(i).toString();
		}
		return items;
	}

	public static String[] optStrings(JSONObject json, String name) {
		JSONArray jsonArray = json.optJSONArray(name);
		if(jsonArray == null) {
			return new String[] {};
		}
		int jsonArrayLen = jsonArray.length();
		String[] items = new String[jsonArrayLen];
		for (int i = 0; i < jsonArrayLen; i++) {
			items[i] = jsonArray.get(i).toString();
		}
		return items;
	}

	public static <T> Vec<T> optVec(JSONObject json, String name, Function<JSONObject, T> parser) {
		Vec<T> vec = new Vec<T>();
		JSONArray jsonArray = json.optJSONArray(name);
		if(jsonArray != null) {
			int jsonArrayLen = jsonArray.length();
			for (int i = 0; i < jsonArrayLen; i++) {
				JSONObject value = jsonArray.getJSONObject(i);
				T element = parser.apply(value);
				if(element != null) {
					vec.add(element);
				}
			}	
		}
		return vec;
	}

	public static void writeOpt(JSONWriter json, String name, String value) {
		if(value != null && !value.isEmpty()) {
			json.key(name);
			json.value(value);
		}
	}

	public static void writeOpt(JSONWriter json, String name, double value) {
		if(Double.isFinite(value)) {
			json.key(name);
			json.value(value);
		}
	}

	public static void writeOpt(JSONWriter json, String name, Object o) {
		if(o != null) {
			String value = o.toString();
			if(!value.isEmpty()) {
				json.key(name);
				json.value(value);
			}
		}	
	}

	public static <T> void writeArray(JSONWriter json, Iterable<T> iterable, BiConsumer<T, JSONWriter> writer) {
		json.array();
		if(iterable != null) {
			iterable.forEach(e -> writer.accept(e, json));
		}
		json.endArray();
	}

	public static <T> void writeArray(JSONWriter json, String name, Iterable<T> iterable, BiConsumer<T, JSONWriter> writer) {
		json.key(name);
		writeArray(json, iterable, writer);
	}

	public static void write(HttpServletResponse response, Consumer<JSONWriter> writer) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		writer.accept(json);
		json.endObject();
	}

	public static LocalDateTime optLocalDateTime(JSONObject json, String name, LocalDateTime def) {
		Object value = json.opt(name);		
		return value == null ? def :  LocalDateTime.parse(value.toString());
	}

	public static void writeOpt(JSONWriter json, String name, boolean opt, long timestamp) {
		if(opt) {
			json.key(name);
			json.value(timestamp);
		}
	}

	public static <E> void writeOpt(JSONWriter json, String name, ReadonlyList<E> list, BiConsumer<E, JSONWriter> fun) {
		if(list.isEmpty()) {
			return;
		}
		json.key(name);
		json.array();
		for(E e : list) {
			fun.accept(e, json);
		}
		json.endArray();
	}





}
