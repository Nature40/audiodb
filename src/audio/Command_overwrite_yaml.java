package audio;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Command_overwrite_yaml extends Command_create_yaml {
	static final Logger log = LogManager.getLogger();

	@Override
	protected boolean traverseFile(File file) {
		String path = file.getPath();
		if(path.endsWith(".wav") || path.endsWith(".WAV")) {
			String yamlPath = path + ".yaml";
			File yamlFile = new File(yamlPath);
			return createYaml(file, yamlFile.toPath());
		}
		return false;		
	}
}
