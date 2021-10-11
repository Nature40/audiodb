package audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.DeviceInventory.Entry;
import audio.SampleManagerConnector.SQL;
import audio.SampleManagerConnector.TlSampleManagerConnector;
import util.Timer;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class SampleManager {
	static final Logger log = LogManager.getLogger();

	private final Path root_path;

	private final Broker broker;

	private final Connection conn;
	public final TlSampleManagerConnector tlSampleManagerConnector;	
	public final DeviceInventory deviceInventory;

	public SampleManager(Broker broker) {
		this.broker = broker;
		this.root_path = broker.config().audioConfig.root_path;
		this.deviceInventory = new DeviceInventory(broker.config().audioConfig.device_inventory_file);
		try {
			conn = DriverManager.getConnection("jdbc:h2:./sample_cache");
			tlSampleManagerConnector = new TlSampleManagerConnector(conn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		tlSampleManagerConnector.get().init();
		rescan(false);	
	}

	public static String metaRelPathToID(String project, String meta_rel_path) {
		String id = meta_rel_path.replaceAll("/", "__");
		id = id.replaceAll("\\\\", "__");
		id = id.replaceAll(".yaml", "");
		//id = project + "__" + id;
		return id;
	}

	private void traverse(AudioProjectConfig projectConfig, Path root, int[] stats) throws IOException {
		log.info("traverse " + root);
		try {
			for(Path path:Files.newDirectoryStream(root)) {
				if(path.toFile().isDirectory()) {
					traverse(projectConfig, path, stats);
				} else if(path.toFile().isFile()) {
					try {
						if(path.getFileName().toString().endsWith(".yaml")) {
							refreshSampleEntry(projectConfig, root, path, stats);
						}
					} catch(Exception e) {
						log.warn(e);
						e.printStackTrace();
					}
				} else {
					log.warn("unknown entity: " + path);
				}
			}
		} catch(Exception e) {
			log.warn("error in " + root + "   " + e);
		}
	}

	public boolean isUpToDate(String id, long last_modified) {
		try {
			SampleManagerConnector sqlconnector = tlSampleManagerConnector.get();
			PreparedStatement stmt = sqlconnector.getStatement(SQL.QUERY_IS_UP_TO_DATE);
			stmt.setString(1, id);
			stmt.setLong(2, last_modified);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				return res.getBoolean(1);
			} else {
				throw new RuntimeException("sql error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean existSample(String id) {
		return tlSampleManagerConnector.get().exist(id);
	}

	private void refreshSampleEntry(AudioProjectConfig projectConfig, Path root, Path metaPath, int[] stats) {
		SampleManagerConnector sqlconnector = tlSampleManagerConnector.get();
		String meta_rel_path = projectConfig.root_path.relativize(metaPath).toString();
		String id = metaRelPathToID(projectConfig.project, meta_rel_path);
		if(metaPath.toFile().exists()) {
			sqlconnector.insertTraverse(id);
			long last_modified = metaPath.toFile().lastModified();
			if(!this.isUpToDate(id, last_modified)) {
				//log.info("update sample " + id);
				YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
				if(yamlMap.contains("AudioSens") /*&& yamlMap.getString("AudioSens").equals("v1.0")*/) {
					String sample_file = yamlMap.getString("file");
					long timestamp = yamlMap.optLong("timestamp", 0);
					String location = null;
					String device_id = yamlMap.optString("device_id", null);
					if(device_id != null) {
						Entry entry = timestamp == 0 ? deviceInventory.getLastInfinite(device_id) : deviceInventory.getLast(device_id, timestamp);
						if(entry != null && entry.location != null && !entry.location.isBlank()) {
							location = entry.location;
						}
					}
					if(location == null) {
						location = yamlMap.optString("location", null);
					}					
					String sample_rel_path = projectConfig.root_path.relativize(root.resolve(sample_file)).toString(); 
					boolean locked = false; // TODO
					if(sqlconnector.exist(id)) {
						log.info("update sample " + id);
						sqlconnector.update(id, projectConfig.project, meta_rel_path, sample_rel_path, location, timestamp, last_modified, locked);
						if(stats != null) {
							stats[1]++;
						}
					} else {							
						sqlconnector.insert(id, projectConfig.project, meta_rel_path, sample_rel_path, location, timestamp, last_modified, locked);
						if(stats != null) {
							stats[0]++;
						}
					}
				} else {
					log.warn("no valid AudioSens yaml  " + metaPath);
					sqlconnector.deleteSample(id);
				}
			}	
		} else {
			log.info("remove from DB " + metaPath);				
			sqlconnector.deleteSample(id);			
		}
	}

	public void rescan(boolean rereadAll) {
		Timer.start("traverse");
		try {
			if(rereadAll) {
				tlSampleManagerConnector.get().initClear();
			}
			tlSampleManagerConnector.get().initClearTraverseTable();

			AudioProjectConfig projectConfig = broker.config().audioConfig;
			int[] stats = new int[] {0, 0};
			traverse(projectConfig, projectConfig.root_path, stats);
			if(stats[0] > 0 || stats[1] > 0) {
				log.info(stats[0] + " rows inserted, " + stats[1] + " rows updated");
			}
			tlSampleManagerConnector.get().deleteTraverseMissing();
		} catch (IOException e) {
			log.error(e);
		} finally {
			log.info(Timer.stop("traverse"));
		}
	}

	public void forEach(Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEach((String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp) -> {
			Sample2 sample = new Sample2(id, project, root_path.resolve(meta_rel_path), root_path.resolve(sample_rel_path), location, timestamp);
			consumer.accept(sample);
		});
	}

	public void forEachPaged(Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachPaged((String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp) -> {
			Sample2 sample = new Sample2(id, project, root_path.resolve(meta_rel_path), root_path.resolve(sample_rel_path), location, timestamp);
			consumer.accept(sample);
		}, limit, offset);
	}

	public void forEachAtLocation(String location, Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEachAtLocation(location, (String id, String project, String meta_rel_path, String sample_rel_path, String locationR, long timestamp) -> {
			Sample2 sample = new Sample2(id, project, root_path.resolve(meta_rel_path), root_path.resolve(sample_rel_path), locationR, timestamp);
			consumer.accept(sample);
		});
	}

	public void forEachPagedAtLocation(String location, Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachPagedAtLocation(location, (String id, String project, String meta_rel_path, String sample_rel_path, String locationR, long timestamp) -> {
			Sample2 sample = new Sample2(id, project, root_path.resolve(meta_rel_path), root_path.resolve(sample_rel_path), locationR, timestamp);
			consumer.accept(sample);
		}, limit, offset);
	}

	public Sample2 getById(String id) {
		try {
			PreparedStatement stmt = tlSampleManagerConnector.get().getStatement(SQL.QUERY_ID);
			stmt.setString(1, id);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				String qId = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String location = res.getString(5);
				long timestamp = res.getLong(6);
				Sample2 sample = new Sample2(qId, project, root_path.resolve(meta_rel_path), root_path.resolve(sample_rel_path), location, timestamp);
				return sample;
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
