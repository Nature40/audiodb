package audio.task;

import java.io.IOException;
import java.nio.file.Path;

import audio.AudioProjectConfig;
import audio.SampleManager.TraverseYamlConsumer;
import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Scan for yaml files with missing data files.")
@Role("admin")
public class Task_audio_scan_yaml_orphans extends Task {
	
	class TraverseExist implements TraverseYamlConsumer {
		
		long counterMissing = 0;

		@Override
		public void apply(AudioProjectConfig projectConfig, Path traversing_path, Path sub_path) {
			//Logger.info(sub_path);
			Path relPath = projectConfig.root_path.relativize(sub_path.getParent());
			//Logger.info(relPath);
			String yamlName = sub_path.getFileName().toString();
			if(!yamlName.endsWith(".yaml")) {
				throw new RuntimeException("error");
			}
			String dataName = yamlName.substring(0, yamlName.length() - 5);
			Path relDataPath = relPath.resolve(dataName);
			Path dataPath = projectConfig.root_data_path.resolve(relDataPath);
			//Logger.info(dataPath);
			if(!dataPath.toFile().exists()) {
				counterMissing++;
				setMessage(dataPath.toString());
			}
		}
		
	}

	@Override
	public void run() throws IOException {
		throw new RuntimeException("not implemented");
		/*TraverseExist traverseExist = new TraverseExist();
		ctx.broker.sampleManager().traverseYamlFiles(traverseExist);
		setMessage(traverseExist.counterMissing + " orphans");*/
	}
}
