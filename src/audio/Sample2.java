package audio;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.yaml.YamlList;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Sample2 {
	static final Logger log = LogManager.getLogger();

	public final String id;
	public final String project;
	public final Path metaPath;
	public final Path samplePath;
	public final String location;
	public final LocalDateTime date;

	public Sample2(String id, String project, Path metaPath, Path samplePath, String location, LocalDateTime date) {
		this.id = id;
		this.project = project;
		this.metaPath = metaPath;
		this.samplePath = samplePath;
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
		return "Sample2 [id=" + id + ", metaPath=" + metaPath + ", samplePath=" + samplePath + ", location=" + location
				+ ", date=" + date + "]";
	}
}
