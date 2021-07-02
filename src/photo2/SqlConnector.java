package photo2;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
			"LAST_MODIFIED INT8 " +
			")";

	public static final String SQL_INSERT_FILE = "INSERT INTO PHOTO " +
			"(ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED) " +
			"VALUES " +
			"(?, ?, ?, ?, ?, ?, ?)";
	public final PreparedStatement stmt_insert_file;

	public static final String SQL_QUERY_ID_EXIST = "SELECT count(ID) FROM PHOTO WHERE ID = ?";
	public final PreparedStatement stmt_query_id_exist;

	public static final String SQL_QUERY_PHOTO = "SELECT ID, PROJECT, META_PATH, IMAGE_PATH, LOCATION, DATE, LAST_MODIFIED FROM PHOTO WHERE ID = ?";
	public final PreparedStatement stmt_query_photo;

	public static final String SQL_QUERY_LOCATIONS = "SELECT DISTINCT LOCATION FROM PHOTO WHERE PROJECT = ?";
	public final PreparedStatement stmt_qery_locations;

	public static final String SQL_QUERY_IDS = "SELECT ID FROM PHOTO WHERE PROJECT = ?";
	public final PreparedStatement stmt_query_ids;
	
	public static final String SQL_QUERY_ALL_IDS = "SELECT ID FROM PHOTO";
	public final PreparedStatement stmt_query_all_ids;

	public static final String SQL_QUERY_IDS_WITH_LOCATION = "SELECT ID FROM PHOTO WHERE PROJECT = ? AND LOCATION = ?";
	public final PreparedStatement stmt_query_ids_with_location;	
	
	public static final String SQL_QUERY_PHOTO_IS_UP_TO_DATE = "SELECT EXISTS (SELECT * FROM PHOTO WHERE ID = ? AND LAST_MODIFIED = ?)";
	public final PreparedStatement stmt_query_photo_is_up_to_date;
	
	public static final String SQL_QUERY_ALL_META_PATH = "SELECT ID, PROJECT, META_PATH FROM PHOTO";
	public final PreparedStatement stmt_query_all_meta_path;
	
	public static final String SQL_DELETE_PHOTO = "DELETE FROM PHOTO WHERE ID = ?";
	public final PreparedStatement stmt_delete_photo;
	
	private static final String SQL_UPDATE_PHOTO = "UPDATE PHOTO SET PROJECT = ?, META_PATH = ?, IMAGE_PATH = ?, LOCATION = ?, DATE = ?, LAST_MODIFIED = ? WHERE ID = ?";
	public final PreparedStatement stmt_update_photo;

	public SqlConnector(Connection conn) {
		this.conn = conn;

		try {
			stmt_insert_file = conn.prepareStatement(SQL_INSERT_FILE);
			stmt_query_id_exist = conn.prepareStatement(SQL_QUERY_ID_EXIST);
			stmt_query_photo = conn.prepareStatement(SQL_QUERY_PHOTO);
			stmt_qery_locations = conn.prepareStatement(SQL_QUERY_LOCATIONS);
			stmt_query_ids = conn.prepareStatement(SQL_QUERY_IDS);
			stmt_query_all_ids = conn.prepareStatement(SQL_QUERY_ALL_IDS);
			stmt_query_ids_with_location = conn.prepareStatement(SQL_QUERY_IDS_WITH_LOCATION);
			stmt_query_photo_is_up_to_date = conn.prepareStatement(SQL_QUERY_PHOTO_IS_UP_TO_DATE);
			stmt_query_all_meta_path = conn.prepareStatement(SQL_QUERY_ALL_META_PATH);
			stmt_delete_photo = conn.prepareStatement(SQL_DELETE_PHOTO);
			stmt_update_photo = conn.prepareStatement(SQL_UPDATE_PHOTO);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
