package audio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.apache.commons.lang3.tuple.Pair;
import org.tinylog.Logger;

public class SampleStorageConnector {
	private static LinkedHashMap<String, String[]> TABLE_MAP = new LinkedHashMap<String, String[]>();

	static {
		TABLE_MAP.put("SAMPLE", new String[] {
				"SAMPLE_ID INT PRIMARY KEY",
				"TIMESTAMP BIGINT NOT NULL",
				"DEVICE_ID INT NOT NULL ",
				"LOCATION_ID INT NOT NULL ",
		});

		TABLE_MAP.put("ORDERED_SAMPLE", new String[] {
				"ROW_ID INT NOT NULL PRIMARY KEY",
				"SAMPLE_ID INT NOT NULL",
				"TIMESTAMP BIGINT NOT NULL",
				/*"DEVICE_ID INT NOT NULL ",*/
				"LOCATION_ID INT NOT NULL ",
		});

		TABLE_MAP.put("FOLDER", new String[] {
				"FOLDER_ID INT AUTO_INCREMENT PRIMARY KEY",
				"FOLDER_NAME VARCHAR(255) NOT NULL",
		});

		TABLE_MAP.put("FILE", new String[] {
				"SAMPLE_ID INT AUTO_INCREMENT PRIMARY KEY",
				"FOLDER_ID INT NOT NULL",
				"FILE_NAME VARCHAR(255) NOT NULL",
				"LAST_MODIFIED BIGINT NOT NULL",
		});

		TABLE_MAP.put("DEVICE", new String[] {
				"DEVICE_ID INT AUTO_INCREMENT PRIMARY KEY",
				"DEVICE_NAME VARCHAR(255) NOT NULL",
		});

		TABLE_MAP.put("LOCATION", new String[] {
				"LOCATION_ID INT AUTO_INCREMENT PRIMARY KEY",
				"LOCATION_NAME VARCHAR(255) NOT NULL",
		});

		TABLE_MAP.put("EXCLUDED_FILE", new String[] {
				"FOLDER_ID INT NOT NULL",
				"FILE_NAME VARCHAR(255) NOT NULL",
				"LAST_MODIFIED BIGINT NOT NULL",
				"PRIMARY KEY (FOLDER_ID, FILE_NAME)",
		});
	}

	private final String[][] INDICES = new String[][] {
		{"UNIQUE", "IDX_FOLDER", "FOLDER", "FOLDER_NAME"},
		{"UNIQUE", "IDX_FILE", "FILE", "FOLDER_ID, FILE_NAME"},
		{"UNIQUE", "IDX_DEVICE", "DEVICE", "DEVICE_NAME"},
		{"UNIQUE", "IDX_LOCATION", "LOCATION", "LOCATION_NAME"},
	};

	private static enum SQL {

		QUERY_FOLDER_ID_BY_FOLDER_NAME("SELECT FOLDER_ID FROM FOLDER WHERE FOLDER_NAME = ?"),		
		QUERY_SAMPLE_ID_BY_FILE("SELECT SAMPLE_ID, LAST_MODIFIED FROM FILE WHERE FOLDER_ID = ? AND FILE_NAME = ?"),
		QUERY_DEVICE_ID_BY_DEVICE_NAME("SELECT DEVICE_ID FROM DEVICE WHERE DEVICE_NAME = ?"),
		QUERY_LOCATION_ID_BY_LOCATION_NAME("SELECT LOCATION_ID FROM LOCATION WHERE LOCATION_NAME = ?"),

		EXIST_SAMPLE_ID("SELECT count(*) FROM SAMPLE WHERE SAMPLE_ID = ?"),
		COUNT_SAMPLE_ID("SELECT count(*) FROM SAMPLE"),

		QUERY_SAMPLE(
				"SELECT SAMPLE.TIMESTAMP, DEVICE.DEVICE_NAME, LOCATION.LOCATION_NAME",
				"FROM SAMPLE JOIN DEVICE ON SAMPLE.DEVICE_ID = DEVICE.DEVICE_ID JOIN LOCATION ON SAMPLE.LOCATION_ID = LOCATION.LOCATION_ID", 
				"WHERE SAMPLE.SAMPLE_ID = ?"
				),

