package audio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.EnumMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SampleManagerConnector {
	static final Logger log = LogManager.getLogger();

	public static enum SQL {
		DROP_TABLE("DROP TABLE SAMPLES"),

		CREATE_TABLE("CREATE TABLE SAMPLES " +
				"(" +
				"ID VARCHAR(255) PRIMARY KEY, " +
				"PROJECT VARCHAR(255), " +
				"META_PATH VARCHAR(255), " +
				"SAMPLE_PATH VARCHAR(255), " +
				"LOCATION VARCHAR(255), " +
				"DATE SMALLDATETIME" +
				")"),
		
		INSERT("INSERT INTO SAMPLES " +
				"(ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, DATE) " +
				"VALUES " +
				"(?, ?, ?, ?, ?, ?)"),
		
		QUERY_ALL("SELECT ID, PROJECT, META_PATH, SAMPLE_PATH, LOCATION, DATE FROM SAMPLES");

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
			ResultSet res = conn.getMetaData().getTables(null, null, "SAMPLES", null);
			if(res.next()) {
				log.info("DROP TABLE");
				getStatement(SQL.DROP_TABLE).executeUpdate();
			}
			getStatement(SQL.CREATE_TABLE).executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void insert(String id, String project, String meta_rel_path, String sample_rel_path, String location, LocalDateTime date) {
		try {
			PreparedStatement stmt = getStatement(SQL.INSERT);		
			stmt.setString(1, id);		
			stmt.setString(2, project);		
			stmt.setString(3, meta_rel_path);
			stmt.setString(4, sample_rel_path);
			stmt.setString(5, location);
			stmt.setObject(6, date);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@FunctionalInterface
	public interface SampleRowConsumer {
		 void accept(String id, String project, String meta_rel_path, String sample_rel_path, String location, LocalDateTime date);
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
				LocalDateTime date = res.getTimestamp(6).toLocalDateTime();
				consumer.accept(id, project, meta_rel_path, sample_rel_path, location, date);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
