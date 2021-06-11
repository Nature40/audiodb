package audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.SampleManagerConnector.TlSampleManagerConnector;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class SampleManager {
	static final Logger log = LogManager.getLogger();

	private final Path root_path = Paths.get("data");

	private final Broker broker;

	private final Connection conn;
	private final TlSampleManagerConnector tlSampleManagerConnector;	

	public SampleManager(Broker broker) {
		this.broker = broker;		
		try {
			conn = DriverManager.getConnection("jdbc:h2:./sample_cache");
			tlSampleManagerConnector = new TlSampleManagerConnector(conn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		rescan();
	}

	private void clear() {
		tlSampleManagerConnector.get().initClear();
	}
	
	private void insertSample(SampleManager projectConfig, Path path, Path root) {
		YamlMap yamlMap = YamlUtil.readYamlMap(path);
		if(yamlMap.contains("AudioSens") && yamlMap.getString("AudioSens").equals("v1.0")) {
			String image_file = yamlMap.getString("file");
			String location = yamlMap.getString("location");
			LocalDateTime date = yamlMap.optLocalDateTime("date"); // nullable
			String project = yamlMap.getString("project");
			//log.info(path);
			try {
				String meta_rel_path = projectConfig.root_path.relativize(path).toString();
				String sample_rel_path = projectConfig.root_path.relativize(root.resolve(image_file)).toString(); 
				log.info("read " + meta_rel_path);							
				String id = meta_rel_path.replaceAll("/", "__");
				id = id.replaceAll("\\\\", "__");
				id = id.replaceAll(".yaml", "");
				tlSampleManagerConnector.get().insert(id, project, meta_rel_path, sample_rel_path, location, date);							
			} catch (Exception e) {
				log.warn(e);
			}

		} else {
			log.warn("no valid AudioSens yaml  " + path);
		}
	}

	private void traverse(SampleManager projectConfig, Path root) throws IOException {
		for(Path path:Files.newDirectoryStream(root)) {
			if(path.toFile().isDirectory()) {
				traverse(projectConfig, path);
			} else if(path.toFile().isFile()) {
				if(path.getFileName().toString().endsWith(".yaml")) {
					insertSample(projectConfig, path, root);
				}
			} else {
				log.warn("unknown entity: " + path);
			}
		}
	}

	public void rescan() {
		try {
			clear();
			traverse(this, root_path);
		} catch (IOException e) {
			log.warn(e);
		}
	}

	public void forEach(Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEach((String id, String project, String meta_rel_path, String sample_rel_path, String location, LocalDateTime date) -> {
			Sample2 sample = new Sample2(id, project, root_path.resolve(meta_rel_path), root_path.resolve(sample_rel_path), location, date);
			consumer.accept(sample);
		});
	}
}
