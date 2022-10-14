package audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;


import org.tinylog.Logger;

import audio.server.Webserver;

@Deprecated
public class Samples {

	private final Path root_path;
	private final Path root_data_path;	
	
	public Map<String, Sample> sampleMap;

	public Samples(Broker broker) {
		this.root_path = broker.config().audioConfig.root_path;
		this.root_data_path = broker.config().audioConfig.root_data_path;
		sampleMap = new ConcurrentSkipListMap<String, Sample>();
		rescan();
	}

	public void rescan() {
		try {
			sampleMap.clear();
			ArrayList<Path> paths = Webserver.getAudioPaths(root_path, null);
			for(Path metaPath:paths) {
				try {
					String id = root_path.relativize(metaPath).toString();
					Logger.info("read " + id);
					id = id.replaceAll("/", "__");
					id = id.replaceAll("\\\\", "__");
					id = id.replaceAll(".yaml", "");
					Path metaDirectoryPath = metaPath.getParent();
					Path metaRelDirectoryPath = root_path.relativize(metaDirectoryPath);
					Path dataDirectoryPath = root_data_path.resolve(metaRelDirectoryPath);
					Sample sample = new Sample(id, metaPath, dataDirectoryPath);
					sample.readFromFile();
					sample.checkAndCorrectLabelDublicates();
					sampleMap.put(id, sample);
				} catch (Exception e) {
					//e.printStackTrace();
					Logger.warn("error in " + metaPath + "   " + e);
				}
			}
		} catch (IOException e) {
			Logger.warn(e);
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
