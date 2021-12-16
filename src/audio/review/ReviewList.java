package audio.review;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.function.Consumer;


import org.tinylog.Logger;

import util.collections.vec.SyncVec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class ReviewList {
	
	
	private final Path path;
	private final SyncVec<ReviewListEntry> entries = new SyncVec<ReviewListEntry>();

	private ReviewList(Path path) {
		this.path = path;
	}

	public void mutate(Consumer<SyncVec<ReviewListEntry>> action) {
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

	public void forEach(Consumer<ReviewListEntry> consumer) {
		entries.forEach(consumer);
	}

	public static ReviewList ofFile(Path path) {
		ReviewList reviewList = new ReviewList(path);
		reviewList.readFromFile();
		return reviewList;
	}

	private void readFromFile() {
		entries.lockWrite();
		try {
			entries.clearUnsync();
			YamlMap yamlMap = YamlUtil.readYamlMap(path);			
			YamlUtil.optListConsumer(yamlMap, "entries", ReviewListEntry::ofYAML, entries::addUnsync);
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
			YamlUtil.putList(map, "entries", entries, ReviewListEntry::toMap);		
			YamlUtil.writeSafeYamlMap(path, map);	
		} finally {
			entries.unlockRead();
		}
	}

	/*public synchronized LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();		
		YamlUtil.putArray(map, "entries", entries, ReviewListEntry::toMap);
		return map;
	}

	public static ReviewList ofYAML(YamlMap yamlMap) {		
		ReviewList reviewList = new ReviewList();		
		reviewList.entries = YamlUtil.optSyncVec(yamlMap, "entries", ReviewListEntry::ofYAML);
		return reviewList;
	}

	public void forEach(Consumer<ReviewListEntry> consumer) {
		entries.forEach(consumer);
	}

	public int findIndexOf(String sample_id, String label_name, double label_start, double label_end) {
		return entries.findIndexOf(e -> e.sample_id.equals(sample_id) && e.label_name.equals(label_name) && e.isInterval(label_start, label_end));		
	}

	public ReviewListEntry getEntry(int label_index) {
		return entries.get(label_index);		
	}

	public Vec<ReviewListEntry> copyEntries() {
		return entries.copy();
	}

	public synchronized void set(Vec<ReviewListEntry> entries) {
		this.entries = entries.copy();		
	}*/
}
