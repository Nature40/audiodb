package audio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SampleManagerConnector {
	static final Logger log = LogManager.getLogger();

	public static enum SQL {
		DROP_TABLE("DROP TABLE IF EXISTS SAMPLE"),

		CREATE_TABLE("CREATE TABLE IF NOT EXISTS SAMPLE " +
				"(" +
				"ID VARCHAR(255) PRIMARY KEY, " +
				"PROJECT VARCHAR(255), " +
				"META_PATH VARCHAR(255), " +
				"SAMPLE_PATH VARCHAR(255), " +
				"LOCATION VARCHAR(255), " +
				"TIMESTAMP INT8, " +
				"LAST_MODIFIED INT8, " +
				"LOCKED BOOLEAN " +
				")"),

		INSERT("INSERT INTO SAMPLE " +
				"(ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED) " +
				"VALUES " +
				"(?, ?, ?, ?, ?, ?, ?, ?)"),

		UPDATE("UPDATE SAMPLE SET PROJECT = ?, META_PATH = ?, SAMPLE_PATH = ?, LOCATION = ?, TIMESTAMP = ?, LAST_MODIFIED = ?, LOCKED = ? WHERE ID = ?"),

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
		
		COUNT_ALL("SELECT COUNT(ID) FROM SAMPLE WHERE NOT LOCKED"),
		
		COUNT_AT_TIMESTAMP("SELECT COUNT(ID) FROM SAMPLE WHERE TIMESTAMP = ? AND NOT LOCKED"),
		
		QUERY_ALL("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE NOT LOCKED"),
		
		QUERY_AT_TIMESTAMP("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE TIMESTAMP = ? AND NOT LOCKED"),
		
		QUERY_ALL_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE NOT LOCKED LIMIT ? OFFSET ?"),
		
		QUERY_AT_TIMESTAMP_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE TIMESTAMP = ? AND NOT LOCKED LIMIT ? OFFSET ?"),
		
		COUNT_AT_LOCATION("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION = ? AND NOT LOCKED"),
		
		COUNT_AT_LOCATION_AT_TIMESTAMP("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION = ? AND TIMESTAMP = ? AND NOT LOCKED"),
		
		COUNT_AT_LOCATION_NULL("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION IS NULL AND NOT LOCKED"),
		
		COUNT_AT_LOCATION_NULL_AT_TIMESTAMP("SELECT COUNT(ID) FROM SAMPLE WHERE LOCATION IS NULL AND TIMESTAMP = ? AND NOT LOCKED"),
		
		QUERY_AT_LOCATION("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION = ? AND NOT LOCKED"),
		
		QUERY_AT_LOCATION_AT_TIMESTAMP("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION = ? AND TIMESTAMP = ? AND NOT LOCKED"),

		QUERY_AT_LOCATION_NULL("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION IS NULL AND NOT LOCKED"),
		
		QUERY_AT_LOCATION_NULL_AT_TIMESTAMP("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION IS NULL AND TIMESTAMP = ? AND NOT LOCKED"),

		QUERY_AT_LOCATION_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION = ? AND NOT LOCKED LIMIT ? OFFSET ?"),
		
		QUERY_AT_LOCATION_AT_TIMESTAMP_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION = ? AND TIMESTAMP = ? AND NOT LOCKED LIMIT ? OFFSET ?"),

		QUERY_AT_LOCATION_NULL_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION IS NULL AND NOT LOCKED LIMIT ? OFFSET ?"),
		
		QUERY_AT_LOCATION_NULL_AT_TIMESTAMP_PAGED("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE LOCATION IS NULL AND TIMESTAMP = ? AND NOT LOCKED LIMIT ? OFFSET ?"),

		QUERY_ID("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE ID = ? AND NOT LOCKED"),
		
		QUERY_ALL_LOCATION("SELECT DISTINCT LOCATION FROM SAMPLE ORDER BY LOCATION"),
		
		QUERY_ALL_TIMESTAMP("SELECT DISTINCT TIMESTAMP FROM SAMPLE ORDER BY TIMESTAMP");	

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

	public void initClear() {
		try {
			ResultSet res = conn.getMetaData().getTables(null, null, "SAMPLE", null);
			if(res.next()) {
				log.info("DROP TABLE");
				getStatement(SQL.DROP_TABLE).executeUpdate();
			}
			getStatement(SQL.CREATE_TABLE).executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void init() {
		try {
			ResultSet res = conn.getMetaData().getTables(null, null, "SAMPLE", null);
			if(!res.next()) {
				getStatement(SQL.CREATE_TABLE).executeUpdate();
			}			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void insert(String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp, long last_modified, boolean locked) {
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
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void update(String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp, long last_modified, boolean locked) {
		try {
			PreparedStatement stmt = getStatement(SQL.UPDATE);
			stmt.setString(1, project);		
			stmt.setString(2, meta_rel_path);
			stmt.setString(3, sample_rel_path);
			stmt.setString(4, location);
			stmt.setLong(5, timestamp);
			stmt.setLong(6, last_modified);
			stmt.setBoolean(7, locked);
			stmt.setString(8, id);			
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
		void accept(String id, String project, String meta_rel_path, String sample_rel_path, String location, long timestamp);
	}

	public void forEach(SampleRowConsumer consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String location = res.getString(5);
				long timestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, location, timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachAtTimestamp(long timestamp, SampleRowConsumer consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_AT_TIMESTAMP);
			stmt.setLong(1, timestamp);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String location = res.getString(5);
				long rtimestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, location, rtimestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachLocation(Consumer<String> consumer) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL_LOCATION);
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
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL_TIMESTAMP);
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
	
	public int countAtTimestamp(long timestamp) {
		try {
			PreparedStatement stmt = getStatement(SQL.COUNT_AT_TIMESTAMP);
			stmt.setLong(1, timestamp);
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
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL_PAGED);
			stmt.setInt(1, limit);
			stmt.setInt(2, offset);
			ResultSet res = stmt.executeQuery();			
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String location = res.getString(5);
				long timestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, location, timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachAtTimestampPaged(long timestamp, SampleRowConsumer consumer, int limit, int offset) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_AT_TIMESTAMP_PAGED);
			stmt.setLong(1, timestamp);
			stmt.setInt(2, limit);
			stmt.setInt(3, offset);
			ResultSet res = stmt.executeQuery();			
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String location = res.getString(5);
				long rtimestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, location, rtimestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
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
	
	public int countAtLocationAtTimestamp(String location, long timestamp) {		
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.COUNT_AT_LOCATION_NULL_AT_TIMESTAMP);
				stmt.setLong(1, timestamp);
			} else {
				stmt = getStatement(SQL.COUNT_AT_LOCATION_AT_TIMESTAMP);
				stmt.setString(1, location);
				stmt.setLong(2, timestamp);
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
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String locationR = res.getString(5);
				long timestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, locationR, timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachAtLocationAtTimestamp(String location, long timestamp, SampleRowConsumer consumer) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_NULL_AT_TIMESTAMP);
				stmt.setLong(1, timestamp);
			} else {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_AT_TIMESTAMP);
				stmt.setString(1, location);
				stmt.setLong(2, timestamp);
			}
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String locationR = res.getString(5);
				long rtimestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, locationR, rtimestamp);
			}
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
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String locationR = res.getString(5);
				long timestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, locationR, timestamp);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void forEachPagedAtLocationAtTimestamp(String location, long timestamp, SampleRowConsumer consumer, int limit, int offset) {
		try {
			PreparedStatement stmt;
			if(location == null) {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_NULL_AT_TIMESTAMP_PAGED);
				stmt.setLong(1, timestamp);
				stmt.setInt(2, limit);
				stmt.setInt(3, offset);
			} else {
				stmt = getStatement(SQL.QUERY_AT_LOCATION_AT_TIMESTAMP_PAGED);
				stmt.setString(1, location);
				stmt.setLong(2, timestamp);
				stmt.setInt(3, limit);
				stmt.setInt(4, offset);
			}
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);
				String sample_rel_path = res.getString(4);
				String locationR = res.getString(5);
				long rtimestamp = res.getLong(6);
				consumer.accept(id, project, meta_rel_path, sample_rel_path, locationR, rtimestamp);
			}
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
		//log.info("insertTraverse " + id);
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
}
