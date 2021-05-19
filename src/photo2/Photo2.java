package photo2;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	@Override
	public String toString() {
		return "Photo2 [id=" + id + ", metaPath=" + metaPath + ", imagePath=" + imagePath + ", location=" + location
				+ ", date=" + date + "]";
	}
}
