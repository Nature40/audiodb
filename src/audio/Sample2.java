package audio;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Sample2 {
	static final Logger log = LogManager.getLogger();

	public final String id;
	public final String project;
	public final Path metaPath;
	public final Path samplePath;
	public final String location;
	public final long timestamp;

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
}
