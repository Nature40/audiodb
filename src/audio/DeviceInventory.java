package audio;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;


import org.tinylog.Logger;

import photo2.CsvTable;
import photo2.CsvTable.CsvCell;
import util.AudioTimeUtil;

public class DeviceInventory {

	private static Entry[] EMPTY = new Entry[] {};

	public static class Entry {

		public static Comparator<Entry> COMPARATOR = new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				int c = Long.compare(o1.start, o2.start);				
				return c == 0 ? Long.compare(o1.end, o2.end) : 0;
			}
		};

		public final String device; // not null
		public final String location;
		public final long start;
		public final long end;

		public Entry(String device, String location, long start, long end) {
			this.device = device == null ? null : device.trim();
			this.location = location == null ? null : location.trim();
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
	private TreeMap<String, Object> deviceMap = new TreeMap<String, Object>();

	private void clear() {
		deviceMap.clear();
	}

	private void insert(Entry entry) {
		//Logger.info("insert " + entry);
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
			Logger.warn("missing inventory file: " + deviceInventoryPath);
			return;
		}
		Logger.info("device inventory read "  + deviceInventoryPath);
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
					if(device.isBlank() || device.equals("NA")) {
						Logger.info("skip row with missing device " + csvRow);
					} else if(location.isBlank() || location.equals("NA")) {
						Logger.info("skip row with missing location " + csvRow);
					} else if(startText.equals("NA")) {
						Logger.info("skip row with start NA " + csvRow);
					} else if(endText.equals("NA")) {
						Logger.info("skip row with end NA " + csvRow);
					} else{
						long start = AudioTimeUtil.toAudiotimeStart(startText);
						long end = AudioTimeUtil.toAudiotimeEnd(endText);
						if(start <= end) {
							Entry entry = new Entry(device, location, start, end);				
							insert(entry);
						} else {
							Logger.warn("start higher than end not inserted " + csvRow);
						}
					}
				} catch(Exception e) {
					Logger.warn(e);
				}
			});
		} catch(Exception e) {
			Logger.warn(e);
		}
		consolidate();
		sort();
		validate();
		//forEach(entry -> Logger.info(entry));
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

	private void consolidate() {
		TreeMap<String, Object> m = new TreeMap<String, Object>();
		deviceMap.forEach((deviceName, o) -> {			
			if(o instanceof Entry) {
				Entry entry = (Entry) o;
				m.put(deviceName, entry);
			} else {
				Entry[] v = (Entry[]) o;
				Entry[] a = v.clone();
				int removeCount = 0;
				for (int i = 0; i < a.length; i++) {
					Entry x = a[i];
					inner: for (int j = i + 1; j < a.length; j++) {
						Entry y = a[j];
						if(
								x.location.equals(y.location) 
								&& (x.start <= (y.end + 1) || x.start <= y.end) // infinity wrap around check
								&& ((x.end + 1) >= y.start || x.end >= y.start) // infinity wrap around check
								) {
							Entry z = new Entry(deviceName, x.location, Math.min(x.start, y.start), Math.max(x.end, y.end));
							a[i] = null;
							removeCount++;
							a[j] = z;
							//Logger.info("consolidated one row");
							break inner;
						}
					}
				}
				if(removeCount == 0) {
					m.put(deviceName, a);
				} else {
					int bLen = a.length - removeCount;
					if(bLen == 0) {
						throw new RuntimeException("error");
					} else if(bLen == 1) {
						for (int i = 0; i < a.length; i++) {
							Entry x = a[i];
							if(x != null) {
								m.put(deviceName, x);
								break;
							}
						}
					} else {
						Entry[] b = new Entry[bLen];
						int bPos = 0;
						for (int i = 0; i < a.length; i++) {
							Entry x = a[i];
							if(x != null) {
								b[bPos++] = x;
							}
						}
						m.put(deviceName, b);
					}
				}
			}			
		});
		deviceMap = m;
	}

	private void sort() {
		deviceMap.forEach((deviceName, o) -> {			
			if(o instanceof Entry) {
				// nothing
			} else {
				Entry[] a = (Entry[]) o;
				Arrays.sort(a, Entry.COMPARATOR);
			}			
		});		
	}

	private void validate() {
		deviceMap.forEach((deviceName, o) -> {			
			if(o instanceof Entry) {
				// nothing
			} else {
				Entry[] a = (Entry[]) o;
				for (int i = 0; i < a.length; i++) {
					Entry x = a[i];
					for (int j = i + 1; j < a.length; j++) {
						Entry y = a[j];
						if(x.start <= y.end && x.end >= y.start) {
							if(x.location.equals(y.location)) {
								Logger.warn("not consolidated " + x + " " +y);
							} else {
								Logger.warn("inconsistency overlapping " + x + " " +y);
							}
						}
					}
				}
			}			
		});		
	}
}
