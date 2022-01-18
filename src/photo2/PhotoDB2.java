package photo2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;


import org.tinylog.Logger;

import audio.Broker;
import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import photo2.SqlConnector.SQL;
import photo2.api.PhotoMeta;
import util.Timer;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class PhotoDB2 {
	

	private final Broker broker;
	public final PhotoConfig config;
	public final ThumbManager thumbManager;
	public final ReviewListManager reviewListManager;

	private Connection conn;

	private Map<String, Vec<ClassificationDefinition>> classificationDefinitionsMap;

	ThreadLocal<SqlConnector> tlsqlconnector = new ThreadLocal<SqlConnector>() {
		@Override
		public SqlConnector initialValue() {
			return new SqlConnector(conn);
		}		
	};	

	public PhotoDB2(Broker broker) {
		this.broker = broker;
		this.config = broker.config().photoConfig;
		this.thumbManager = new ThumbManager(this);
		try {
			this.conn = DriverManager.getConnection("jdbc:h2:./photo_cache");
			//Logger.info("transaction isolation level: " + this.conn.getTransactionIsolation());
			//Logger.info("auto-commit mode: " + this.conn.getAutoCommit());
			Statement stmt = conn.createStatement();

			/*ResultSet res = conn.getMetaData().getTables(null, null, "PHOTO", null);
			if(res.next()) {
				Logger.info("DROP TABLE");
				stmt.executeUpdate("DROP TABLE PHOTO");
			}*/		

			ResultSet res1 = conn.getMetaData().getTables(null, null, "PHOTO", null);
			if(!res1.next()) {
				Logger.info("CREATE TABLE");
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
			Logger.warn(e1);
		}
		Logger.info(Timer.stop("scanRemoved"));

		for(PhotoProjectConfig projectConfig : broker.config().photoConfig.projectMap.values()) {
			Timer.start("traverse");
			try {
				int[] stats = new int[] {0, 0};
				traverse(projectConfig, projectConfig.root_path, stats);
				if(stats[0] > 0 || stats[1] > 0) {
					Logger.info(stats[0] + " rows inserted, " + stats[1] + " rows updated");
				}
			} catch (IOException e) {
				Logger.error(e);
			}
			Logger.info(Timer.stop("traverse"));
		}

		readDefinitions();


		updateThumbs();

		this.reviewListManager = new ReviewListManager(this);
		reviewListManager.init();
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
					Logger.info("no config for project, remove meta data");					
					try {
						sqlconnector.stmt_delete_project.setString(1, project);
						sqlconnector.stmt_delete_project.executeUpdate();
					} catch (SQLException e) {
						Logger.warn(e);
					}
				} else {
					Path meta_path = projectConfig.root_path.resolve(meta_rel_path);
					//Logger.info("check " + meta_path);
					if(!meta_path.toFile().exists()) {
						Logger.info("remove from DB " + meta_path);
						sqlconnector.stmt_delete_photo.setString(1, id);
						sqlconnector.stmt_delete_photo.executeUpdate();
						removeCount++;
					}
				}
			}
			if(removeCount > 0) {
				Logger.info("Removed from DB " + removeCount + " rows.");	
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		thumbManager.clean();
	}



	public static String metaRelPathToID(String project, String meta_rel_path) {
		String id = meta_rel_path.replaceAll("/", "__");
		id = id.replaceAll("\\\\", "__");
		id = id.replaceAll(".yaml", "");
		id = project + "__" + id;
		return id;
	}

	private void traverse(PhotoProjectConfig projectConfig, Path root, int[] stats) throws IOException {
		Logger.info("traverse " + root);
		try {
			for(Path path:Files.newDirectoryStream(root)) {
				if(path.toFile().isDirectory()) {
					traverse(projectConfig, path, stats);
				} else if(path.toFile().isFile()) {
					try {
						if(path.getFileName().toString().endsWith(".yaml")) {
							refreshPhotoDBentry(projectConfig, root, path, stats);
						}
					} catch(Exception e) {
						Logger.warn(e);
						e.printStackTrace();
					}
				} else {
					Logger.warn("unknown entity: " + path);
				}
			}
		} catch(Exception e) {
			Logger.warn("error in " + root + "   " + e);
		}
	}

	public void refreshPhotoDBentry(Photo2 photo, int[] stats) {
		refreshPhotoDBentry(photo.projectConfig, photo.metaPath.getParent(), photo.metaPath, stats);
	}

	public void refreshPhotoDBentry(PhotoProjectConfig projectConfig, Path root, Path metaPath, int[] stats) {
		String meta_rel_path = projectConfig.root_path.relativize(metaPath).toString();
		String id = metaRelPathToID(projectConfig.project, meta_rel_path);
		if(metaPath.toFile().exists()) {			
			long last_modified = metaPath.toFile().lastModified();
			if(!this.isUpToDate(id, last_modified)) {
				//Logger.info("refresh " + metaPath);

				YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
				if(yamlMap.contains("PhotoSens") /*&& yamlMap.getString("PhotoSens").equals("v1.0")*/) {
					String image_file = yamlMap.getString("file");
					String location = yamlMap.getString("location");
					LocalDateTime date = yamlMap.optLocalDateTime("date"); // nullable

					PhotoMeta photoMeta = new PhotoMeta(yamlMap);
					boolean locked = photoMeta.isClassifiedAsPerson();

					//Logger.info("refresh " + metaPath + "  locked " + locked);

					//Logger.info(path);
					try {
						String image_rel_path = projectConfig.root_path.relativize(root.resolve(image_file)).toString(); 
						//Logger.info("read " + meta_rel_path);
						//Logger.info("read+" + id);	

						if(this.contains(id)) {
							SqlConnector sqlconnector = tlsqlconnector.get();
							sqlconnector.stmt_update_photo.setString(1, projectConfig.project);
							sqlconnector.stmt_update_photo.setString(2, meta_rel_path);
							sqlconnector.stmt_update_photo.setString(3, image_rel_path);
							sqlconnector.stmt_update_photo.setString(4, location);
							sqlconnector.stmt_update_photo.setObject(5, date);
							sqlconnector.stmt_update_photo.setLong(6, last_modified);
							sqlconnector.stmt_update_photo.setBoolean(7, locked);
							sqlconnector.stmt_update_photo.setString(8, id);
							sqlconnector.stmt_update_photo.executeUpdate();
							if(stats != null) {
								stats[1]++;
							}
						} else {
							SqlConnector sqlconnector = tlsqlconnector.get();
							sqlconnector.stmt_insert_file.setString(1, id);
							sqlconnector.stmt_insert_file.setString(2, projectConfig.project);
							sqlconnector.stmt_insert_file.setString(3, meta_rel_path);
							sqlconnector.stmt_insert_file.setString(4, image_rel_path);
							sqlconnector.stmt_insert_file.setString(5, location);
							sqlconnector.stmt_insert_file.setObject(6, date);
							sqlconnector.stmt_insert_file.setLong(7, last_modified);
							sqlconnector.stmt_insert_file.setBoolean(8, locked);
							sqlconnector.stmt_insert_file.executeUpdate();
							if(stats != null) {
								stats[0]++;
							}
						}
					} catch (SQLException e) {
						Logger.warn(e);
					}
				} else {
					Logger.warn("no valid PhotoSens yaml  " + metaPath);
				}
			}	
		} else {
			try {
				SqlConnector sqlconnector = tlsqlconnector.get();
				Logger.info("remove from DB " + metaPath);
				sqlconnector.stmt_delete_photo.setString(1, id);
				sqlconnector.stmt_delete_photo.executeUpdate();

			} catch (SQLException e) {
				Logger.warn(e);
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
			PreparedStatement stmt;
			if(location == null) {
				stmt = sqlconnector.stmt_query_ids;
				stmt.setString(1, project);
			} else {
				stmt = sqlconnector.stmt_query_ids_with_location;
				stmt.setString(1, project);
				stmt.setString(2, location);
			}
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//Logger.info(id);
				consumer.accept(id);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@FunctionalInterface
	public interface ReviewListSetConsumer {
		void accept(String id, String name);
	}

	@FunctionalInterface
	public interface ReviewListConsumer {
		void accept(String id, String set, String name);
	}
	
	public void foreachReviewListSetByProject(String project, ReviewListSetConsumer consumer) {
		try {
			SqlConnector sqlConnector = getSqlConnector();
			PreparedStatement stmt = sqlConnector.getStatement(SQL.QUERY_REVIEW_LIST_COLLECTION_BY_PROJECT);
			stmt.setString(1, project);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				String name = res.getString(2);
				consumer.accept(id, name);				
			}
		} catch (SQLException e) {
			Logger.warn(e);
			throw new RuntimeException(e);
		}
	}

	public void foreachReviewListByProject(String project, ReviewListConsumer consumer) {
		try {
			SqlConnector sqlConnector = getSqlConnector();
			PreparedStatement stmt = sqlConnector.getStatement(SQL.QUERY_REVIEW_LIST_BY_PROJECT);
			stmt.setString(1, project);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				String set = res.getString(2);
				String name = res.getString(3);
				consumer.accept(id, set, name);				
			}
		} catch (SQLException e) {
			Logger.warn(e);
			throw new RuntimeException(e);
		}
	}
	
	public void foreachReviewListBySet(String set, ReviewListConsumer consumer) {
		try {
			SqlConnector sqlConnector = getSqlConnector();
			PreparedStatement stmt = sqlConnector.getStatement(SQL.QUERY_REVIEW_LIST_BY_COLLECTION);
			stmt.setString(1, set);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				String setName = res.getString(2);
				String name = res.getString(3);
				consumer.accept(id, setName, name);				
			}
		} catch (SQLException e) {
			Logger.warn(e);
			throw new RuntimeException(e);
		}
	}

	@FunctionalInterface
	public interface ReviewListEntryConsumer {
		void accept(int pos, String photo, String name);
	}

	public void foreachReviewListEntryById(String reviewListId, ReviewListEntryConsumer consumer) {
		try {
			SqlConnector sqlConnector = getSqlConnector();
			PreparedStatement stmt = sqlConnector.getStatement(SQL.QUERY_REVIEW_LIST_ENTRY_BY_REVIEW_LIST);
			stmt.setString(1, reviewListId);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int pos = res.getInt(1);
				String photo = res.getString(2);
				String name = res.getString(3);
				consumer.accept(pos, photo, name);				
			}
		} catch (SQLException e) {
			Logger.warn(e);
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
				//Logger.info(id);
				consumer.accept(id);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void foreachIdNotLocked(Consumer<String> consumer) {
		try {
			SqlConnector sqlconnector = tlsqlconnector.get();
			PreparedStatement stmt = sqlconnector.stmt_query_all_ids_not_locked;
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				String id = res.getString(1);
				//Logger.info(id);
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
				Logger.info("\n\n" + expl + "\n\n");
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

	public void foreachClassificationDefinition(String project, Consumer<ClassificationDefinition> consumer) {
		Vec<ClassificationDefinition> vec = classificationDefinitionsMap.get(project);
		if(vec != null) {
			vec.forEach(consumer);
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

	public Photo2 getPhoto2NotLocked(String id) {
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
				long last_modified = res.getLong(7);
				boolean locked = res.getBoolean(8);
				if(locked) {
					return null;
				}
				Logger.info("locked " + locked + "  " + meta_rel_path + "    " + image_rel_path);
				return new Photo2(id, projectConfig, projectConfig.root_path.resolve(meta_rel_path), projectConfig.root_path.resolve(image_rel_path), location, date, last_modified, locked);
			}
			return null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateThumbs() {
		new Thread(() -> {
			foreachIdNotLocked(photo_id -> {
				try {
					final int parallelism = ForkJoinPool.commonPool().getParallelism();
					int maxQueue = parallelism * 2;					
					long reqWidth = 320;
					long reqHeight = 320;
					String cacheFilename = thumbManager.getCacheFilename(photo_id, reqWidth, reqHeight);
					//Logger.info("id: " + id + " -->  " + cacheFilename);
					ThumbSqlConnector sqlConn = thumbManager.getSqlConnector();
					sqlConn.stmt_exist_id.setString(1, cacheFilename);
					ResultSet res = sqlConn.stmt_exist_id.executeQuery();
					if(res.next()) {
						//Logger.info("true");
					} else {
						Photo2 photo = getPhoto2NotLocked(photo_id);
						if(photo != null) {
							Logger.info("insert " + cacheFilename + "  " + photo.imagePath + "    " + photo.id);
							while(ForkJoinPool.commonPool().getQueuedSubmissionCount() > maxQueue) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									Logger.warn(e);
								}
							}
							//Logger.info(ForkJoinPool.commonPool().getQueuedSubmissionCount());
							//Logger.info("start " + parallelism + "  " + ForkJoinPool.commonPool().getPoolSize() + "  " + ForkJoinPool.commonPool().getRunningThreadCount());
							thumbManager.submitScaled(cacheFilename, photo, reqWidth, reqHeight);					
							//Logger.info("done");
						}
					}
				} catch (Exception e) {
					Logger.warn(e);
					e.printStackTrace();
				}
			});			
		}).start();
	}

	public void readDefinitions() {
		HashMap<String, Vec<ClassificationDefinition>> map = new HashMap<String, Vec<ClassificationDefinition>>();
		for(PhotoProjectConfig pc: config.projectMap.values()) {
			if(pc.classification_definition_csv != null) {
				try (CsvReader csv = CsvReader.builder().commentStrategy(CommentStrategy.SKIP).build(pc.classification_definition_csv, Charset.forName("UTF-8"))) {
					try(CloseableIterator<CsvRow> it = csv.iterator()) {
						if(it.hasNext()) {
							CsvRow header = it.next();
							HashMap<String, Integer> headerMap = new HashMap<String, Integer>();

							for(int i = 0; i < header.getFieldCount(); i++) {
								headerMap.putIfAbsent(header.getField(i), i);
							}

							int iName = headerMap.get("name");
							int iDescription = headerMap.getOrDefault("description", -1);

							Vec<ClassificationDefinition> vec = new Vec<ClassificationDefinition>();

							while(it.hasNext()) {
								CsvRow row = it.next();
								int rowLen = row.getFieldCount();
								String name = row.getField(iName);
								String description = iDescription < 0 || rowLen <= iDescription  ? "" : row.getField(iDescription);
								ClassificationDefinition classificationDefinition = new ClassificationDefinition(name, description);
								vec.add(classificationDefinition);
							}
							map.put(pc.project, vec);
						}
					}
					csv.forEach(System.out::println);
				} catch(Exception e) {
					Logger.warn("error in read classification_definition_csv: " + e);
				}
			}
		}
		classificationDefinitionsMap = map;
	}

	public SqlConnector getSqlConnector() {
		return tlsqlconnector.get();		
	}
	
	public void deleteReviewList(String reviewListId) {
		SqlConnector sqlConnector = getSqlConnector();
		try {
			PreparedStatement stmt = sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_ENTRY_BY_REVIEW_LIST);
			stmt.setString(1, reviewListId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		try {
			PreparedStatement stmt1 = sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_BY_ID);
			stmt1.setString(1, reviewListId);
			stmt1.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void deleteReviewListSet(String setId) {
		SqlConnector sqlConnector = getSqlConnector();
		try {
			PreparedStatement stmt = sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_COLLECTION_BY_ID);
			stmt.setString(1, setId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		foreachReviewListBySet(setId, (id, setName, name) -> {
			Logger.info("remove in set " + setId + "  list " + id);
			deleteReviewList(id);
		});
	}
}
