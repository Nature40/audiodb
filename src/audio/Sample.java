package audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import util.yaml.YamlMap;

public class Sample {
	static final Logger log = LogManager.getLogger();

	public final String id;
	public final Path metaPath;
	public final Path directoryPath;
	
	private YamlMap yamlMap;
	private String audioFilename;
	private Path audioPath;
	private File audioFile;

	public Sample(String id, Path metaPath) {
		this.id = id;		
		this.metaPath = metaPath;
		this.directoryPath = metaPath.getParent();
	}
	
	
	public void loadMeta() {
		Object inObject;
		try(InputStream in = new FileInputStream(metaPath.toFile())) {
			inObject = new Yaml().load(in);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if(inObject instanceof String) {
			inObject = new Yaml().load((String) inObject);
		}		
		yamlMap = YamlMap.ofObject(inObject);
		if(!yamlMap.contains("AudioSens")) {
			throw new RuntimeException("no AudioSens");
		}
		audioFilename = yamlMap.getString("file");
		audioPath = directoryPath.resolve(audioFilename);
		audioFile = audioPath.toFile();
	}
	
	public File getAudioFile() {
		return audioFile;
	}

	public String getAudioFileName() {
		return audioFilename;
	}


	public YamlMap getMetaMap() {
		return yamlMap;
	}
}
