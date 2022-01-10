package audio;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.tinylog.Logger;

public class LabelStoreConnector {

	public static enum SQL {
		DROP_TABLE_LABEL_STORE("DROP TABLE IF EXISTS LABEL_STORE"),

		CREATE_TABLE_LABEL_STORE("CREATE TABLE IF NOT EXISTS LABEL_STORE " +
				"(" +
				"ID INTEGER, " +
				"LABEL INTEGER, " +
				"RELIABILITY TINYINT " +
				")"),

		INSERT_LABEL_STORE("INSERT INTO LABEL_STORE " +
				"(ID, LABEL, RELIABILITY) " +
				"VALUES " +
				"(?, ?, ?)"),

		QUERY_ALL("SELECT ID, LABEL, RELIABILITY FROM LABEL_STORE"),
		

		DROP_TABLE_ID_SAMPLE_MAP("DROP TABLE IF EXISTS ID_SAMPLE_MAP"),

		CREATE_TABLE_ID_SAMPLE_MAP("CREATE TABLE IF NOT EXISTS ID_SAMPLE_MAP " +
				"(" +
				"ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
				"SAMPLE VARCHAR(255) NOT NULL UNIQUE" +
				")"),

		CREATE_IDX_ID_SAMPLE_MAP_SAMPLE("CREATE INDEX IF NOT EXISTS IDX_ID_SAMPLE_MAP_SAMPLE ON ID_SAMPLE_MAP (SAMPLE)"),

		INSERT_ID_SAMPLE("INSERT INTO ID_SAMPLE_MAP " +
				"(SAMPLE) " +
				"VALUES " +
				"(?)"),		

		QUERY_ID_BY_SAMPLE("SELECT ID FROM ID_SAMPLE_MAP WHERE SAMPLE = ?"),

		QUERY_SAMPLE_BY_ID("SELECT SAMPLE FROM ID_SAMPLE_MAP WHERE ID = ?"),
		
		
		DROP_TABLE_ID_LABEL_MAP("DROP TABLE IF EXISTS ID_LABEL_MAP"),

		CREATE_TABLE_ID_LABEL_MAP("CREATE TABLE IF NOT EXISTS ID_LABEL_MAP " +
				"(" +
				"ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
				"LABEL VARCHAR(255) NOT NULL UNIQUE" +
				")"),

		CREATE_IDX_ID_LABEL_MAP_LABEL("CREATE INDEX IF NOT EXISTS IDX_ID_LABEL_MAP_LABEL ON ID_LABEL_MAP (LABEL)"),

		INSERT_ID_LABEL("INSERT INTO ID_LABEL_MAP " +
				"(LABEL) " +
				"VALUES " +
				"(?)"),		

		QUERY_ID_BY_LABEL("SELECT ID FROM ID_LABEL_MAP WHERE LABEL = ?"),

		QUERY_LABEL_BY_ID("SELECT LABEL FROM ID_LABEL_MAP WHERE ID = ?");


		public final String sql;

		SQL(String sql) {
			this.sql = sql;
		}
	}

	public static class TlLabelStoreConnector extends ThreadLocal<LabelStoreConnector> {

		private final Connection conn;

