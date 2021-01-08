package audio;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.server.Webserver;

public class Samples {
	static final Logger log = LogManager.getLogger();

	public Map<String, Sample> sampleMap;

	public Samples() {
		sampleMap = new ConcurrentSkipListMap<String, Sample>();
		rescan();
	}

	public void rescan() {
		try {
			Path root = Paths.get("data");
			sampleMap.clear();
			ArrayList<Path> paths = Webserver.getAudioPaths(root, null);
			for(Path path:paths) {
				try {
					String id = root.relativize(path).toString();
					id = id.replaceAll("/", "__");
					id = id.replaceAll("\\\\", "__");
					id = id.replaceAll(".yaml", "");
					Sample sample = new Sample(id, path);
					sample.readFromFile();
					sampleMap.put(id, sample);
				} catch (Exception e) {
					log.warn("error in " + path + "   " + e);
				}
			}
		} catch (IOException e) {
			log.warn(e);
		}

	}

	public Sample getThrow(String id) {
		Sample sample = sampleMap.get(id);
		if(sample == null) {
			throw new RuntimeException("sample not found: " + id);
		}
		return sample;
	}

}
