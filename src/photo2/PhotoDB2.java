package photo2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.Broker;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class PhotoDB2 {
	static final Logger log = LogManager.getLogger();

	private final Broker broker;
	public final PhotoConfig config;

	private Connection conn;

	ThreadLocal<SqlConnector> tlsqlconnector = new ThreadLocal<SqlConnector>() {
		@Override
		public SqlConnector initialValue() {
			return new SqlConnector(conn);
		}		
	};

	public PhotoDB2(Broker broker) {
		this.broker = broker;
		this.config = broker.config().photoConfig;
		try {
			this.conn = DriverManager.getConnection("jdbc:h2:./photo_cache");

			Statement stmt = conn.createStatement();
			ResultSet res = conn.getMetaData().getTables(null, null, "PHOTO", null);
			if(res.next()) {
				log.info("DROP TABLE");
				stmt.executeUpdate("DROP TABLE PHOTO");
			}

			stmt.executeUpdate(SqlConnector.SQL_CREATE_TABLE);
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IDX_LOCATION ON PHOTO (LOCATION)");
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IDX_PROJECT ON PHOTO (PROJECT)");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}			

		for(PhotoProjectConfig projectConfig : broker.config().photoConfig.projectMap.values()) {
			try {
				traverse(projectConfig, projectConfig.root_path);
			} catch (IOException e) {
				log.error(e);
			}
		}


	}

	private void traverse(PhotoProjectConfig projectConfig, Path root) throws IOException {
		for(Path path:Files.newDirectoryStream(root)) {
			if(path.toFile().isDirectory()) {
				traverse(projectConfig, path);
			} else if(path.toFile().isFile()) {
				if(path.getFileName().toString().endsWith(".yaml")) {
					YamlMap yamlMap = YamlUtil.readYamlMap(path);
					if(yamlMap.contains("PhotoSens") && yamlMap.getString("PhotoSens").equals("v1.0")) {
						String image_file = yamlMap.getString("file");
						String location = yamlMap.getString("location");
						LocalDateTime date = yamlMap.optLocalDateTime("date"); // nullable
						//log.info(path);
						try {
							String meta_rel_path = projectConfig.root_path.relativize(path).toString();
							String image_rel_path = projectConfig.root_path.relativize(root.resolve(image_file)).toString(); 
							log.info("read " + meta_rel_path);							
							String id = meta_rel_path.replaceAll("/", "__");
							id = id.replaceAll("\\\\", "__");
							id = id.replaceAll(".yaml", "");
							log.info("read+" + id);
							SqlConnector sqlconnector = tlsqlconnector.get();
							sqlconnector.stmt_insert_file.setString(1, id);
							sqlconnector.stmt_insert_file.setString(2, projectConfig.project);
							sqlconnector.stmt_insert_file.setString(3, meta_rel_path);
							sqlconnector.stmt_insert_file.setString(4, image_rel_path);
							sqlconnector.stmt_insert_file.setString(5, location);
							//sqlconnector.stmt_insert_file.setTimestamp(6, java.sql.Timestamp.valueOf(date));
							sqlconnector.stmt_insert_file.setObject(6, date);
							sqlconnector.stmt_insert_file.executeUpdate();
						} catch (SQLException e) {
							log.warn(e);
						}

					} else {
						log.warn("no valid PhotoSens yaml  " + path);
					}
				}
			} else {
				log.warn("unknown entity: " + path);
			}
		}
	}

	public synchronized void close() {
		if(conn != null) {
			try {
				Connection c = conn;
				conn = null;
				c.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void foreachId(String project, String location, Consumer<String> consumer) {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			PreparedStatement stmt = sqlconnector.stmt_query_ids;
			if(location != null) {
				stmt = sqlconnector.stmt_query_ids_with_location;
				stmt.setString(1, project);
				stmt.setString(2, location);
			}
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//log.info(id);
				consumer.accept(id);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void foreachLocation(String project, Consumer<String> consumer) {
		try {
			PreparedStatement stmt = conn.prepareStatement("EXPLAIN ANALYZE " + SqlConnector.SQL_QUERY_LOCATIONS);
			stmt.setString(1, project);
			ResultSet explRes = stmt.executeQuery();
			if(explRes.next()) {
				String expl = explRes.getString(1);
				log.info("\n\n" + expl + "\n\n");
			}
			SqlConnector sqlconnector = tlsqlconnector.get();
			sqlconnector.stmt_qery_locations.setString(1, project);
			ResultSet res = sqlconnector.stmt_qery_locations.executeQuery();
			while(res.next()) {
				String location = res.getString(1);
				consumer.accept(location);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void foreachProject(Consumer<PhotoProjectConfig> consumer) {
		config.projectMap.values().forEach(consumer);
	}

	public boolean contains(String id) {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			sqlconnector.stmt_query_id_exist.setString(1, id);
			ResultSet res = sqlconnector.stmt_query_id_exist.executeQuery();
			if(res.next()) {
				int count = res.getInt(1);
				if(count == 0) {
					return false;
				} else if(count == 1) {
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Photo2 getPhoto2(String id) {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			sqlconnector.stmt_qery_photo.setString(1, id);
			ResultSet res = sqlconnector.stmt_qery_photo.executeQuery();
			if(res.next()) {
				String id2 = res.getString(1);
				if(!id.equals(id2)) {
					throw new RuntimeException("internal error: " + id + "   " + id2);
				}
				String project = res.getString(2);
				PhotoProjectConfig projectConfig = config.projectMap.get(project);
				if(projectConfig == null) {
					throw new RuntimeException("no config for project");
				}
				String meta_rel_path = res.getString(3);
				String image_rel_path = res.getString(4);
				String location = res.getString(5);
				Object obj5 = res.getObject(6);
				log.info(obj5.getClass());
				LocalDateTime date = res.getTimestamp(6).toLocalDateTime();
				return new Photo2(id, projectConfig.root_path.resolve(meta_rel_path), projectConfig.root_path.resolve(image_rel_path), location, date);
			}
			return null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
