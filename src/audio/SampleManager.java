package audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;


import org.tinylog.Logger;

import audio.DeviceInventory.Entry;
import audio.SampleManagerConnector.SQL;
import audio.SampleManagerConnector.SampleRowConsumer;
import audio.SampleManagerConnector.TlSampleManagerConnector;
import util.Timer;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class SampleManager {
	

	private final Path root_path;
	private final Path root_data_path;

	private final Broker broker;

	private final Connection conn;
	public final TlSampleManagerConnector tlSampleManagerConnector;	
	public final DeviceInventory deviceInventory;
	
	public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public SampleManager(Broker broker) {
		this.broker = broker;
		this.root_path = broker.config().audioConfig.root_path;
		this.root_data_path = broker.config().audioConfig.root_data_path;
		this.deviceInventory = new DeviceInventory(broker.config().audioConfig.device_inventory_file);
		try {
			conn = DriverManager.getConnection("jdbc:h2:./sample_cache");
			tlSampleManagerConnector = new TlSampleManagerConnector(conn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		tlSampleManagerConnector.get().init(false);
		rescan(false);	
	}

	public static String metaRelPathToID(String project, String meta_rel_path) {
		String id = meta_rel_path.replaceAll("/", "__");
		id = id.replaceAll("\\\\", "__");
		id = id.replaceAll(".yaml", "");
		//id = project + "__" + id;
		return id;
	}

	private void traverse(AudioProjectConfig projectConfig, Path traversing_path, int[] stats) throws IOException {
		Logger.info("traverse " + traversing_path);
		try {
			for(Path sub_path:Files.newDirectoryStream(traversing_path)) {
				if(sub_path.toFile().isDirectory()) {
					traverse(projectConfig, sub_path, stats);
				} else if(sub_path.toFile().isFile()) {
					try {
						if(sub_path.getFileName().toString().endsWith(".yaml")) {
							refreshSampleEntry(projectConfig, traversing_path, sub_path, stats);
						}
					} catch(Exception e) {
						Logger.warn(e);
						e.printStackTrace();
					}
				} else {
					Logger.warn("unknown entity: " + sub_path);
				}
			}
		} catch(Exception e) {
			Logger.warn("error in " + traversing_path + "   " + e);
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
				//Logger.info("update sample " + id);
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
						Logger.info("update sample " + id);
						sqlconnector.update(id, projectConfig.project, meta_rel_path, sample_rel_path, location, timestamp, last_modified, locked, device_id);
						if(stats != null) {
							stats[1]++;
						}
					} else {							
						sqlconnector.insert(id, projectConfig.project, meta_rel_path, sample_rel_path, location, timestamp, last_modified, locked, device_id);
						if(stats != null) {
							stats[0]++;
						}
					}
				} else {
					Logger.warn("no valid AudioSens yaml  " + metaPath);
					sqlconnector.deleteSample(id);
				}
			}	
		} else {
			Logger.info("remove from DB " + metaPath);				
			sqlconnector.deleteSample(id);			
		}
	}

	public void rescan(boolean rereadAll) {
		Timer.start("traverse");
		try {
			if(rereadAll) {
				tlSampleManagerConnector.get().init(true);
			}
			tlSampleManagerConnector.get().initClearTraverseTable();

			AudioProjectConfig projectConfig = broker.config().audioConfig;
			int[] stats = new int[] {0, 0};
			traverse(projectConfig, projectConfig.root_path, stats);
			if(stats[0] > 0 || stats[1] > 0) {
				Logger.info(stats[0] + " rows inserted, " + stats[1] + " rows updated");
			}
			tlSampleManagerConnector.get().deleteTraverseMissing();
		} catch (IOException e) {
			Logger.error(e);
		} finally {
			Logger.info(Timer.stop("traverse"));
		}
	}
	
	abstract class AbstractSampleConverter implements SampleRowConsumer {
		@Override
		public final void accept(String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp, long lastModified, boolean locked, String device) {
			Sample2 sample = new Sample2(id, project, root_path.resolve(meta_rel_path), root_data_path.resolve(sample_rel_path), location, timestamp, device);
			accept(sample);			
		}
		public abstract void accept(Sample2 sample);
	}
	
	class SampleConverter extends AbstractSampleConverter {
		private final Consumer<Sample2> consumer;
		
		SampleConverter(Consumer<Sample2> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void accept(Sample2 sample) {
			consumer.accept(sample);			
		}		
	}
	
	class SampleHolder extends AbstractSampleConverter {
		public Sample2 sample;

		@Override
		public void accept(Sample2 sample) {
			this.sample = sample;			
		}
	}

	public void forEach(Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEach(new SampleConverter(consumer));
	}
	
	public void forEachAtTimestamp(long timestamp, Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEachAtTimestamp(timestamp, new SampleConverter(consumer));
	}
	
	public void forEachAtTimerange(long start, long end, Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEachAtTimerange(start, end, new SampleConverter(consumer));
	}

	public void forEachPaged(Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachPaged(new SampleConverter(consumer), limit, offset);
	}
	
	public void forEachAtTimestampPaged(long timestamp, Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachAtTimestampPaged(timestamp, new SampleConverter(consumer), limit, offset);
	}
	
	public void forEachAtTimerangePaged(long start, long end, Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachAtTimerangePaged(start, end, new SampleConverter(consumer), limit, offset);
	}

	public void forEachAtLocation(String location, Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEachAtLocation(location, new SampleConverter(consumer));
	}
	
	public void forEachAtLocationAtTimestamp(String location, long timestamp, Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEachAtLocationAtTimestamp(location, timestamp, new SampleConverter(consumer));
	}
	
	public void forEachAtLocationAtTimerange(String location, long start, long end, Consumer<Sample2> consumer) {
		tlSampleManagerConnector.get().forEachAtLocationAtTimerange(location, start, end, new SampleConverter(consumer));
	}

	public void forEachPagedAtLocation(String location, Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachPagedAtLocation(location, new SampleConverter(consumer), limit, offset);
	}
	
	public void forEachPagedAtLocationAtTimestamp(String location, long timestamp, Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachPagedAtLocationAtTimestamp(location, timestamp, new SampleConverter(consumer), limit, offset);
	}
	
	public void forEachPagedAtLocationAtTimerange(String location, long start, long end, Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleManagerConnector.get().forEachPagedAtLocationAtTimerange(location, start, end, new SampleConverter(consumer), limit, offset);
	}
	
	private Sample2 convertRow(ResultSet res) {
		SampleHolder sampleHolder = new SampleHolder();
		SampleManagerConnector.consumeRow(res, sampleHolder);
		return sampleHolder.sample;
	}

	public Sample2 getById(String id) {
		try {
			PreparedStatement stmt = tlSampleManagerConnector.get().getStatement(SQL.QUERY_ID);
			stmt.setString(1, id);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				Sample2 sample = convertRow(res);
				return sample;
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
