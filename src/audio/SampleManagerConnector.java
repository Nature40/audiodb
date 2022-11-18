package audio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.tinylog.Logger;

public class SampleManagerConnector {	

	public static enum SQL {
		DROP_TABLE("DROP TABLE IF EXISTS SAMPLE"),

		CREATE_TABLE("CREATE TABLE IF NOT EXISTS SAMPLE " +
				"(" +
				"ID VARCHAR(255) PRIMARY KEY, " +
				"PROJECT VARCHAR(255), " +
				"META_PATH VARCHAR(255), " +
				"SAMPLE_PATH VARCHAR(255), " +
				"LOCATION VARCHAR(255), " +
				"TIMESTAMP BIGINT, " +
				"LAST_MODIFIED BIGINT, " +
				"LOCKED BOOLEAN, " +
				"DEVICE VARCHAR(255) " +
				")"),

		CREATE_INDEX_PROJECT("CREATE INDEX IF NOT EXISTS IDX_PROJECT ON SAMPLE (PROJECT)"),
		CREATE_INDEX_LOCATION("CREATE INDEX IF NOT EXISTS IDX_LOCATION ON SAMPLE (LOCATION)"),
		CREATE_INDEX_DEVICE("CREATE INDEX IF NOT EXISTS IDX_DEVICE ON SAMPLE (DEVICE)"),
		CREATE_INDEX_TIMESTAMP("CREATE INDEX IF NOT EXISTS IDX_TIMESTAMP ON SAMPLE (TIMESTAMP)"),
		CREATE_INDEX_LOCKED("CREATE INDEX IF NOT EXISTS IDX_LOCKED ON SAMPLE (LOCKED)"),
		CREATE_INDEX_LOCKED_TIMESTAMP_LOCATION_ID("CREATE INDEX IF NOT EXISTS IDX_LOCKED_TIMESTAMP_LOCATION_ID ON SAMPLE (LOCKED, TIMESTAMP, LOCATION, ID)"),

		INSERT("INSERT INTO SAMPLE " +
				"(ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE) " +
				"VALUES " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?)"),

		UPDATE("UPDATE SAMPLE SET PROJECT = ?, META_PATH = ?, SAMPLE_PATH = ?, LOCATION = ?, TIMESTAMP = ?, LAST_MODIFIED = ?, LOCKED = ?, DEVICE = ? WHERE ID = ?"),

		DELETE("DELETE FROM SAMPLE WHERE ID = ?"),

		DELETE_PROJECT("DELETE FROM SAMPLE WHERE PROJECT = ?"),

		QUERY_IS_UP_TO_DATE("SELECT EXISTS (SELECT 1 FROM SAMPLE WHERE ID = ? AND LAST_MODIFIED = ?)"),

		QUERY_EXIST("SELECT 1 FROM SAMPLE WHERE ID = ?"),

		DROP_TRAVERSE_TABLE("DROP TABLE IF EXISTS TRAVERSE"),

		CREATE_TRAVERSE_TABLE("CREATE TEMPORARY TABLE IF NOT EXISTS TRAVERSE " +
				"(" +
				"ID VARCHAR(255) PRIMARY KEY " +
				") NOT PERSISTENT"),

		INSERT_TRAVERSE("INSERT INTO TRAVERSE " +
				"(ID) " +
				"VALUES " +
				"(?)"),

		DELETE_TRAVERSE_MISSING("DELETE FROM SAMPLE WHERE NOT EXISTS ( SELECT 1 FROM TRAVERSE WHERE SAMPLE.ID = TRAVERSE.ID)"),
		

		COUNT_TABLE("SELECT COUNT(1) FROM SAMPLE"),
		
		COUNT_ALL("SELECT COUNT(ID) FROM SAMPLE WHERE NOT LOCKED"),

		
		COUNT_AT_TIMERANGE("SELECT COUNT(ID) FROM SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED"),
		

		QUERY_ALL("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE USE INDEX (IDX_LOCKED_TIMESTAMP_LOCATION_ID) WHERE NOT LOCKED ORDER BY TIMESTAMP, LOCATION"),

