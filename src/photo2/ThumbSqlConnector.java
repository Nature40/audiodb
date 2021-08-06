package photo2;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ThumbSqlConnector {

	public final Connection conn;
	
	public static final String SQL_CREATE_THUMB_TABLE = "CREATE TABLE THUMB "
			+ "("
			+ "ID VARCHAR(255) PRIMARY KEY, "
			+ "PHOTO VARCHAR(255), "
			+ "FILE BLOB"
			+ ")";

	public static final String SQL_INSERT_FILE = "INSERT INTO THUMB " +
			"(ID, PHOTO, FILE) " +
			"VALUES " +
			"(?, ?, ?)";
	public final PreparedStatement stmt_insert_file ;

	public static final String SQL_QUERY_FILE = "SELECT FILE FROM THUMB WHERE ID = ?";
	public final PreparedStatement stmt_query_file;
	
	public static final String SQL_EXIST_ID = "SELECT ID FROM THUMB WHERE ID = ?";
	public final PreparedStatement stmt_exist_id;
	
	//public static final String SQL_DELETE_MISSING = "DELETE FROM THUMB T WHERE NOT EXISTS(SELECT P.ID FROM PHOTO P WHERE P.ID = T.ID)";
	//public final PreparedStatement stmt_delete_missing;
	
	public static final String SQL_QUERY_PHOTO_IDS = "SELECT PHOTO FROM THUMB";
	public final PreparedStatement stmt_query_photo_ids;
	
	public static final String SQL_DELETE_BY_PHOTO_ID = "DELETE FROM THUMB WHERE PHOTO = ?";
	public final PreparedStatement stmt_delete_thumb_by_photo_id;

	public ThumbSqlConnector(Connection conn) {
		this.conn = conn;
		try {
			stmt_insert_file = conn.prepareStatement(SQL_INSERT_FILE);
			stmt_query_file = conn.prepareStatement(SQL_QUERY_FILE);	
			stmt_exist_id = conn.prepareStatement(SQL_EXIST_ID);
			//stmt_delete_missing = conn.prepareStatement(SQL_DELETE_MISSING);
			stmt_query_photo_ids = conn.prepareStatement(SQL_QUERY_PHOTO_IDS);
			stmt_delete_thumb_by_photo_id = conn.prepareStatement(SQL_DELETE_BY_PHOTO_ID);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
