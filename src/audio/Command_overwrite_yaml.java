package audio;

import java.io.File;

@Deprecated
public class Command_overwrite_yaml extends Command_create_yaml {

	@Override
	protected boolean traverseFile(File file) {
		String path = file.getPath();
		if(path.endsWith(".wav") || path.endsWith(".WAV")) {
			String yamlPath = path + ".yaml";
			File yamlFile = new File(yamlPath);
			return MetaCreator.createYaml(file, yamlFile.toPath());
		}
		return false;		
	}
}
