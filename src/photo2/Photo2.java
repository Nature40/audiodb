package photo2;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Consumer;


import org.tinylog.Logger;

import photo2.api.PhotoMeta;
import util.yaml.YamlList;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Photo2 {
	

	public final String id;
	public final PhotoProjectConfig projectConfig;
	public final Path metaPath;
	public final Path imagePath;
	public final String location;
	public final LocalDateTime date;
	public final long last_modified;
	public final boolean locked;

	public Photo2(String id, PhotoProjectConfig projectConfig, Path metaPath, Path imagePath, String location, LocalDateTime date, long last_modified, boolean locked) {
		this.id = id;
		this.projectConfig = projectConfig;
		this.metaPath = metaPath;
		this.imagePath = imagePath;
		this.location = location;
		this.date = date;
		this.last_modified = last_modified;
		this.locked = locked;
		//Logger.info(this);
	}

	/*public void foreachClassification(Consumer<YamlMap> consumer) {
		YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
		YamlList list = yamlMap.optList("classifications");
		list.asMaps().forEach(consumer);
	}*/

	public void foreachDetection(Consumer<YamlMap> consumer) {
		YamlMap yamlMap = getMeta();
		YamlList list = yamlMap.optList("detections");
		list.asMaps().forEach(consumer);
	}

	public YamlMap getMeta() {
		YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
		return yamlMap;
	}

	private void writeMeta(Map<String, Object> map) {
		YamlUtil.writeSafeYamlMap(metaPath, map);
	}

	public void setClassification(float[] bbox, String classification, String classificator, String identity, String date) {		
		PhotoMeta photoMeta = new PhotoMeta(getMeta());
		photoMeta.setClassification(bbox, classification, classificator, identity, date);
		writeMeta(photoMeta.metaMap.getInternalMap());
	}

	@Override
	public String toString() {
		return "Photo2 [id=" + id + ", metaPath=" + metaPath + ", imagePath=" + imagePath + ", location=" + location
				+ ", date=" + date + ", last_modified=" + last_modified + ", locked=" + locked + "]";
	}
}
