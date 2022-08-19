package photo2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;

import org.tinylog.Logger;

public class SqlConnector {	

	public static enum SQL {
		CREATE_TABLE(
				"CREATE TABLE PHOTO " +
						"(" +
						"ID VARCHAR(255) PRIMARY KEY, " +
						"PROJECT VARCHAR(255), " +
						"META_PATH VARCHAR(255), " +
						"IMAGE_PATH VARCHAR(255), " +
						"LOCATION VARCHAR(255), " +
						"DATE SMALLDATETIME, " +
						"LAST_MODIFIED INT8, " +
						"LOCKED BOOLEAN " +
						")"
				),
		
		CREATE_IDX_LOCATION("CREATE INDEX IF NOT EXISTS IDX_LOCATION ON PHOTO (LOCATION)"),
		
		CREATE_IDX_PROJECT("CREATE INDEX IF NOT EXISTS IDX_PROJECT ON PHOTO (PROJECT)"),

		INSERT_FILE(
				"INSERT INTO PHOTO " +
						"(ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED, LOCKED) " +
						"VALUES " +
						"(?, ?, ?, ?, ?, ?, ?, ?)"
				),
		
		QUERY_ID_EXIST("SELECT count(ID) FROM PHOTO WHERE ID = ?"),
		
		QUERY_PHOTO("SELECT ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED, LOCKED FROM PHOTO WHERE ID = ?"),
		
		QUERY_PHOTOS_BY_PROJECT("SELECT ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED, LOCKED FROM PHOTO WHERE PROJECT = ? AND NOT LOCKED"),
		
		QUERY_LOCATIONS("SELECT DISTINCT LOCATION FROM PHOTO WHERE PROJECT = ?"),
		
		QUERY_IDS_NOT_LOCKED("SELECT ID FROM PHOTO WHERE PROJECT = ? AND NOT LOCKED"),
		
		QUERY_IDS_NOT_LOCKED_SORT_DATE("SELECT ID FROM PHOTO WHERE PROJECT = ? AND NOT LOCKED ORDER BY DATE"),
		
		QUERY_ALL_IDS("SELECT ID FROM PHOTO"),
		
		QUERY_ALL_IDS_NOT_LOCKED("SELECT ID FROM PHOTO WHERE NOT LOCKED"),
		
		QUERY_IDS_NOT_LOCKED_WITH_LOCATION("SELECT ID FROM PHOTO WHERE PROJECT = ? AND LOCATION = ? AND NOT LOCKED"),
		
		QUERY_IDS_NOT_LOCKED_WITH_LOCATION_SORT_DATE("SELECT ID FROM PHOTO WHERE PROJECT = ? AND LOCATION = ? AND NOT LOCKED ORDER BY DATE"),
		
		QUERY_PHOTO_IS_UP_TO_DATE("SELECT EXISTS (SELECT * FROM PHOTO WHERE ID = ? AND LAST_MODIFIED = ?)"),
		
		QUERY_ALL_META_PATH("SELECT ID, PROJECT, META_PATH FROM PHOTO"),
		
		DELETE_PHOTO("DELETE FROM PHOTO WHERE ID = ?"),
		
		DELETE_PROJECT("DELETE FROM PHOTO WHERE PROJECT = ?"),
		
		UPDATE_PHOTO("UPDATE PHOTO SET PROJECT = ?, META_PATH = ?, IMAGE_PATH = ?, LOCATION = ?, DATE = ?, LAST_MODIFIED = ?, LOCKED = ? WHERE ID = ?"),

		
		CREATE_TABLE_REVIEW_LIST_COLLECTION(
				"CREATE TABLE REVIEW_LIST_COLLECTION " +
						"(" +
						"ID VARCHAR(255), " +			
						"PROJECT VARCHAR(255), " +
						"NAME VARCHAR(255), " +	
						"RECIPE VARCHAR, " +	
						"PRIMARY KEY (ID)" +
						")"
				),		

		CREATE_TABLE_REVIEW_LIST(
				"CREATE TABLE REVIEW_LIST " +
						"(" +
						"ID VARCHAR(255), " +			
						"PROJECT VARCHAR(255), " +
						"COLLECTION VARCHAR(255), " +	
						"NAME VARCHAR(255), " +						
						"LAST_MODIFIED INT8, " +
						"PRIMARY KEY (ID)" +
						")"
				),		

		CREATE_TABLE_REVIEW_LIST_ENTRY(
				"CREATE TABLE REVIEW_LIST_ENTRY " +
						"(" +
						"REVIEW_LIST VARCHAR(255), " +			
						"POS INT4, " +
						"PHOTO VARCHAR(255), " +
						"NAME VARCHAR(255), " +
						"RANKING FLOAT4, " +						
						"PRIMARY KEY (REVIEW_LIST, POS)" +
						")"
				),


