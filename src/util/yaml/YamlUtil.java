package util.yaml;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
		try(InputStream inputStream = new FileInputStream(path.toFile())) {	
			try(BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
				LoaderOptions loaderOptions = new LoaderOptions();
				DumperOptions dumperOptions = new DumperOptions();
				Yaml yaml = new Yaml(
						new Constructor(loaderOptions), 
						new Representer(dumperOptions), 
						dumperOptions, 
						loaderOptions, 
						new CleanResolver()
					);
				YamlMap yamlMap = YamlMap.ofObject(yaml.load(bufferedInputStream));
				return yamlMap;
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeSafeYamlMap(Path path, Map<String, Object> yamlMap) {		
		Path writepath = Paths.get(path.toString()+"_temp_" + Math.abs(ThreadLocalRandom.current().nextLong()));
		File writeFile = writepath.toFile();		
		try(FileWriter fileWriter = new FileWriter(writeFile, StandardCharsets.UTF_8)){
			try(BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)){
				new Yaml().dump(yamlMap, bufferedWriter);
				bufferedWriter.close();
				fileWriter.close();
				for(int count = 0;;) {
					try {
						Files.move(writepath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
						if(count > 0) {
							Logger.warn((count + 1) + " tries on successful move " + writepath);
						}
						return;
					} catch (Exception e) {
						count++;
						if(count >= 100) {
							throw e;
						}
					}
					Thread.sleep(50);
				}
			}				
		} catch (RuntimeException e) {
			throw e;
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

	public static String readFileToString(Path path) throws IOException {
		return Files.readString(path, StandardCharsets.UTF_8);
	}
}
