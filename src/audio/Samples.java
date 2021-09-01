package audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.server.Webserver;

public class Samples {
	static final Logger log = LogManager.getLogger();

	private final Path root_path;
	
	public Map<String, Sample> sampleMap;	

	public Samples(Broker broker) {
		this.root_path = broker.config().audioConfig.root_path;
		sampleMap = new ConcurrentSkipListMap<String, Sample>();
		rescan();
	}

	public void rescan() {
		try {
			sampleMap.clear();
			ArrayList<Path> paths = Webserver.getAudioPaths(root_path, null);
			for(Path path:paths) {
				try {
					String id = root_path.relativize(path).toString();
					log.info("read " + id);
					id = id.replaceAll("/", "__");
					id = id.replaceAll("\\\\", "__");
					id = id.replaceAll(".yaml", "");
					Sample sample = new Sample(id, path);
					sample.readFromFile();
					sample.checkAndCorrectLabelDublicates();
					sampleMap.put(id, sample);
				} catch (Exception e) {
					//e.printStackTrace();
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

	public Sample getSample(String sample_id) {
		return sampleMap.get(sample_id);
	}
	
	public void forEach(Consumer<Sample> action) {
		sampleMap.values().forEach(action);
	}
}