		QUERY_FILE(
				"SELECT FOLDER.FOLDER_NAME, FILE.FILE_NAME, FILE.LAST_MODIFIED",
				"FROM FILE JOIN FOLDER ON FILE.FOLDER_ID = FOLDER.FOLDER_ID", 
				"WHERE FILE.SAMPLE_ID = ?"
				),


		INSERT_SAMPLE("INSERT INTO SAMPLE (SAMPLE_ID, TIMESTAMP, DEVICE_ID, LOCATION_ID) VALUES (?, ?, ?, ?)"),
		MERGE_SAMPLE("MERGE INTO SAMPLE (SAMPLE_ID, TIMESTAMP, DEVICE_ID, LOCATION_ID) VALUES (?, ?, ?, ?)"),

		UPDATE_FILE("UPDATE FILE SET LAST_MODIFIED=? WHERE SAMPLE_ID=?"),

		QUERY_LOCATION_NAME("SELECT LOCATION_NAME FROM LOCATION ORDER BY LOCATION_NAME"),

		QUERY_DEVICE_NAME("SELECT DEVICE_NAME FROM DEVICE ORDER BY DEVICE_NAME"),

		/*CLEAR_ODERED_SAMPLE("DELETE FROM ORDERED_SAMPLE"),*/

		/*FILL_ODERED_SAMPLE(
				"INSERT INTO ORDERED_SAMPLE (ROW_ID, SAMPLE_ID, TIMESTAMP, DEVICE_ID, LOCATION_ID)",
				"SELECT ROW_NUMBER() OVER (ORDER BY TIMESTAMP, LOCATION_ID) as ROW_ID, SAMPLE_ID, TIMESTAMP, DEVICE_ID, LOCATION_ID",
				"FROM SAMPLE"
				),*/

		/*FILL_ODERED_SAMPLE(
				"INSERT INTO ORDERED_SAMPLE (ROW_ID, SAMPLE_ID, TIMESTAMP, DEVICE_ID, LOCATION_ID)",
				"SELECT ROW_NUMBER() OVER (ORDER BY LOCATION_ID, TIMESTAMP) as ROW_ID, SAMPLE_ID, TIMESTAMP, DEVICE_ID, LOCATION_ID",
				"FROM SAMPLE"
				),*/
		
		FILL_ODERED_SAMPLE(
				"INSERT INTO ORDERED_SAMPLE (ROW_ID, SAMPLE_ID, TIMESTAMP, LOCATION_ID)",
				"SELECT ROW_NUMBER() OVER (ORDER BY LOCATION_ID, TIMESTAMP) as ROW_ID, SAMPLE_ID, TIMESTAMP, LOCATION_ID",
				"FROM SAMPLE"
				),

		QUERY_DATES("SELECT DISTINCT ((TIMESTAMP + ?) - ((TIMESTAMP + ?) % 86400)) AS DATE FROM ORDERED_SAMPLE"),

		QUERY_DATES_AT_LOCATION_ID("SELECT DISTINCT ((TIMESTAMP + ?) - ((TIMESTAMP + ?) % 86400)) AS DATE FROM ORDERED_SAMPLE WHERE LOCATION_ID = ?"),

		COUNT_ORDERED_SAMPLES("SELECT COUNT(*) FROM ORDERED_SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ?"),

		QUERY_ORDERED_SAMPLES("SELECT SAMPLE_ID FROM ORDERED_SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ? LIMIT ? OFFSET ?"),

		COUNT_ORDERED_SAMPLES_AT_LOCATION_ID("SELECT COUNT(*) FROM ORDERED_SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ? AND LOCATION_ID = ?"),

		QUERY_ORDERED_SAMPLES_AT_LOCATION_ID("SELECT SAMPLE_ID FROM ORDERED_SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ? AND LOCATION_ID = ? LIMIT ? OFFSET ?"),

		SHUTDOWN_COMPACT("SHUTDOWN COMPACT"),

		END_MARKER("");

		public final String sql;

