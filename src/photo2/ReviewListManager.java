package photo2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import photo2.CsvTable.CsvCell;
import photo2.SqlConnector.SQL;

public class ReviewListManager {
	static final Logger log = LogManager.getLogger();

	private final PhotoDB2 photodb;

	public ReviewListManager(PhotoDB2 photodb) {
		this.photodb = photodb;
	}

	public void init() {
		try {
			SqlConnector sqlConnector = photodb.getSqlConnector();
			ResultSet res = sqlConnector.conn.getMetaData().getTables(null, null, "REVIEW_LIST_SET", null);
			if(!res.next()) {
				log.info("CREATE TABLE REVIEW_LIST_SET");
				sqlConnector.getStatement(SQL.CREATE_TABLE_REVIEW_LIST_SET).executeUpdate();
			}
			res = sqlConnector.conn.getMetaData().getTables(null, null, "REVIEW_LIST", null);
			if(!res.next()) {
				log.info("CREATE TABLE REVIEW_LIST");
				sqlConnector.getStatement(SQL.CREATE_TABLE_REVIEW_LIST).executeUpdate();
			}
			res = sqlConnector.conn.getMetaData().getTables(null, null, "REVIEW_LIST_ENTRY", null);
			if(!res.next()) {
				log.info("CREATE TABLE REVIEW_LIST_ENTRY");
				sqlConnector.getStatement(SQL.CREATE_TABLE_REVIEW_LIST_ENTRY).executeUpdate();
			}
			refresh();
		} catch(Exception e) {
			log.warn(e);
			throw new RuntimeException(e);
		}
	}	

	public void refresh() {
		try {
			SqlConnector sqlConnector = photodb.getSqlConnector();
			sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_ENTRY).executeUpdate();
			sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST).executeUpdate();
			sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_SET).executeUpdate();			
			photodb.foreachProject(projectConfig -> {
				refreshProject(projectConfig);
			});
		} catch(Exception e) {
			log.warn(e);
		}
	}

	public void refreshProject(PhotoProjectConfig projectConfig) {
		try {
			SqlConnector sqlConnector = photodb.getSqlConnector();
			try {
				PreparedStatement stmt = sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_ENTRY_BY_PROJECT);
				stmt.setString(1, projectConfig.project);
				stmt.executeUpdate();
				stmt = sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_BY_PROJECT);
				stmt.setString(1, projectConfig.project);
				stmt.executeUpdate();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			if(projectConfig.review_list_path != null) {
				try {
					PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST_SET);
					stmt.setString(1, projectConfig.project + "__" + "file");
					stmt.setString(2, projectConfig.project);
					stmt.setString(3, "file");
					stmt.executeUpdate();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
				traverse(projectConfig, projectConfig.review_list_path);
			}
		} catch (Exception e) {
			log.warn(e);
		}
	}

	private void traverse(PhotoProjectConfig projectConfig, Path root) throws IOException {
		log.info("traverse " + root);
		for(Path path:Files.newDirectoryStream(root)) {
			if(path.toFile().isDirectory()) {
				traverse(projectConfig, path);
			} else if(path.toFile().isFile()) {
				try {
					if(path.getFileName().toString().endsWith(".csv")) {
						refreshReviewList(projectConfig, path);
					}
				} catch(Exception e) {
					log.warn(e);
				}
			} else {
				log.warn("unknown entity: " + path);
			}
		}
	}

	private static String createId(String project, String filename) {
		String id = project + "__" + filename;
		id = id.replaceAll("/", "__");
		id = id.replaceAll("\\\\", "__");
		id = id.replaceAll(".csv", "");
		return id;
	}

	private void refreshReviewList(PhotoProjectConfig projectConfig, Path path) throws Exception {
		String filename = path.getFileName().toString();
		String name = filename.replaceAll(".csv", "");
		String reviewListId = createId(projectConfig.project, filename);
		long last_modified = path.toFile().lastModified();
		SqlConnector sqlConnector = photodb.getSqlConnector();

		photodb.deleteReviewList(reviewListId);

		PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST);
		stmt.setString(1, reviewListId);
		stmt.setString(2, projectConfig.project);
		stmt.setString(3, projectConfig.project + "__" + "file");
		stmt.setString(4, name);
		stmt.setLong(5, last_modified);
		stmt.executeUpdate();

		PreparedStatement insStmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST_ENTRY);
		try(CsvTable csvTable = new CsvTable(path)) {
			CsvCell cellPath = csvTable.getCell("path");			
			csvTable.forEachThrowable((csvRow, pos) -> {
				String photoPath = cellPath.get(csvRow);
				String photoId = PhotoDB2.metaRelPathToID(projectConfig.project, photoPath);
				insStmt.setString(1, reviewListId);
				insStmt.setInt(2, (pos + 1)); // 1 based index
				insStmt.setString(3, photoId);
				insStmt.setString(4, "name");
				insStmt.setFloat(5, 0f);
				insStmt.executeUpdate();
			});
		}
	}
}
