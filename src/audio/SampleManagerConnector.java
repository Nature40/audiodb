package audio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;

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

		QUERY_ALL("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, TIMESTAMP, LAST_MODIFIED, LOCKED FROM SAMPLE WHERE NOT LOCKED"),

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

		DELETE_TRAVERSE_MISSING("DELETE FROM SAMPLE WHERE NOT EXISTS ( SELECT 1 FROM TRAVERSE WHERE SAMPLE.ID = TRAVERSE.ID)");

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