		SQL(String... sql) {
			this.sql = sql.length == 1 ? sql[0] : String.join(" ", sql);
		}
	}

	private static enum SQL_GK {

		INSERT_FOLDER_NAME("INSERT INTO FOLDER (FOLDER_NAME) VALUES (?)"),
		INSERT_FILE("INSERT INTO FILE (FOLDER_ID, FILE_NAME, LAST_MODIFIED) VALUES (?, ?, ?)"),
		INSERT_DEVICE_NAME("INSERT INTO DEVICE (DEVICE_NAME) VALUES (?)"),
		INSERT_LOCATION_NAME("INSERT INTO LOCATION (LOCATION_NAME) VALUES (?)");

		public final String sql;

		SQL_GK(String sql) {
			this.sql = sql;
		}
	}

	public static class TlSampleStorageConnector extends ThreadLocal<SampleStorageConnector> {

		private final Connection conn;

		public TlSampleStorageConnector(Connection conn) {
			this.conn = conn;
		}

		@Override
		public SampleStorageConnector initialValue() {
			return new SampleStorageConnector(conn);
		}
	}

	private final Connection conn;

	private final EnumMap<SQL, PreparedStatement> statementMap = new EnumMap<SQL, PreparedStatement>(SQL.class);
	private final EnumMap<SQL_GK, PreparedStatement> statementMap_GK = new EnumMap<SQL_GK, PreparedStatement>(SQL_GK.class);

	private SampleStorageConnector(Connection conn) {
		this.conn = conn;
	}