		QUERY_AT_TIMERANGE("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ?  AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION"),

		QUERY_ALL_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE USE INDEX (IDX_LOCKED_TIMESTAMP_LOCATION_ID) WHERE NOT LOCKED ORDER BY TIMESTAMP, LOCATION LIMIT ? OFFSET ?"),

		QUERY_AT_TIMERANGE_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ?  AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION LIMIT ? OFFSET ?"),
		

		COUNT_AT_LOCATION("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION = ? AND NOT LOCKED"),

		COUNT_AT_LOCATION_AT_TIMERANGE("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION = ? AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED"),

		COUNT_AT_LOCATION_NULL("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION IS NULL AND NOT LOCKED"),

		COUNT_AT_LOCATION_NULL_AT_TIMERANGE("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION IS NULL AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED"),
		

		QUERY_AT_LOCATION("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION = ? AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION"),

		QUERY_AT_LOCATION_AT_TIMERANGE("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION = ? AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION"),

		QUERY_AT_LOCATION_NULL("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION IS NULL AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION"),

		QUERY_AT_LOCATION_NULL_AT_TIMERANGE("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION IS NULL AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION"),

		QUERY_AT_LOCATION_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION = ? AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION LIMIT ? OFFSET ?"),

		QUERY_AT_LOCATION_AT_TIMERANGE_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION = ? AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION LIMIT ? OFFSET ?"),		

		QUERY_AT_LOCATION_NULL_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION IS NULL AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION LIMIT ? OFFSET ?"),

		QUERY_AT_LOCATION_NULL_AT_TIMERANGE_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE LOCATION IS NULL AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED ORDER BY TIMESTAMP, LOCATION LIMIT ? OFFSET ?"),
		
		
		QUERY_BY_ID("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED, DEVICE FROM SAMPLE WHERE ID = ? AND NOT LOCKED"),

		
		QUERY_LOCATIONS_ALL("SELECT DISTINCT LOCATION FROM SAMPLE ORDER BY LOCATION"),

		
		QUERY_TIMESTAMPS_ALL("SELECT DISTINCT TIMESTAMP FROM SAMPLE ORDER BY TIMESTAMP"),
		
		QUERY_DATES_ALL("SELECT DISTINCT (TIMESTAMP - (TIMESTAMP % 86400)) AS DATE FROM SAMPLE ORDER BY DATE"),

		QUERY_TIMESTAMPS_AT_LOCATION("SELECT DISTINCT TIMESTAMP FROM SAMPLE WHERE LOCATION = ? ORDER BY TIMESTAMP"),
		
		QUERY_DATES_AT_LOCATION("SELECT DISTINCT (TIMESTAMP - (TIMESTAMP % 86400)) AS DATE FROM SAMPLE WHERE LOCATION = ? ORDER BY DATE"),

		QUERY_TIMESTAMPS_AT_LOCATION_NULL("SELECT DISTINCT TIMESTAMP FROM SAMPLE WHERE LOCATION IS NULL ORDER BY TIMESTAMP"),
		
		QUERY_DATES_AT_LOCATION_NULL("SELECT DISTINCT (TIMESTAMP - (TIMESTAMP % 86400)) AS DATE FROM SAMPLE WHERE LOCATION IS NULL ORDER BY DATE"),
		
		
		QUERY_DEVICES_ALL("SELECT DISTINCT DEVICE FROM SAMPLE WHERE NOT LOCKED ORDER BY DEVICE"),
		
		QUERY_DEVICES_AT_TIMERANGE("SELECT DISTINCT DEVICE FROM SAMPLE WHERE TIMESTAMP >= ? AND TIMESTAMP <= ?  AND NOT LOCKED ORDER BY DEVICE"),

		QUERY_DEVICES_AT_LOCATION_NULL("SELECT DISTINCT DEVICE FROM SAMPLE WHERE LOCATION IS NULL AND NOT LOCKED ORDER BY DEVICE"),
		
