package audio;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class ReviewList {	
	private Vec<ReviewListEntry> entries;

	public synchronized LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();		
		YamlUtil.putArray(map, "entries", entries, ReviewListEntry::toMap);
		return map;
	}

	public static ReviewList ofYAML(YamlMap yamlMap) {		
		ReviewList reviewList = new ReviewList();		
		reviewList.entries = YamlUtil.optVec(yamlMap, "entries", ReviewListEntry::ofYAML);
		return reviewList;
	}
	
	public void forEach(Consumer<ReviewListEntry> consumer) {
		entries.forEach(consumer);
	}
}
