package audio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class SampleStorageConnector {
	private static LinkedHashMap<String, String[]> TABLE_MAP = new LinkedHashMap<String, String[]>();

	static {
		TABLE_MAP.put("SAMPLE", new String[] {
				"SAMPLE_ID INT PRIMARY KEY",
				"LOCATION_ID INT NOT NULL ",
				"TIMESTAMP BIGINT NOT NULL",	
		});

		TABLE_MAP.put("FOLDER", new String[] {
				"FOLDER_ID INT AUTO_INCREMENT PRIMARY KEY",
				"FOLDER_PATH VARCHAR(255) NOT NULL",
		});

		TABLE_MAP.put("PATH", new String[] {
				"SAMPLE_ID INT AUTO_INCREMENT PRIMARY KEY",
				"FOLDER_ID INT NOT NULL",
				"FILENAME VARCHAR(255) NOT NULL",
		});

		TABLE_MAP.put("LOCATION", new String[] {
				"LOCATION_ID INT AUTO_INCREMENT PRIMARY KEY",
				"LOCATION_NAME VARCHAR(255) NOT NULL",
		});

		TABLE_MAP.put("EXCLUDED_PATH", new String[] {
				"FOLDER_ID INT NOT NULL",
				"FILENAME VARCHAR(255) NOT NULL",
				"PRIMARY KEY (FOLDER_ID, FILENAME)",
		});
	}

	private static enum SQL {

		QUERY_FOLDER_ID_BY_FOLDER_PATH("SELECT FOLDER_ID FROM FOLDER WHERE FOLDER_PATH = ?"),		
		QUERY_SAMPLE_ID_BY_PATH("SELECT SAMPLE_ID FROM PATH WHERE FOLDER_ID = ? AND FILENAME = ?"),
		QUERY_LOCATION_ID_BY_LOCATION_NAME("SELECT LOCATION_ID FROM LOCATION WHERE LOCATION_NAME = ?"),
		
		EXIST_SAMPLE_ID("SELECT count(*) FROM SAMPLE WHERE SAMPLE_ID = ?"),
		
		INSERT_SAMPLE("INSERT INTO SAMPLE (SAMPLE_ID, LOCATION_ID, TIMESTAMP) VALUES (?, ?, ?)");

		public final String sql;

		SQL(String sql) {
			this.sql = sql;
		}
	}
	
	private static enum SQL_GK {

		INSERT_FOLDER_PATH("INSERT INTO FOLDER (FOLDER_PATH) VALUES (?)"),
		INSERT_PATH("INSERT INTO PATH (FOLDER_ID, FILENAME) VALUES (?, ?)"),
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
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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

	public int getFolderId(String folder_path) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_FOLDER_ID_BY_FOLDER_PATH);
			stmt.setString(1, folder_path);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return res.getInt(1);
			} else {				
				stmt = getStatement(SQL_GK.INSERT_FOLDER_PATH);
				stmt.setString(1, folder_path);
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

	public int getSampleIdByPath(int folderId, String data_filename) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_SAMPLE_ID_BY_PATH);
			stmt.setInt(1, folderId);
			stmt.setString(2, data_filename);
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				return res.getInt(1);
			} else {
				stmt = getStatement(SQL_GK.INSERT_PATH);
				stmt.setInt(1, folderId);
				stmt.setString(2, data_filename);
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
	
	public boolean existSampleId(int sampleId) {
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
	
	public void insertSample(int sampleId, int locationId, long timestamp) {
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT_SAMPLE);
			stmt.setInt(1, sampleId);
			stmt.setInt(2, locationId);
			stmt.setLong(3, timestamp);
			stmt.executeUpdate();
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
}