		QUERY_DEVICES_AT_LOCATION("SELECT DISTINCT DEVICE FROM SAMPLE WHERE LOCATION = ? AND NOT LOCKED ORDER BY DEVICE"),
		
		QUERY_DEVICES_AT_LOCATION_NULL_AT_TIMERANGE("SELECT DISTINCT DEVICE FROM SAMPLE WHERE LOCATION IS NULL AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED ORDER BY DEVICE"),
		
		QUERY_DEVICES_AT_LOCATION_AT_TIMERANGE("SELECT DISTINCT DEVICE FROM SAMPLE WHERE LOCATION = ? AND TIMESTAMP >= ? AND TIMESTAMP <= ? AND NOT LOCKED ORDER BY DEVICE");
		

		public final String sql;

		SQL(String sql) {
			this.sql = sql;
		}
	}

	private final Connection conn;

	private final EnumMap<SQL, PreparedStatement> statementMap = new EnumMap<SQL, PreparedStatement>(SQL.class);

	public static class TlSampleManagerConnector extends ThreadLocal<SampleManagerConnector> {

		private final Connection conn;

		public TlSampleManagerConnector(Connection conn) {
			this.conn = conn;
		}

		@Override
		public SampleManagerConnector initialValue() {
			return new SampleManagerConnector(conn);
		}
	}

