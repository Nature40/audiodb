package photo2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.Broker;
import util.Timer;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class PhotoDB2 {
	static final Logger log = LogManager.getLogger();

	private final Broker broker;
	public final PhotoConfig config;
	public final ThumbManager thumbManager;

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
		this.thumbManager = new ThumbManager();
		try {
			this.conn = DriverManager.getConnection("jdbc:h2:./photo_cache");
			Statement stmt = conn.createStatement();

			/*ResultSet res = conn.getMetaData().getTables(null, null, "PHOTO", null);
			if(res.next()) {
				log.info("DROP TABLE");
				stmt.executeUpdate("DROP TABLE PHOTO");
			}*/		

			ResultSet res1 = conn.getMetaData().getTables(null, null, "PHOTO", null);
			if(!res1.next()) {
				log.info("CREATE TABLE");
				stmt.executeUpdate(SqlConnector.SQL_CREATE_TABLE);
				stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IDX_LOCATION ON PHOTO (LOCATION)");
				stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IDX_PROJECT ON PHOTO (PROJECT)");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		Timer.start("scanRemoved");
		try {
			scanRemoved();
		} catch (IOException e1) {
			log.warn(e1);
		}
		log.info(Timer.stop("scanRemoved"));

		for(PhotoProjectConfig projectConfig : broker.config().photoConfig.projectMap.values()) {
			Timer.start("traverse");
			try {
				int[] stats = new int[] {0, 0};
				traverse(projectConfig, projectConfig.root_path, stats);
				if(stats[0] > 0 || stats[1] > 0) {
					log.info(stats[0] + " rows inserted, " + stats[1] + " rows updated");
				}
			} catch (IOException e) {
				log.error(e);
			}
			log.info(Timer.stop("traverse"));
		}

		updateThumbs();
	}

	private void scanRemoved() throws IOException {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			PreparedStatement stmt = sqlconnector.stmt_query_all_meta_path;
			ResultSet res = stmt.executeQuery();
			int removeCount = 0;
			while(res.next()) {
				String id = res.getString(1);
				String project = res.getString(2);
				String meta_rel_path = res.getString(3);	

				PhotoProjectConfig projectConfig = config.projectMap.get(project);
				if(projectConfig == null) {
					throw new RuntimeException("no config for project");
				}

				Path meta_path = projectConfig.root_path.resolve(meta_rel_path);
				//log.info("check " + meta_path);
				if(!meta_path.toFile().exists()) {
					log.info("remove from DB " + meta_path);
					sqlconnector.stmt_delete_photo.setString(1, id);
					sqlconnector.stmt_delete_photo.executeUpdate();
					removeCount++;
				}
			}
			if(removeCount > 0) {
				log.info("Removed from DB " + removeCount + " rows.");	
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String metaRelPathToID(String meta_rel_path) {
		String id = meta_rel_path.replaceAll("/", "__");
		id = id.replaceAll("\\\\", "__");
		id = id.replaceAll(".yaml", "");
		return id;
	}

	private void traverse(PhotoProjectConfig projectConfig, Path root, int[] stats) throws IOException {
		log.info("traverse " + root);
		SqlConnector sqlconnector = tlsqlconnector.get();
		for(Path path:Files.newDirectoryStream(root)) {
			if(path.toFile().isDirectory()) {
				traverse(projectConfig, path, stats);
			} else if(path.toFile().isFile()) {
				if(path.getFileName().toString().endsWith(".yaml")) {
					String meta_rel_path = projectConfig.root_path.relativize(path).toString();
					String id = metaRelPathToID(meta_rel_path);
					long last_modified = path.toFile().lastModified();
					if(!this.isUpToDate(id, last_modified)) {


						YamlMap yamlMap = YamlUtil.readYamlMap(path);
						if(yamlMap.contains("PhotoSens") && yamlMap.getString("PhotoSens").equals("v1.0")) {
							String image_file = yamlMap.getString("file");
							String location = yamlMap.getString("location");
							LocalDateTime date = yamlMap.optLocalDateTime("date"); // nullable
							//log.info(path);
							try {
								String image_rel_path = projectConfig.root_path.relativize(root.resolve(image_file)).toString(); 
								//log.info("read " + meta_rel_path);
								//log.info("read+" + id);

								if(this.contains(id)) {
									sqlconnector.stmt_update_photo.setString(1, projectConfig.project);
									sqlconnector.stmt_update_photo.setString(2, meta_rel_path);
									sqlconnector.stmt_update_photo.setString(3, image_rel_path);
									sqlconnector.stmt_update_photo.setString(4, location);
									sqlconnector.stmt_update_photo.setObject(5, date);
									sqlconnector.stmt_update_photo.setLong(6, last_modified);
									sqlconnector.stmt_update_photo.setString(7, id);
									sqlconnector.stmt_update_photo.executeUpdate();
									stats[1]++;
								} else {
									sqlconnector.stmt_insert_file.setString(1, id);
									sqlconnector.stmt_insert_file.setString(2, projectConfig.project);
									sqlconnector.stmt_insert_file.setString(3, meta_rel_path);
									sqlconnector.stmt_insert_file.setString(4, image_rel_path);
									sqlconnector.stmt_insert_file.setString(5, location);
									sqlconnector.stmt_insert_file.setObject(6, date);
									sqlconnector.stmt_insert_file.setLong(7, last_modified);
									sqlconnector.stmt_insert_file.executeUpdate();
									stats[0]++;
								}
							} catch (SQLException e) {
								log.warn(e);
							}

						} else {
							log.warn("no valid PhotoSens yaml  " + path);
						}

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

	public void foreachId(Consumer<String> consumer) {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			PreparedStatement stmt = sqlconnector.stmt_query_all_ids;
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
			/*PreparedStatement stmt = conn.prepareStatement("EXPLAIN ANALYZE " + SqlConnector.SQL_QUERY_LOCATIONS);
			stmt.setString(1, project);
			ResultSet explRes = stmt.executeQuery();
			if(explRes.next()) {
				String expl = explRes.getString(1);
				log.info("\n\n" + expl + "\n\n");
			}*/
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

	public boolean isUpToDate(String id, long last_modified) {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			sqlconnector.stmt_query_photo_is_up_to_date.setString(1, id);
			sqlconnector.stmt_query_photo_is_up_to_date.setLong(2, last_modified);
			ResultSet res = sqlconnector.stmt_query_photo_is_up_to_date.executeQuery();
			if(res.next()) {
				return res.getBoolean(1);
			} else {
				throw new RuntimeException("sql error");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Photo2 getPhoto2(String id) {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			sqlconnector.stmt_query_photo.setString(1, id);
			ResultSet res = sqlconnector.stmt_query_photo.executeQuery();
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
				LocalDateTime date = res.getTimestamp(6).toLocalDateTime();
				return new Photo2(id, projectConfig.root_path.resolve(meta_rel_path), projectConfig.root_path.resolve(image_rel_path), location, date);
			}
			return null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateThumbs() {
		new Thread(() -> {
			foreachId(id -> {
				try {
					final int parallelism = ForkJoinPool.commonPool().getParallelism();
					int maxQueue = parallelism * 2;
					Photo2 photo = getPhoto2(id);
					long reqWidth = 320;
					long reqHeight = 320;
					String cacheFilename = thumbManager.getCacheFilename(photo, reqWidth, reqHeight);
					//log.info("id: " + id + " -->  " + cacheFilename);
					ThumbSqlConnector sqlConn = thumbManager.getSqlConnector();
					sqlConn.stmt_exist_id.setString(1, cacheFilename);
					ResultSet res = sqlConn.stmt_exist_id.executeQuery();
					if(res.next()) {
						//log.info("true");
					} else {
						log.info("insert " + cacheFilename + "  " + photo.imagePath + "    " + photo.id);
						while(ForkJoinPool.commonPool().getQueuedSubmissionCount() > maxQueue) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								log.warn(e);
							}
						}
						//log.info(ForkJoinPool.commonPool().getQueuedSubmissionCount());
						//log.info("start " + parallelism + "  " + ForkJoinPool.commonPool().getPoolSize() + "  " + ForkJoinPool.commonPool().getRunningThreadCount());
						thumbManager.submitScaled(cacheFilename, photo, reqWidth, reqHeight);					
						//log.info("done");
					}
				} catch (Exception e) {
					log.warn(e);
				}
			});			
		}).start();
	}
}