	private PreparedStatement createStatement(SQL sql) {
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.sql);
			return stmt;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}				
	}

	private PreparedStatement createStatement_GK(SQL_GK sql) {
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.sql, Statement.RETURN_GENERATED_KEYS);
			return stmt;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}				
	}

	private PreparedStatement getStatement(SQL sql) {	
		return statementMap.computeIfAbsent(sql, this::createStatement);
	}

	private PreparedStatement getStatement(SQL_GK sql) {	
		return statementMap_GK.computeIfAbsent(sql, this::createStatement_GK);
	}

	public void init(boolean clear) {
		try {
			if(clear) {
				for(String table:TABLE_MAP.keySet()) {
					if(tableExists(table)) {
						dropTable(table);
					}
				}
			}
			for(Entry<String, String[]> entry : TABLE_MAP.entrySet()) {
				String table = entry.getKey();
				if(!tableExists(table)) {
					String[] colDefs = entry.getValue();
					createTable(table, colDefs);
				}
			}


			for(String[] index:INDICES) {
				createIndex(index);
			}

			/*getStatement(SQL.CREATE_INDEX_FOLDER).executeUpdate();
			getStatement(SQL.CREATE_INDEX_FILE).executeUpdate();
			getStatement(SQL.CREATE_INDEX_DEVICE).executeUpdate();
			getStatement(SQL.CREATE_INDEX_LOCATION).executeUpdate();*/
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void createIndex(String[] index) throws SQLException {
		String sql = String.format("CREATE %s INDEX IF NOT EXISTS %s ON %s (%s)", index[0], index[1], index[2], index[3]);
		Logger.info(sql);
		conn.prepareStatement(sql).executeUpdate();
	}

	private boolean tableExists(String name) throws SQLException {
		ResultSet res = conn.getMetaData().getTables(null, null, name, null);
		return res.next();		
	}

	private void dropTable(String name) throws SQLException {
		conn.createStatement().executeUpdate(String.format("DROP TABLE IF EXISTS %s", name));
	}

	private void createTable(String name, String[] colDefs) throws SQLException {		
		String cDef = String.join(", ", colDefs);		
		conn.createStatement().executeUpdate(String.format("CREATE TABLE IF NOT EXISTS %s (%s)", name, cDef));
	}

	public int getOrInsertFolderId(String folder_name) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_FOLDER_ID_BY_FOLDER_NAME);
			stmt.setString(1, folder_name);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return res.getInt(1);
			} else {				
				stmt = getStatement(SQL_GK.INSERT_FOLDER_NAME);
				stmt.setString(1, folder_name);
				stmt.executeUpdate();
				res = stmt.getGeneratedKeys();
				if (res.next()) {
					return res.getInt(1);
				} else {
					throw new RuntimeException("insert key error");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Pair<Integer, Long> getOrInsertSampleIdByFile(int folderId, String fileName, long lastModifiedIfInsert) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_SAMPLE_ID_BY_FILE);
			stmt.setInt(1, folderId);
			stmt.setString(2, fileName);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return Pair.of(res.getInt(1), res.getLong(2));
			} else {
				stmt = getStatement(SQL_GK.INSERT_FILE);
				stmt.setInt(1, folderId);
				stmt.setString(2, fileName);
				stmt.setLong(3, lastModifiedIfInsert);
				stmt.executeUpdate();
				res = stmt.getGeneratedKeys();
				if (res.next()) {
					return Pair.of(res.getInt(1), lastModifiedIfInsert);
				} else {
					throw new RuntimeException("insert key error");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateFile(int sampleId, long fileLastModified) {
		try {
			PreparedStatement stmt = getStatement(SQL.UPDATE_FILE);
			stmt.setLong(1, fileLastModified);	
			stmt.setInt(2, sampleId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}	
	}

	public boolean existSample(int sampleId) {
		try {
			PreparedStatement stmt = getStatement(SQL.EXIST_SAMPLE_ID);
			stmt.setInt(1, sampleId);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return res.getInt(1) > 0;
			} else {				
				throw new RuntimeException("sql query error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void insertSample(int sampleId, long timestamp, int deviceId, int locationId) {
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT_SAMPLE);
			stmt.setInt(1, sampleId);
			stmt.setLong(2, timestamp);
			stmt.setInt(3, deviceId);
			stmt.setInt(4, locationId);			
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void setSample(int sampleId, long timestamp, int deviceId, int locationId) {
		try {
			PreparedStatement stmt = getStatement(SQL.MERGE_SAMPLE);
			stmt.setInt(1, sampleId);
			stmt.setLong(2, timestamp);
			stmt.setInt(3, deviceId);
			stmt.setInt(4, locationId);			
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int getOrInsertDeviceId(String deviceName) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_DEVICE_ID_BY_DEVICE_NAME);
			stmt.setString(1, deviceName);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return res.getInt(1);
			} else {				
				stmt = getStatement(SQL_GK.INSERT_DEVICE_NAME);
				stmt.setString(1, deviceName);
				stmt.executeUpdate();
				res = stmt.getGeneratedKeys();
				if (res.next()) {
					return res.getInt(1);
				} else {
					throw new RuntimeException("insert key error");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int getOrInsertLocationId(String locationName) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_LOCATION_ID_BY_LOCATION_NAME);
			stmt.setString(1, locationName);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return res.getInt(1);
			} else {				
				stmt = getStatement(SQL_GK.INSERT_LOCATION_NAME);
				stmt.setString(1, locationName);
				stmt.executeUpdate();
				res = stmt.getGeneratedKeys();
				if (res.next()) {
					return res.getInt(1);
				} else {
					throw new RuntimeException("insert key error");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int getLocationId(String locationName) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_LOCATION_ID_BY_LOCATION_NAME);
			stmt.setString(1, locationName);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return res.getInt(1);
			} else {				
				throw new RuntimeException("location not found");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachLocation(Consumer<String> consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_LOCATION_NAME);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String location = res.getString(1);
				consumer.accept(location);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachDevice(Consumer<String> consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_DEVICE_NAME);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String deviceName = res.getString(1);
				consumer.accept(deviceName);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int getSampleCount() {
		try {
			PreparedStatement stmt = getStatement(SQL.COUNT_SAMPLE_ID);
			ResultSet res = stmt.executeQuery();
			if(!res.next()) {
				throw new RuntimeException("no result");
			}
			return res.getInt(1);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/*private void clearOrderedSample() {
		try {
			PreparedStatement stmt = getStatement(SQL.CLEAR_ODERED_SAMPLE);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}*/

	public void refreshOrderedSample() {
		try {
			dropTable("ORDERED_SAMPLE");
			createTable("ORDERED_SAMPLE", TABLE_MAP.get("ORDERED_SAMPLE"));
			
			getStatement(SQL.FILL_ODERED_SAMPLE).executeUpdate();			
			
			createIndex(new String[]{"", "IDX_ORDERED_SAMPLE_LOCATION_ID", "ORDERED_SAMPLE", "LOCATION_ID"});
			/*{"", "IDX_ORDERED_SAMPLE_TIMESTAMP", "ORDERED_SAMPLE", "TIMESTAMP"},
			{"", "IDX_ORDERED_SAMPLE_LOCATION_ID", "ORDERED_SAMPLE", "LOCATION_ID"},*/
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachZonedDate(int timeZoneOffsetSeconds, LongConsumer consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_DATES);
			stmt.setInt(1, timeZoneOffsetSeconds);
			stmt.setInt(2, timeZoneOffsetSeconds);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				long timestamp = res.getLong(1) - timeZoneOffsetSeconds; // convert back to UTC
				consumer.accept(timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachZonedDateAtLocationId(int timeZoneOffsetSeconds, int locationId, LongConsumer consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_DATES_AT_LOCATION_ID);
			stmt.setInt(1, timeZoneOffsetSeconds);
			stmt.setInt(2, timeZoneOffsetSeconds);
			stmt.setInt(3, locationId);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				long timestamp = res.getLong(1) - timeZoneOffsetSeconds; // convert back to UTC
				consumer.accept(timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int getSampleCount(long start, long end) {		
		try {
			PreparedStatement stmt = getStatement(SQL.COUNT_ORDERED_SAMPLES);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			ResultSet res = stmt.executeQuery();
			if(!res.next()) {
				throw new RuntimeException("no result");
			}
			return res.getInt(1);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int getSampleCountAtLocationId(long start, long end, int locationId) {		
		try {
			PreparedStatement stmt = getStatement(SQL.COUNT_ORDERED_SAMPLES_AT_LOCATION_ID);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			stmt.setInt(3, locationId);
			ResultSet res = stmt.executeQuery();
			if(!res.next()) {
				throw new RuntimeException("no result");
			}
			return res.getInt(1);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachOrderedSampleId(long start, long end, IntConsumer consumer, int limit, int offset) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ORDERED_SAMPLES);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			stmt.setInt(3, limit);
			stmt.setInt(4, offset);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int sampleId = res.getInt(1);
				consumer.accept(sampleId);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachOrderedSampleAtLocationId(long start, long end, int locationId, IntConsumer consumer, int limit, int offset) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ORDERED_SAMPLES_AT_LOCATION_ID);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			stmt.setInt(3, locationId);
			stmt.setInt(4, limit);
			stmt.setInt(5, offset);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int sampleId = res.getInt(1);
				consumer.accept(sampleId);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static class StorageSample {

		public final int sampleId;

		public long timestamp;
		public String deviceName;
		public String locationName;
		public String folderName;
		public String fileName;
		public long lastModified;		

		StorageSample(int sampleId) {
			this.sampleId = sampleId;
		}

		@Override
		public String toString() {
			return "StorageSample [sampleId=" + sampleId + ", timestamp=" + timestamp + ", deviceName=" + deviceName
					+ ", locationName=" + locationName + ", folderName=" + folderName + ", fileName=" + fileName
					+ ", lastModified=" + lastModified + "]";
		}
	}

	public StorageSample getSample(int sampleId) {
		StorageSample storageSample = new StorageSample(sampleId);
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_SAMPLE);
			stmt.setInt(1, sampleId);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				storageSample.timestamp = res.getLong(1);
				storageSample.deviceName = res.getString(2);
				storageSample.locationName = res.getString(3);
			} else {				
				throw new RuntimeException("sql query error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_FILE);
			stmt.setInt(1, sampleId);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				storageSample.folderName = res.getString(1);
				storageSample.fileName = res.getString(2);
				storageSample.lastModified = res.getLong(3);
			} else {				
				throw new RuntimeException("sql query error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return storageSample;
	}

	public void compact() {
		try {
			PreparedStatement stmt = getStatement(SQL.SHUTDOWN_COMPACT);		
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
