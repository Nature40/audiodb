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
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

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

}
