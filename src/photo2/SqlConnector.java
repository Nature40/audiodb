package photo2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumMap;

public class SqlConnector {

	public final Connection conn;

	public static final String SQL_CREATE_TABLE = "CREATE TABLE PHOTO " +
			"(" +
			"ID VARCHAR(255) PRIMARY KEY, " +
			"PROJECT VARCHAR(255), " +
			"META_PATH VARCHAR(255), " +
			"IMAGE_PATH VARCHAR(255), " +
			"LOCATION VARCHAR(255), " +
			"DATE SMALLDATETIME, " +
			"LAST_MODIFIED INT8, " +
			"LOCKED BOOLEAN " +
			")";

	public static final String SQL_INSERT_FILE = "INSERT INTO PHOTO " +
			"(ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED, LOCKED) " +
			"VALUES " +
			"(?, ?, ?, ?, ?, ?, ?, ?)";
	public final PreparedStatement stmt_insert_file;

	public static final String SQL_QUERY_ID_EXIST = "SELECT count(ID) FROM PHOTO WHERE ID = ?";
	public final PreparedStatement stmt_query_id_exist;

	public static final String SQL_QUERY_PHOTO = "SELECT ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED, LOCKED FROM PHOTO WHERE ID = ?";
	public final PreparedStatement stmt_query_photo;
	
	public static final String SQL_QUERY_PHOTOS_BY_PROJECT = "SELECT ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED, LOCKED FROM PHOTO WHERE PROJECT = ? AND NOT LOCKED";
	public final PreparedStatement stmt_query_photos_by_project;

	public static final String SQL_QUERY_LOCATIONS = "SELECT DISTINCT LOCATION FROM PHOTO WHERE PROJECT = ?";
	public final PreparedStatement stmt_qery_locations;

	public static final String SQL_QUERY_IDS_NOT_LOCKED = "SELECT ID FROM PHOTO WHERE PROJECT = ? AND NOT LOCKED";
	public final PreparedStatement stmt_query_ids;

	public static final String SQL_QUERY_ALL_IDS = "SELECT ID FROM PHOTO";
	public final PreparedStatement stmt_query_all_ids;
	
	public static final String SQL_QUERY_ALL_IDS_NOT_LOCKED = "SELECT ID FROM PHOTO WHERE NOT LOCKED";
	public final PreparedStatement stmt_query_all_ids_not_locked;

	public static final String SQL_QUERY_IDS_NOT_LOCKED_WITH_LOCATION = "SELECT ID FROM PHOTO WHERE PROJECT = ? AND LOCATION = ? AND NOT LOCKED";
	public final PreparedStatement stmt_query_ids_with_location;	

	public static final String SQL_QUERY_PHOTO_IS_UP_TO_DATE = "SELECT EXISTS (SELECT * FROM PHOTO WHERE ID = ? AND LAST_MODIFIED = ?)";
	public final PreparedStatement stmt_query_photo_is_up_to_date;

	public static final String SQL_QUERY_ALL_META_PATH = "SELECT ID, PROJECT, META_PATH FROM PHOTO";
	public final PreparedStatement stmt_query_all_meta_path;

	public static final String SQL_DELETE_PHOTO = "DELETE FROM PHOTO WHERE ID = ?";
	public final PreparedStatement stmt_delete_photo;

	public static final String SQL_DELETE_PROJECT = "DELETE FROM PHOTO WHERE PROJECT = ?";
	public final PreparedStatement stmt_delete_project;

	private static final String SQL_UPDATE_PHOTO = "UPDATE PHOTO SET PROJECT = ?, META_PATH = ?, IMAGE_PATH = ?, LOCATION = ?, DATE = ?, LAST_MODIFIED = ?, LOCKED = ? WHERE ID = ?";
	public final PreparedStatement stmt_update_photo;

	public static enum SQL {

		CREATE_TABLE_REVIEW_LIST_COLLECTION(
				"CREATE TABLE REVIEW_LIST_COLLECTION " +
						"(" +
						"ID VARCHAR(255), " +			
						"PROJECT VARCHAR(255), " +
						"NAME VARCHAR(255), " +	
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
						"(ID, PROJECT, NAME) " +
						"VALUES " +
						"(?, ?, ?)"
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
		
		
		QUERY_REVIEW_LIST_COLLECTION_BY_PROJECT("SELECT ID, NAME FROM REVIEW_LIST_COLLECTION WHERE PROJECT = ?"),
		
		QUERY_REVIEW_LIST_BY_PROJECT("SELECT ID, COLLECTION, NAME FROM REVIEW_LIST WHERE PROJECT = ?"),
		
		QUERY_REVIEW_LIST_BY_COLLECTION("SELECT ID, COLLECTION, NAME FROM REVIEW_LIST WHERE COLLECTION = ?"),
		
		
		QUERY_REVIEW_LIST_ENTRY_BY_REVIEW_LIST("SELECT E.POS, E.PHOTO, E.NAME FROM REVIEW_LIST_ENTRY AS E INNER JOIN PHOTO AS P ON E.PHOTO = P.ID WHERE E.REVIEW_LIST = ? AND NOT P.LOCKED"),
		
		QUERY_REVIEW_LIST_ENTRY_BY_ID_ORDER_BY_RANKING("SELECT PHOTO, NAME, RANKING FROM REVIEW_LIST_ENTRY WHERE REVIEW_LIST = ? ORDER BY RANKING DESC");	
		
		
		public final String sql;

		SQL(String sql) {
			this.sql = sql;
		}
	}

	private final EnumMap<SQL, PreparedStatement> statementMap = new EnumMap<SQL, PreparedStatement>(SQL.class);

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

	public SqlConnector(Connection conn) {
		this.conn = conn;

		try {
			stmt_insert_file = conn.prepareStatement(SQL_INSERT_FILE);
			stmt_query_id_exist = conn.prepareStatement(SQL_QUERY_ID_EXIST);
			stmt_query_photo = conn.prepareStatement(SQL_QUERY_PHOTO);
			stmt_query_photos_by_project = conn.prepareStatement(SQL_QUERY_PHOTOS_BY_PROJECT);
			stmt_qery_locations = conn.prepareStatement(SQL_QUERY_LOCATIONS);
			stmt_query_ids = conn.prepareStatement(SQL_QUERY_IDS_NOT_LOCKED);
			stmt_query_all_ids = conn.prepareStatement(SQL_QUERY_ALL_IDS);
			stmt_query_all_ids_not_locked = conn.prepareStatement(SQL_QUERY_ALL_IDS_NOT_LOCKED);
			stmt_query_ids_with_location = conn.prepareStatement(SQL_QUERY_IDS_NOT_LOCKED_WITH_LOCATION);
			stmt_query_photo_is_up_to_date = conn.prepareStatement(SQL_QUERY_PHOTO_IS_UP_TO_DATE);
			stmt_query_all_meta_path = conn.prepareStatement(SQL_QUERY_ALL_META_PATH);
			stmt_delete_photo = conn.prepareStatement(SQL_DELETE_PHOTO);
			stmt_delete_project = conn.prepareStatement(SQL_DELETE_PROJECT);
			stmt_update_photo = conn.prepareStatement(SQL_UPDATE_PHOTO);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
