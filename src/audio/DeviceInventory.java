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
import util.AudioTimeUtil;

public class DeviceInventory {
	private static final Logger log = LogManager.getLogger();

	private static Entry[] EMPTY = new Entry[] {};

	public static class Entry {
		public final String device; // not null
		public final String location;
		public final long start;
		public final long end;

		public Entry(String device, String location, long start, long end) {
			this.device = device;
			this.location = location;
			this.start = start;
			this.end = end;
		}

		public boolean contains(long timestamp) {
			return start <= timestamp && timestamp <= end; 		
		}

		@Override
		public String toString() {
			return "Entry [device=" + device + ", location=" + location + ", start=" + AudioTimeUtil.toString(start) + ", end=" + AudioTimeUtil.toString(end) + "]";
		}

		public boolean isInfinite() {
			return start == Long.MIN_VALUE && end == Long.MAX_VALUE;
		}
	}

	private final Path deviceInventoryPath;
	private final Map<String, Object> deviceMap = new HashMap<String, Object>();

	private void clear() {
		deviceMap.clear();
	}

	private void insert(Entry entry) {
		//log.info("insert " + entry);
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
			CsvCell cellStart = csvTable.getCell("start");
			CsvCell cellEnd = csvTable.getCell("end");
			csvTable.forEach(csvRow -> {
				try {
					String device = cellDevice.get(csvRow);
					String location = cellLocation.get(csvRow);
					String startText = cellStart.get(csvRow);
					String endText = cellEnd.get(csvRow);
					long start = AudioTimeUtil.toAudiotimeStart(startText);
					long end = AudioTimeUtil.toAudiotimeEnd(endText);
					Entry entry = new Entry(device, location, start, end);				
					insert(entry);
				} catch(Exception e) {
					log.warn(e);
				}
			});
		} catch(Exception e) {
			log.warn(e);
		}
		//forEach(entry -> log.info(entry));
	}

	public Entry getLast(String device, long timestamp) {
		Entry entry = null;
		Object o = deviceMap.get(device);
		if(o == null) {
			// nothing
		} else if(o instanceof Entry) {
			Entry e = (Entry) o;
			if(e.contains(timestamp)) {
				entry = e;
			}
		} else {
			Entry[] entries = (Entry[]) o;
			int entriesLen = entries.length;
			for(int i = entriesLen - 1; i >= 0; i--) {
				Entry e = entries[i];
				if(e.contains(timestamp)) {
					entry = e;
					break;
				}
			}
		}
		return entry;
	}

	public Entry getLastInfinite(String device) {
		Entry entry = null;
		Object o = deviceMap.get(device);
		if(o == null) {
			// nothing
		} else if(o instanceof Entry) {
			Entry e = (Entry) o;
			if(e.isInfinite()) {
				entry = e;
			}
		} else {
			Entry[] entries = (Entry[]) o;
			int entriesLen = entries.length;
			for(int i = entriesLen - 1; i >= 0; i--) {
				Entry e = entries[i];
				if(e.isInfinite()) {
					entry = e;
					break;
				}
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
