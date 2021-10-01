package audio;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import photo2.CsvTable;
import photo2.CsvTable.CsvCell;

public class DeviceInventory {
	private static final Logger log = LogManager.getLogger();

	private static Entry[] EMPTY = new Entry[] {};

	public static class Entry {
		public final String device; // not null
		public final String location;

		public Entry(String device, String location) {
			this.device = device;
			this.location = location;
		}

		@Override
		public String toString() {
			return "Entry [device=" + device + ", location=" + location + "]";
		}
	}

	private final Path deviceInventoryPath;
	private final Map<String, Object> deviceMap = new HashMap<String, Object>();

	private void clear() {
		deviceMap.clear();
	}

	private void insert(Entry entry) {
		String key = entry.device;
		Object o = deviceMap.get(key);
		if(o == null) {
			deviceMap.put(key, entry);
		} else if(o instanceof Entry) {
			Entry e = (Entry) o;
			Entry[] v = new Entry[] {e, entry};
			deviceMap.put(key, v);
		} else {
			Entry[] v = (Entry[]) o;
			int vLen = v.length;
			Entry[] w = Arrays.copyOf(v, vLen + 1);
			w[vLen] = entry;
			deviceMap.put(key, w);
		}
	}

	public DeviceInventory(Path deviceInventoryPath) {
		this.deviceInventoryPath = deviceInventoryPath;
		read();
	}

	private void read() {
		clear();
		if(deviceInventoryPath == null) {
			return;
		}
		if(!deviceInventoryPath.toFile().exists()) {
			log.warn("missing inventory file: " + deviceInventoryPath);
			return;
		}
		log.info("device inventroy read "  + deviceInventoryPath);
		try(CsvTable csvTable = new CsvTable(deviceInventoryPath)) {
			CsvCell cellDevice = csvTable.getCell("device");
			CsvCell cellLocation = csvTable.getCell("location");	
			csvTable.forEach(csvRow -> {
				String device = cellDevice.get(csvRow);
				String location = cellLocation.get(csvRow);
				Entry entry = new Entry(device, location);
				insert(entry);
			});
		} catch(Exception e) {
			log.warn(e);
		}
		forEach(entry -> log.info(entry));
	}

	public Entry getLast(String device) {
		Entry entry = null;
		Object o = deviceMap.get(device);
		if(o == null) {
			// nothing
		} else if(o instanceof Entry) {
			entry = (Entry) o;
		} else {
			Entry[] v = (Entry[]) o;
			for(Entry e : v) {
				entry = e;	
			}
		}
		return entry;
	}

	public Entry[] getMulti(String device) {
		Entry[] entries;
		Object o = deviceMap.get(device);
		if(o == null) {
			entries = EMPTY;
		} else if(o instanceof Entry) {
			entries = new Entry[] {(Entry) o};
		} else {
			Entry[] v = (Entry[]) o;
			entries = v.clone();
		}
		return entries;
	}

	public void forEach(Consumer<Entry> consumer) {
		for(Object o : deviceMap.values()) {
			if(o instanceof Entry) {
				Entry entry = (Entry) o;
				consumer.accept(entry);
			} else {
				Entry[] v = (Entry[]) o;
				for(Entry entry : v) {
					consumer.accept(entry);
				}
			}
		}
	}
}
