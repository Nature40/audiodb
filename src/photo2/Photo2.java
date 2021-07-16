package photo2;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.resolver.Resolver;

import audio.Label;
import photo2.api.PhotoMeta;
import util.collections.vec.Vec;
import util.yaml.YamlList;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Photo2 {
	static final Logger log = LogManager.getLogger();

	public final String id;
	public final Path metaPath;
	public final Path imagePath;
	public final String location;
	public final LocalDateTime date;

	public Photo2(String id, Path metaPath, Path imagePath, String location, LocalDateTime date) {
		this.id = id;
		this.metaPath = metaPath;
		this.imagePath = imagePath;
		this.location = location;
		this.date = date;
		//log.info(this);
	}

	public void foreachClassification(Consumer<YamlMap> consumer) {
		YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
		YamlList list = yamlMap.optList("classifications");
		list.asMaps().forEach(consumer);
	}

	public void foreachDetection(Consumer<YamlMap> consumer) {
		YamlMap yamlMap = getMeta();
		YamlList list = yamlMap.optList("detections");
		list.asMaps().forEach(consumer);
	}

	public YamlMap getMeta() {
		YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
		return yamlMap;
	}

	@Override
	public String toString() {
		return "Photo2 [id=" + id + ", metaPath=" + metaPath + ", imagePath=" + imagePath + ", location=" + location
				+ ", date=" + date + "]";
	}

	private void writeMeta(Map<String, Object> map) {
		YamlUtil.writeSafeYamlMap(metaPath, map);
	}

	public void setClassification(float[] bbox, String classification, String classificator, String identity, String date) {		
		PhotoMeta photoMeta = new PhotoMeta(getMeta());
		photoMeta.setClassification(bbox, classification, classificator, identity, date);
		writeMeta(photoMeta.metaMap.getInternalMap());
	}
}
