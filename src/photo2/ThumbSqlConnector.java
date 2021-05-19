package photo2;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ThumbSqlConnector {

	public final Connection conn;

	public static final String SQL_INSERT_FILE = "INSERT INTO THUMB " +
			"(ID, FILE) " +
			"VALUES " +
			"(?, ?)";
	public final PreparedStatement stmt_insert_file ;

	public static final String SQL_QUERY_FILE = "SELECT FILE FROM THUMB WHERE ID = ?";
	public final PreparedStatement stmt_query_file;
	
	public static final String SQL_EXIST_ID = "SELECT ID FROM THUMB WHERE ID = ?";
	public final PreparedStatement stmt_exist_id;

	public ThumbSqlConnector(Connection conn) {
		this.conn = conn;
		try {
			stmt_insert_file = conn.prepareStatement(SQL_INSERT_FILE);
			stmt_query_file = conn.prepareStatement(SQL_QUERY_FILE);	
			stmt_exist_id = conn.prepareStatement(SQL_EXIST_ID);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
