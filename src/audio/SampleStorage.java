package audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.tinylog.Logger;

import audio.DeviceInventory.Entry;
import audio.SampleStorageConnector.TlSampleStorageConnector;
import util.Timer;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class SampleStorage {
	public static final boolean UNKNOWN_LOCATION_AS_DEVICE = true;
	
	private final Path root_path;
	private final Path root_data_path;

	private final Broker broker;

	public final TlSampleStorageConnector tlSampleStorageConnector;	
	public final DeviceInventory deviceInventory;

	public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public SampleStorage(Broker broker) {
		this.broker = broker;
		this.root_path = broker.config().audioConfig.root_path;
		this.root_data_path = broker.config().audioConfig.root_data_path;
		this.deviceInventory = new DeviceInventory(broker.config().audioConfig.device_inventory_file);
		try {
			Connection conn = DriverManager.getConnection("jdbc:h2:./sample_meta");
			tlSampleStorageConnector = new TlSampleStorageConnector(conn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		tlSampleStorageConnector.get().init(false);
		if(!broker.commandlineConfig.no_yaml_scan) {
			refresh(false);	
		}
	}

	public void refresh(boolean clear) {
		Timer.start("traverse");
		SampleStorageConnector sampleStorageConnector = tlSampleStorageConnector.get();
		try {
			if(clear) {
				sampleStorageConnector.init(true);
			}
			//sampleStorageConnector.initClearTraverseTable();

			AudioProjectConfig projectConfig = broker.config().audioConfig;
			int[] stats = new int[] {0, 0};
			traverse(projectConfig, projectConfig.root_path, stats);
			if(stats[0] > 0 || stats[1] > 0) {
				Logger.info(stats[0] + " rows inserted, " + stats[1] + " rows updated");
			}
			//sampleStorageConnector.deleteTraverseMissing();
		} catch (IOException e) {
			Logger.error(e);
		} finally {
			Logger.info(Timer.stop("traverse"));
		}
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
		} catch(NoSuchFileException e) {
			Logger.warn("missing  " + traversing_path);
		} catch(Exception e) {
			Logger.warn("error in " + traversing_path + "   " + e);
		}
	}

	private void refreshSampleEntry(AudioProjectConfig projectConfig, Path root, Path metaPath, int[] stats) {
		SampleStorageConnector sampleStorageConnector = tlSampleStorageConnector.get();
		Path meta_rel_path = projectConfig.root_path.relativize(metaPath);
		Path meta_rel_parent_path = meta_rel_path.getParent();
		String meta_filename = meta_rel_path.getFileName().toString();
		if(!meta_filename.endsWith(".yaml")) {
			throw new RuntimeException("wrong meta");
		}
		String data_filename = meta_filename.substring(0, meta_filename.length() - 5);
		int folderID = meta_rel_parent_path == null ? 0 : sampleStorageConnector.getFolderId(meta_rel_parent_path.toString());
		int sampleId = sampleStorageConnector.getSampleIdByPath(folderID, data_filename);
		if(!sampleStorageConnector.existSampleId(sampleId)) {			
			YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
			if(yamlMap.contains("AudioSens")) {
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
				if(location != null && location.isBlank()) {
					location = null;
				}
				if(location == null && UNKNOWN_LOCATION_AS_DEVICE && device_id != null && !device_id.isBlank()) {
					location = "(device) " + device_id;
				}				
				int locationId = location == null ? 0 : sampleStorageConnector.getLocationId(location);
				sampleStorageConnector.insertSample(sampleId, locationId, timestamp);
				Logger.info("insertSample " + sampleId + "   " + locationId + "  " + timestamp);
			}			
		}		
	}
}
