package audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.apache.commons.lang3.tuple.Pair;
import org.tinylog.Logger;

import audio.DeviceInventory.Entry;
import audio.SampleStorageConnector.StorageSample;
import audio.SampleStorageConnector.TlSampleStorageConnector;
import util.Timer;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class SampleStorage {
	public static final boolean UNKNOWN_LOCATION_AS_DEVICE = true;

	private final Broker broker;

	public final TlSampleStorageConnector tlSampleStorageConnector;	
	public final DeviceInventory deviceInventory;

	public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final AudioProjectConfig projectConfig;

	public SampleStorage(Broker broker) {
		this.broker = broker;
		projectConfig = broker.config().audioConfig;
		this.deviceInventory = new DeviceInventory(projectConfig.device_inventory_file);
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

			int[] stats = new int[] {0, 0};
			traverse(projectConfig, projectConfig.root_path, stats);
			if(stats[0] > 0 || stats[1] > 0) {
				sampleStorageConnector.refreshOrderedSample();
				Logger.info(stats[0] + " samples inserted, " + stats[1] + " samples updated");
			}
			//sampleStorageConnector.deleteTraverseMissing();
		} catch (IOException e) {
			Logger.error(e);
		} finally {
			Logger.info(Timer.stop("traverse"));
		}
	}
	
	public void  recreateLocationsFromInventory() {		
		SampleStorageConnector sampleStorageConnector = tlSampleStorageConnector.get();
		int[] cnt = new int[] {0};
		sampleStorageConnector.clearLocation();
		Logger.info("start recreateLocationsFromInventory forEachSampleTimeDevice");
		sampleStorageConnector.forEachSampleTimeDevice((int sampleId, long timestamp, int deviceId, String deviceName) -> {
			if(cnt[0] % 1_000_000 == 0) {
				Logger.info(cnt[0] + "  forEachSampleTimeDevice");
			}
			String locationName = null;			
			Entry entry = timestamp == 0 ? deviceInventory.getLastInfinite(deviceName) : deviceInventory.getLast(deviceName, timestamp);
			if(entry != null && entry.location != null && !entry.location.isBlank()) {
				locationName = entry.location;
			}			
			int locationId = sampleStorageConnector.getOrInsertLocationId(locationName == null ? MISSING_LOCATION : locationName);
			sampleStorageConnector.setSample(sampleId, timestamp, deviceId, locationId);
			cnt[0]++;			
		});
		Logger.info("end recreateLocationsFromInventory forEachSampleTimeDevice");
		sampleStorageConnector.refreshOrderedSample();
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

	public static String ROOT_FOLDER_MARKER = "ROOT_FOLDER_MARKER";
	public static String MISSING_DEVICE = "MISSING_DEVICE";
	public static String MISSING_LOCATION = "MISSING_LOCATION";

	private void refreshSampleEntry(AudioProjectConfig projectConfig, Path root, Path metaPath, int[] stats) {
		SampleStorageConnector sampleStorageConnector = tlSampleStorageConnector.get();
		Path meta_rel_path = projectConfig.root_path.relativize(metaPath);
		Path meta_rel_parent_path = meta_rel_path.getParent();
		String meta_filename = meta_rel_path.getFileName().toString();
		if(!meta_filename.endsWith(".yaml")) {
			throw new RuntimeException("wrong meta");
		}
		String data_filename = meta_filename.substring(0, meta_filename.length() - 5);
		int folderID = sampleStorageConnector.getOrInsertFolderId(meta_rel_parent_path == null ? ROOT_FOLDER_MARKER : meta_rel_parent_path.toString());
		long fileLastModified = metaPath.toFile().lastModified();
		Pair<Integer, Long> pair = sampleStorageConnector.getOrInsertSampleIdByFile(folderID, data_filename, fileLastModified);
		int sampleId = pair.getLeft();
		long dbLastModified = pair.getRight();
		boolean existSample = sampleStorageConnector.existSample(sampleId);
		if((!existSample) || dbLastModified < fileLastModified) {			
			YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
			if(yamlMap.contains("AudioSens")) {
				long timestamp = yamlMap.optLong("timestamp", 0);				
				String locationName = null;
				String deviceName = yamlMap.optString("device_id", null);
				if(deviceName != null) {
					Entry entry = timestamp == 0 ? deviceInventory.getLastInfinite(deviceName) : deviceInventory.getLast(deviceName, timestamp);
					if(entry != null && entry.location != null && !entry.location.isBlank()) {
						locationName = entry.location;
					}
				}
				if(locationName == null) {
					locationName = yamlMap.optString("location", null);
				}
				if(locationName != null && locationName.isBlank()) {
					locationName = null;
				}
				if(locationName == null && UNKNOWN_LOCATION_AS_DEVICE && deviceName != null && !deviceName.isBlank()) {
					locationName = "(device) " + deviceName;
				}				
				int deviceId = sampleStorageConnector.getOrInsertDeviceId(deviceName == null ? MISSING_DEVICE : deviceName);
				int locationId = sampleStorageConnector.getOrInsertLocationId(locationName == null ? MISSING_LOCATION : locationName);
				if(existSample) {
					sampleStorageConnector.setSample(sampleId, timestamp, deviceId, locationId);
				} else {
					sampleStorageConnector.insertSample(sampleId, timestamp, deviceId, locationId);
				}
				if(dbLastModified != fileLastModified) {
					sampleStorageConnector.updateFile(sampleId, fileLastModified);
				}
				if(stats != null) {
					if(existSample) {
						stats[1]++;
					} else {
						stats[0]++;
					}
				}
				//Logger.info("insertSample " + sampleId + "  " + timestamp + "  " + deviceId + "  " + locationId);
			}			
		}		
	}
	
	public StorageSample getStorageSample(int sampleId) {
		StorageSample storageSample = tlSampleStorageConnector.get().getSample(sampleId);
		return storageSample;
	}

	public Sample2 getSample(int sampleId) {
		StorageSample storageSample = tlSampleStorageConnector.get().getSample(sampleId);
		Path metaPath = storageSample.folderName.equals(ROOT_FOLDER_MARKER) ? projectConfig.root_path.resolve(storageSample.fileName + ".yaml") : projectConfig.root_path.resolve(storageSample.folderName).resolve(storageSample.fileName + ".yaml");
		Path samplePath = storageSample.folderName.equals(ROOT_FOLDER_MARKER) ? projectConfig.root_data_path.resolve(storageSample.fileName) : projectConfig.root_data_path.resolve(storageSample.folderName).resolve(storageSample.fileName);
		Sample2 sample = new Sample2(Integer.toString(storageSample.sampleId), projectConfig.project, metaPath, samplePath, storageSample.locationName, storageSample.timestamp, storageSample.deviceName);
		return sample;
	}

	public void forEachZonedDateAtLocationName(int timeZoneOffsetSeconds, String locationName, LongConsumer consumer) {
		String loc = locationName == null ? MISSING_LOCATION : locationName;
		int locationId = tlSampleStorageConnector.get().getLocationId(loc);
		tlSampleStorageConnector.get().forEachZonedDateAtLocationId(timeZoneOffsetSeconds, locationId, consumer);
	}

	public int getSampleCountAtLocationName(long start, long end, String locationName) {		
		String loc = locationName == null ? MISSING_LOCATION : locationName;
		int locationId = tlSampleStorageConnector.get().getLocationId(loc);
		return tlSampleStorageConnector.get().getSampleCountAtLocationId(start, end, locationId);
	}

	public void forEachOrderedSampleIdAtLocationName(long start, long end, String locationName, IntConsumer consumer, int limit, int offset) {
		String loc = locationName == null ? MISSING_LOCATION : locationName;
		int locationId = tlSampleStorageConnector.get().getLocationId(loc);
		tlSampleStorageConnector.get().forEachOrderedSampleAtLocationId(start, end, locationId, consumer, limit, offset);
	}

	public void forEachOrderedSampleAtLocationName(long start, long end, String locationName, Consumer<StorageSample> consumer, int limit, int offset) {
		forEachOrderedSampleIdAtLocationName(start, end, locationName, sampleId -> {
			StorageSample sample = getStorageSample(sampleId);
			consumer.accept(sample);
		}, limit, offset);
	}

	public void forEachOrderedSample(long start, long end, Consumer<Sample2> consumer, int limit, int offset) {
		tlSampleStorageConnector.get().forEachOrderedSampleId(start, end, sampleId -> {
			Sample2 sample = getSample(sampleId);
			consumer.accept(sample);
		}, limit, offset);
	}

	public void forEachOrderedSample(Consumer<Sample2> consumer) {
		forEachOrderedSample(Long.MIN_VALUE, Long.MAX_VALUE, consumer, Integer.MAX_VALUE, 0);
	}

	public void compact() {
		Logger.info("SHUTDOWN COMPACT start");
		tlSampleStorageConnector.get().compact();		
		Logger.info("SHUTDOWN COMPACT done. Database is closed now. AudioDB should be manually terminated and startet again.");
	}
}