	private SampleManagerConnector(Connection conn) {
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

	public PreparedStatement getStatement(SQL sql) {	
		return statementMap.computeIfAbsent(sql, this::createStatement);
	}
	
	public void explain(SQL sql) {
		try {
			String text = "EXPLAIN " + sql.sql;
			Logger.info(text);
			ResultSet res = conn.createStatement().executeQuery(text);
			//Logger.info(res.getMetaData().getColumnCount());
			while(res.next()) {
				Logger.info(res.getString(1));
			}
		} catch (SQLException e) {
			Logger.warn(e);
		}
	}

	public void init(boolean clear) {
		try {
			if(clear) {
				ResultSet res = conn.getMetaData().getTables(null, null, "SAMPLE", null);
				if(res.next()) {
					Logger.info("DROP TABLE");
					getStatement(SQL.DROP_TABLE).executeUpdate();
				}
			}
			ResultSet res = conn.getMetaData().getTables(null, null, "SAMPLE", null);
			if(!res.next()) {
				getStatement(SQL.CREATE_TABLE).executeUpdate();
			}
			getStatement(SQL.CREATE_INDEX_PROJECT).executeUpdate();
			getStatement(SQL.CREATE_INDEX_LOCATION).executeUpdate();
			getStatement(SQL.CREATE_INDEX_DEVICE).executeUpdate();
			getStatement(SQL.CREATE_INDEX_TIMESTAMP).executeUpdate();
			getStatement(SQL.CREATE_INDEX_LOCKED).executeUpdate();
			getStatement(SQL.CREATE_INDEX_LOCKED_TIMESTAMP_LOCATION_ID).executeUpdate();			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void insert(String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp, long last_modified, boolean locked, String device) {
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT);		
			stmt.setString(1, id);		
			stmt.setString(2, project);		
			stmt.setString(3, meta_rel_path);
			stmt.setString(4, sample_rel_path);
			stmt.setString(5, location);
			stmt.setLong(6, timestamp);
			stmt.setLong(7, last_modified);
			stmt.setBoolean(8, locked);
			stmt.setString(9, device);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void update(String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp, long last_modified, boolean locked, String device) {
		try {
			PreparedStatement stmt = getStatement(SQL.UPDATE);
			stmt.setString(1, project);		
			stmt.setString(2, meta_rel_path);
			stmt.setString(3, sample_rel_path);
			stmt.setString(4, location);
			stmt.setLong(5, timestamp);
			stmt.setLong(6, last_modified);
			stmt.setBoolean(7, locked);
			stmt.setString(8, device);
			stmt.setString(9, id);			
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteSample(String id) {
		try {
			PreparedStatement stmt = getStatement(SQL.DELETE);
			stmt.setString(1, id);			
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteProject(String project) {
		try {
			PreparedStatement stmt = getStatement(SQL.DELETE_PROJECT);
			stmt.setString(1, project);			
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@FunctionalInterface
	public interface SampleRowConsumer {
		void accept(String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp, long lastModified, boolean locked, String device);
	}

	public void forEachRow(ResultSet res, SampleRowConsumer consumer) {
		try {
			while(res.next()) {
				consumeRow(res, consumer);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachString(ResultSet res, Consumer<String> consumer) {
		try {
			while(res.next()) {
				String value = res.getString(1);
				consumer.accept(value);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static void consumeRow(ResultSet res, SampleRowConsumer consumer) {
		try {
			String id = res.getString(1);
			String project = res.getString(2);
			String meta_rel_path = res.getString(3);
			String sample_rel_path = res.getString(4);
			String location = res.getString(5);
			long rtimestamp = res.getLong(6);
			long lastModified = res.getLong(7);
			boolean locked = res.getBoolean(8);
			String device = res.getString(9);
			consumer.accept(id, project, meta_rel_path, sample_rel_path, location, rtimestamp, lastModified, locked, device);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEach(SampleRowConsumer consumer) {		
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL);
			forEachRow(stmt.executeQuery(), consumer);			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachDevice(Consumer<String> consumer) {		
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_DEVICES_ALL);
			forEachString(stmt.executeQuery(), consumer);			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachAtTimerange(long start, long end, SampleRowConsumer consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_AT_TIMERANGE);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			forEachRow(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachDeviceAtTimerange(long start, long end, Consumer<String> consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_DEVICES_AT_TIMERANGE);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			forEachString(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachLocation(Consumer<String> consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_LOCATIONS_ALL);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String location = res.getString(1);
				consumer.accept(location);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachTimestamp(LongConsumer consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_TIMESTAMPS_ALL);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				long timestamp = res.getLong(1);
				consumer.accept(timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachDate(LongConsumer consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_DATES_ALL);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				long timestamp = res.getLong(1);
				consumer.accept(timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachTimestamp(String location, LongConsumer consumer) {
		PreparedStatement stmt;
		try {
			if(location == null) {
				stmt = getStatement(SQL.QUERY_TIMESTAMPS_AT_LOCATION_NULL);
			} else {
				stmt = getStatement(SQL.QUERY_TIMESTAMPS_AT_LOCATION);
				stmt.setString(1, location);
			}				
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				long timestamp = res.getLong(1);
				consumer.accept(timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachDate(String location, LongConsumer consumer) {
		PreparedStatement stmt;
		try {
			if(location == null) {
				stmt = getStatement(SQL.QUERY_DATES_AT_LOCATION_NULL);
			} else {
				stmt = getStatement(SQL.QUERY_DATES_AT_LOCATION);
				stmt.setString(1, location);
			}				
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				long timestamp = res.getLong(1);
				consumer.accept(timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int count() {
		try {
			PreparedStatement stmt = getStatement(SQL.COUNT_ALL);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int count = res.getInt(1);
				return count;
			} else {
				throw new RuntimeException("internal error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int countAtTimerange(long start, long end) {
		try {
			PreparedStatement stmt = getStatement(SQL.COUNT_AT_TIMERANGE);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int count = res.getInt(1);
				return count;
			} else {
				throw new RuntimeException("internal error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachPaged(SampleRowConsumer consumer, int limit, int offset) {		
		try {
			//explain(SQL.QUERY_ALL_PAGED);
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL_PAGED);
			stmt.setInt(1, limit);
			stmt.setInt(2, offset);
			forEachRow(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachAtTimerangePaged(long start, long end, SampleRowConsumer consumer, int limit, int offset) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_AT_TIMERANGE_PAGED);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			stmt.setInt(3, limit);
			stmt.setInt(4, offset);
			forEachRow(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}	

	/**
	 * 
	 * @param location    null == unknown location
	 * @return
	 */
	public int countAtLocation(String location) {		
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.COUNT_AT_LOCATION_NULL);
			} else {
				stmt = getStatement(SQL.COUNT_AT_LOCATION);
				stmt.setString(1, location);
			}
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int count = res.getInt(1);
				return count;
			} else {
				throw new RuntimeException("internal error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int countAtLocationAtTimerange(String location, long start, long end) {		
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.COUNT_AT_LOCATION_NULL_AT_TIMERANGE);
				stmt.setLong(1, start);
				stmt.setLong(2, end);
			} else {
				stmt = getStatement(SQL.COUNT_AT_LOCATION_AT_TIMERANGE);
				stmt.setString(1, location);
				stmt.setLong(2, start);
				stmt.setLong(3, end);
			}			
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int count = res.getInt(1);
				return count;
			} else {
				throw new RuntimeException("internal error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachAtLocation(String location, SampleRowConsumer consumer) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_NULL);	
			} else {
				stmt = getStatement(SQL.QUERY_AT_LOCATION);
				stmt.setString(1, location);			
			}
			forEachRow(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachDeviceAtLocation(String location, Consumer<String> consumer) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_DEVICES_AT_LOCATION_NULL);	
			} else {
				stmt = getStatement(SQL.QUERY_DEVICES_AT_LOCATION);
				stmt.setString(1, location);			
			}
			forEachString(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachAtLocationAtTimerange(String location, long start, long end, SampleRowConsumer consumer) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_NULL_AT_TIMERANGE);
				stmt.setLong(1, start);
				stmt.setLong(2, end);
			} else {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_AT_TIMERANGE);
				stmt.setString(1, location);
				stmt.setLong(2, start);
				stmt.setLong(3, end);
			}
			forEachRow(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachDeviceAtLocationAtTimerange(String location, long start, long end, Consumer<String> consumer) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_DEVICES_AT_LOCATION_NULL_AT_TIMERANGE);
				stmt.setLong(1, start);
				stmt.setLong(2, end);
			} else {
				stmt = getStatement(SQL.QUERY_DEVICES_AT_LOCATION_AT_TIMERANGE);
				stmt.setString(1, location);
				stmt.setLong(2, start);
				stmt.setLong(3, end);
			}
			forEachString(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}	

	public void forEachPagedAtLocation(String location, SampleRowConsumer consumer, int limit, int offset) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_NULL_PAGED);
				stmt.setInt(1, limit);
				stmt.setInt(2, offset);
			} else {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_PAGED);
				stmt.setString(1, location);
				stmt.setInt(2, limit);
				stmt.setInt(3, offset);
			}
			forEachRow(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEachPagedAtLocationAtTimerange(String location, long start, long end, SampleRowConsumer consumer, int limit, int offset) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_NULL_AT_TIMERANGE_PAGED);
				stmt.setLong(1, start);
				stmt.setLong(2, end);
				stmt.setInt(3, limit);
				stmt.setInt(4, offset);
			} else {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_AT_TIMERANGE_PAGED);
				stmt.setString(1, location);
				stmt.setLong(2, start);
				stmt.setLong(3, end);
				stmt.setInt(4, limit);
				stmt.setInt(5, offset);
			}
			forEachRow(stmt.executeQuery(), consumer);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean exist(String id) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_EXIST);
			stmt.setString(1, id);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int count = res.getInt(1);
				if(count == 0) {
					return false;
				} else if(count == 1) {
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

	public void initClearTraverseTable() {
		try {
			getStatement(SQL.DROP_TRAVERSE_TABLE).executeUpdate();
			getStatement(SQL.CREATE_TRAVERSE_TABLE).executeUpdate();			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}	
	}

	public void insertTraverse(String id) {
		//Logger.info("insertTraverse " + id);
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT_TRAVERSE);		
			stmt.setString(1, id);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

	public void deleteTraverseMissing() {
		try {
			getStatement(SQL.DELETE_TRAVERSE_MISSING).executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public int getTableSize() {
		try {
			PreparedStatement stmt = getStatement(SQL.COUNT_TABLE);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int count = res.getInt(1);
				return count;				
			}
			throw new RuntimeException("error in count table");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
}
