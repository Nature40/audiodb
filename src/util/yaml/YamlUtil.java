package util.yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;


import org.tinylog.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import util.collections.ReadonlyList;
import util.collections.vec.SyncVec;
import util.collections.vec.Vec;

public class YamlUtil {

	private static class CleanResolver extends Resolver {
		@Override
		protected void addImplicitResolvers() {
			addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
			addImplicitResolver(Tag.INT, INT, "-+0123456789");
			addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
			addImplicitResolver(Tag.MERGE, MERGE, "<");
			addImplicitResolver(Tag.NULL, NULL, "~nN\0");
			addImplicitResolver(Tag.NULL, EMPTY, null);
			//addImplicitResolver(Tag.TIMESTAMP, TIMESTAMP, "0123456789"); // do not parse timestamps			
		}
	}

	public static YamlMap readYamlMap(Path path) {
		try(InputStream in = new FileInputStream(path.toFile())) {			
			Yaml yaml = new Yaml(new Constructor(), new Representer(), new DumperOptions(), new LoaderOptions(), new CleanResolver());
			YamlMap yamlMap = YamlMap.ofObject(yaml.load(in));
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

	public static void optPut(Map<String, Object> map, String name, String value, String def) {
		if(value != def) {
			map.put(name, value);
		}
	}

	public static void optPut(Map<String, Object> map, String name, long value, long def) {
		if(value != def) {
			map.put(name, value);
		}
	}

	public static void put(Map<String, Object> map, String name, long value) {
		map.put(name, value);
	}

	public static void put(Map<String, Object> map, String name, String value) {
		map.put(name, value);
	}

	public static void optPut(Map<String, Object> map, String name, double value) {
		if(Double.isFinite(value)) {
			map.put(name, value);
		}
	}

	public static <T> void putList(Map<String, Object> map, String name, Iterable<T> iterable, Function<T, Object> mapper) {
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

	public static <T> SyncVec<T> optSyncVec(YamlMap yamlMap, String name, Function<YamlMap, T> parser) {
		SyncVec<T> vec = new SyncVec<T>();
		List<YamlMap> data = yamlMap.optList(name).asMaps();		
		for(YamlMap v:data) {
			T element = parser.apply(v);
			if(element != null) {
				vec.addUnsync(element);
			}
		}		
		return vec;
	}

	public static <T> void optListConsumer(YamlMap yamlMap, String name, Function<YamlMap, T> parser, Consumer<T> action) {
		List<YamlMap> data = yamlMap.optList(name).asMaps();		
		for(YamlMap v:data) {
			T element = parser.apply(v);
			if(element != null) {
				action.accept(element);
			}
		}		
	}

	public static <E, T> void optPut(LinkedHashMap<String, Object> yamlMap, String name, ReadonlyList<E> list, Function<E, T> mapper) {
		if(list.isEmpty()) {
			return;
		}
		T[] a = list.mapArray(mapper);
		yamlMap.put(name, a);
	}

	public static <E, T> void optPut(LinkedHashMap<String, Object> yamlMap, String name, ReadonlyList<E> list, IntFunction<T[]> generator, Function<E, T> mapper) {
		if(list.isEmpty()) {
			return;
		}
		T[] a = list.mapArray(generator, mapper);
		yamlMap.put(name, a);
	}
}
