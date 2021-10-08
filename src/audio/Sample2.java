package audio;

import java.io.File;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Sample2 implements GeneralSample {
	static final Logger log = LogManager.getLogger();

	public final String id;
	public final String project;
	public final Path metaPath;
	public final Path samplePath;
	public final String location;
	public final long timestamp;

	private YamlMap yamlMap = null;
	private long samples = -2;

	public Sample2(String id, String project, Path metaPath, Path samplePath, String location, long timestamp) {
		this.id = id;
		this.project = project;
		this.metaPath = metaPath;
		this.samplePath = samplePath;
		this.location = location;
		this.timestamp = timestamp;
	}

	public Sample getSample() {
		return new Sample(id, metaPath);
	}

	public boolean hasLocation() {
		return location != null;
	}

	public boolean hasTimestamp() {
		return timestamp > 0;
	}

	@Override
	public File getAudioFile() {
		File audioFile = samplePath.toFile();
		return audioFile;
	}

	public YamlMap meta() {
		if(this.yamlMap == null) {
			this.yamlMap = YamlUtil.readYamlMap(metaPath);
		}
		return this.yamlMap;
	}
	
	public long samples() {
		if(samples == -2) {
			samples = meta().optLong("Samples", -1);
		}
		return samples;
	}
	
	public boolean hasSamples() {
		return samples() >= 0;
	}
}