		public TlLabelStoreConnector(String url) {
			try {
				conn = DriverManager.getConnection(url);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public LabelStoreConnector initialValue() {
			return new LabelStoreConnector(conn);
		}
	}

	public final Connection conn;

	private final EnumMap<SQL, PreparedStatement> statementMap = new EnumMap<SQL, PreparedStatement>(SQL.class);

	private LabelStoreConnector(Connection conn) {
		this.conn = conn;
	}

	public void init(boolean clear) {
		try {
			if(clear) {
				{
					ResultSet res = conn.getMetaData().getTables(null, null, "LABEL_STORE", null);
					if(res.next()) {
						Logger.info("DROP TABLE LABEL_STORE");
						getStatement(SQL.DROP_TABLE_LABEL_STORE).executeUpdate();
					}
				}
				{
					ResultSet res = conn.getMetaData().getTables(null, null, "ID_SAMPLE_MAP", null);
					if(res.next()) {
						Logger.info("DROP TABLE ID_SAMPLE_MAP");
						getStatement(SQL.DROP_TABLE_ID_SAMPLE_MAP).executeUpdate();
					}
				}
				
				{
					ResultSet res = conn.getMetaData().getTables(null, null, "ID_LABEL_MAP", null);
					if(res.next()) {
						Logger.info("DROP TABLE ID_LABEL_MAP");
						getStatement(SQL.DROP_TABLE_ID_LABEL_MAP).executeUpdate();
					}
				}				
			}
			{
				ResultSet res = conn.getMetaData().getTables(null, null, "LABEL_STORE", null);
				if(!res.next()) {
					getStatement(SQL.CREATE_TABLE_LABEL_STORE).executeUpdate();
				}
			}
			{
				ResultSet res = conn.getMetaData().getTables(null, null, "ID_SAMPLE_MAP", null);
				if(!res.next()) {
					getStatement(SQL.CREATE_TABLE_ID_SAMPLE_MAP).executeUpdate();
				}
			}
			{
				ResultSet res = conn.getMetaData().getTables(null, null, "ID_LABEL_MAP", null);
				if(!res.next()) {
					getStatement(SQL.CREATE_TABLE_ID_LABEL_MAP).executeUpdate();
				}
			}			
			getStatement(SQL.CREATE_IDX_ID_SAMPLE_MAP_SAMPLE).executeUpdate();
			getStatement(SQL.CREATE_IDX_ID_LABEL_MAP_LABEL).executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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

	public void insert(int id, int label, int reliability) {
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT_LABEL_STORE);		
			stmt.setInt(1, id);		
			stmt.setInt(2, label);
			stmt.setInt(3, reliability);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@FunctionalInterface
	public interface LabelRowConsumer {
	    void accept(int id, int label, byte reliability);
	}
		
	public void forEach(LabelRowConsumer consumer) {		
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int id = res.getInt(1);
				int label = res.getInt(2);
				byte reliability = res.getByte(3);
				consumer.accept(id, label, reliability);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private int getIdBySampleInternal(String sample) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ID_BY_SAMPLE);
			stmt.setString(1, sample);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int id = res.getInt(1);
				return id;
			} else {
				return -1;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public int getOrCreateIdBySample(String sample) {
		int id = getIdBySampleInternal(sample);
		if(id >= 0) {
			return id;
		}
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT_ID_SAMPLE);		
			stmt.setString(1, sample);
			stmt.executeUpdate();
		} catch (SQLException e) {
			Logger.warn(e);
		}
		id = getIdBySampleInternal(sample);
		if(id >= 0) {
			return id;
		}
		throw new RuntimeException("internal error");
	}
	
	public String getSampleById(int id) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_SAMPLE_BY_ID);
			stmt.setInt(1, id);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				String sample = res.getString(1);
				return sample;
			} else {
				throw new RuntimeException("sample by id not found");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	private int getIdByLabelInternal(String label) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ID_BY_LABEL);
			stmt.setString(1, label);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				int id = res.getInt(1);
				return id;
			} else {
				return -1;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public int getOrCreateIdByLabel(String label) {
		int id = getIdByLabelInternal(label);
		if(id >= 0) {
			return id;
		}
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT_ID_LABEL);		
			stmt.setString(1, label);
			stmt.executeUpdate();
		} catch (SQLException e) {
			Logger.warn(e);
		}
		id = getIdByLabelInternal(label);
		if(id >= 0) {
			return id;
		}
		throw new RuntimeException("internal error");
	}
	
	public String getLabelById(int id) {
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_LABEL_BY_ID);
			stmt.setInt(1, id);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				String label = res.getString(1);
				return label;
			} else {
				throw new RuntimeException("label by id not found");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
}