		INSERT_REVIEW_LIST_COLLECTION(
				"INSERT INTO REVIEW_LIST_COLLECTION " +
						"(ID, PROJECT, NAME, RECIPE) " +
						"VALUES " +
						"(?, ?, ?, ?)"
				),

		INSERT_REVIEW_LIST(
				"INSERT INTO REVIEW_LIST " +
						"(ID, PROJECT, COLLECTION, NAME, LAST_MODIFIED) " +
						"VALUES " +
						"(?, ?, ?, ?, ?)"
				),

		INSERT_REVIEW_LIST_ENTRY(
				"INSERT INTO REVIEW_LIST_ENTRY " +
						"(REVIEW_LIST, POS, PHOTO, NAME, RANKING) " +
						"VALUES " +
						"(?, ?, ?, ?, ?)"
				),


		DELETE_REVIEW_LIST_COLLECTION("DELETE FROM REVIEW_LIST_COLLECTION"),

		DELETE_REVIEW_LIST("DELETE FROM REVIEW_LIST"),

		DELETE_REVIEW_LIST_ENTRY("DELETE FROM REVIEW_LIST_ENTRY"),


		DELETE_REVIEW_LIST_COLLECTION_BY_ID("DELETE FROM REVIEW_LIST_COLLECTION WHERE ID = ?"),

		DELETE_REVIEW_LIST_BY_ID("DELETE FROM REVIEW_LIST WHERE ID = ?"),

		DELETE_REVIEW_LIST_ENTRY_BY_REVIEW_LIST("DELETE FROM REVIEW_LIST_ENTRY WHERE REVIEW_LIST = ?"),


		DELETE_REVIEW_LIST_COLLECTION_BY_PROJECT("DELETE FROM REVIEW_LIST_COLLECTION WHERE PROJECT = ?"),

		DELETE_REVIEW_LIST_BY_PROJECT("DELETE FROM REVIEW_LIST WHERE PROJECT = ?"),

		DELETE_REVIEW_LIST_ENTRY_BY_PROJECT("DELETE FROM REVIEW_LIST_ENTRY AS E WHERE E.REVIEW_LIST IN (SELECT ID FROM REVIEW_LIST AS L WHERE L.PROJECT = ?)"),


		QUERY_REVIEW_LIST_COLLECTION_BY_PROJECT("SELECT ID, PROJECT, NAME, RECIPE FROM REVIEW_LIST_COLLECTION WHERE PROJECT = ?"),
		
		QUERY_REVIEW_LIST_COLLECTION_BY_ID("SELECT ID, PROJECT, NAME, RECIPE FROM REVIEW_LIST_COLLECTION WHERE ID = ?"),


		QUERY_REVIEW_LIST_BY_ID("SELECT PROJECT, COLLECTION, NAME FROM REVIEW_LIST WHERE ID = ?"),

		QUERY_REVIEW_LIST_BY_PROJECT("SELECT ID, COLLECTION, NAME FROM REVIEW_LIST WHERE PROJECT = ?"),

		QUERY_REVIEW_LIST_BY_COLLECTION("SELECT ID, COLLECTION, NAME FROM REVIEW_LIST WHERE COLLECTION = ?"),


		QUERY_REVIEW_LIST_ENTRY_BY_REVIEW_LIST("SELECT E.POS, E.PHOTO, E.NAME FROM REVIEW_LIST_ENTRY AS E INNER JOIN PHOTO AS P ON E.PHOTO = P.ID WHERE E.REVIEW_LIST = ? AND NOT P.LOCKED"),

		QUERY_REVIEW_LIST_ENTRY_BY_ID_ORDER_BY_RANKING("SELECT PHOTO, NAME, RANKING FROM REVIEW_LIST_ENTRY WHERE REVIEW_LIST = ? ORDER BY RANKING DESC"),
		
		COUNT_REVIEW_LIST_ENTRY_BY_REVIEW_LIST("SELECT COUNT(E.POS) FROM REVIEW_LIST_ENTRY AS E WHERE E.REVIEW_LIST = ?");
		

		public final String sql;

		SQL(String sql) {
			this.sql = sql;
		}
	}

	public final Connection conn;

	private final EnumMap<SQL, PreparedStatement> statementMap = new EnumMap<SQL, PreparedStatement>(SQL.class);

	public static class TlPhConnector extends ThreadLocal<SqlConnector> {

		private final Connection conn;

		public TlPhConnector(Connection conn) {
			this.conn = conn;
		}

		@Override
		public SqlConnector initialValue() {
			return new SqlConnector(conn);
		}
	}

	private SqlConnector(Connection conn) {
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
}
