package util.yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import audio.LabelDefinition;
import util.collections.vec.Vec;

public class YamlUtil {
	
	public static YamlMap readYamlMap(Path path) {
		try(InputStream in = new FileInputStream(path.toFile())) {
			YamlMap yamlMap = YamlMap.ofObject(new Yaml().load(in));
			return yamlMap;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void writeSafeYamlMap(Path path, Map<String, Object> yamlMap) {		
		Path writepath = Paths.get(path.toString()+"_temp");
		try(FileWriter fileWriter = new FileWriter(writepath.toFile())){
			PrintWriter out = new PrintWriter(fileWriter);
			new Yaml().dump(yamlMap, out);
			out.close();
			Files.move(writepath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void writeSafe(Path path, Consumer<Map<String, Object>> writer) {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		writer.accept(yamlMap);
		writeSafeYamlMap(path, yamlMap);
	}
	
	public static void optPut(Map<String, Object> map, String name, String value) {
		if(value != null && !value.isEmpty()) {
			map.put(name, value);
		}
	}
	
	public static void optPut(Map<String, Object> map, String name, double value) {
		if(Double.isFinite(value)) {
			map.put(name, value);
		}
	}
	
	public static <T> void putArray(Map<String, Object> map, String name, Iterable<T> iterable, Function<T, Object> mapper) {
		Vec<Object> vec = new Vec<Object>();
		iterable.forEach(label -> vec.add(mapper.apply(label)));
		map.put(name, vec);
	}
	
	public static <T> Vec<T> getVec(YamlMap yamlMap, String name, Function<YamlMap, T> mapper) {
		List<YamlMap> ldList = yamlMap.getList(name).asMaps();
		return ldList.stream().map(mapper).collect(Vec.collector());
	}
	
	public static <T> Vec<T> optVec(YamlMap yamlMap, String name, Function<YamlMap, T> parser) {
		Vec<T> vec = new Vec<T>();
		List<YamlMap> data = yamlMap.optList(name).asMaps();		
		for(YamlMap v:data) {
			T element = parser.apply(v);
			if(element != null) {
				vec.add(element);
			}
		}		
		return vec;
	}
}
