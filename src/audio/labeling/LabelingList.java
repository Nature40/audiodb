package audio.labeling;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.function.Consumer;


import org.tinylog.Logger;

import util.collections.vec.SyncVec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class LabelingList {
	
	
	private final Path path;
	private final SyncVec<LabelingListEntry> entries = new SyncVec<LabelingListEntry>();

	private LabelingList(Path path) {
		this.path = path;
	}

	public void mutate(Consumer<SyncVec<LabelingListEntry>> action) {
		entries.lockWrite();
		try {
			action.accept(entries);
			writeToFile();
		} catch(Exception e) {
			readFromFile();
		} finally {
			entries.unlockWrite();
		}
	}

	public void forEach(Consumer<LabelingListEntry> consumer) {
		entries.forEach(consumer);
	}

	public static LabelingList ofFile(Path path) {
		LabelingList reviewList = new LabelingList(path);
		reviewList.readFromFile();
		return reviewList;
	}

	private void readFromFile() {
		entries.lockWrite();
		try {
			entries.clearUnsync();
			YamlMap yamlMap = YamlUtil.readYamlMap(path);			
			YamlUtil.optListConsumer(yamlMap, "entries", LabelingListEntry::ofYAML, entries::addUnsync);
		} catch(RuntimeException e) {
			entries.clearUnsync();
			Logger.warn(e);
			throw e;
		} finally {
			entries.unlockWrite();
		}
	}

	private void writeToFile() {		
		entries.lockRead();
		try {
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			YamlUtil.putList(map, "entries", entries, LabelingListEntry::toMap);		
			YamlUtil.writeSafeYamlMap(path, map);	
		} finally {
			entries.unlockRead();
		}
	}	
}
