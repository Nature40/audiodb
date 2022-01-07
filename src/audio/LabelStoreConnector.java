package audio;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.function.BiConsumer;

import org.tinylog.Logger;

import audio.SampleManagerConnector.SQL;
import audio.SampleManagerConnector.SampleRowConsumer;

public class LabelStoreConnector {

	public static enum SQL {
		DROP_TABLE_LABEL_STORE("DROP TABLE IF EXISTS LABEL_STORE"),

		CREATE_TABLE_LABEL_STORE("CREATE TABLE IF NOT EXISTS LABEL_STORE " +
				"(" +
				"ID VARCHAR(255), " +
				"LABEL VARCHAR(255) " +
				")"),

		INSERT_LABEL_STORE("INSERT INTO LABEL_STORE " +
				"(ID, LABEL) " +
				"VALUES " +
				"(?, ?)"),

		QUERY_ALL("SELECT ID, LABEL FROM LABEL_STORE");

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
				ResultSet res = conn.getMetaData().getTables(null, null, "LABEL_STORE", null);
				if(res.next()) {
					Logger.info("DROP TABLE");
					getStatement(SQL.DROP_TABLE_LABEL_STORE).executeUpdate();
				}
			}
			ResultSet res = conn.getMetaData().getTables(null, null, "LABEL_STORE", null);
			if(!res.next()) {
				getStatement(SQL.CREATE_TABLE_LABEL_STORE).executeUpdate();
			}		
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

	public void insert(String id, String label) {
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT_LABEL_STORE);		
			stmt.setString(1, id);		
			stmt.setString(2, label);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void forEach(BiConsumer<String, String> consumer) {		
		try {
			PreparedStatement stmt = getStatement(SQL.QUERY_ALL);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				String label = res.getString(2);
				consumer.accept(id, label);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
