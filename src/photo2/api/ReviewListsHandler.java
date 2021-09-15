package photo2.api;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import audio.Broker;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import photo2.Photo2;
import photo2.PhotoDB2;
import photo2.SqlConnector;
import photo2.SqlConnector.SQL;
import photo2.api.ReviewListsHandler.CollectorEntry;
import util.Web;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class ReviewListsHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB2 photodb;

	public ReviewListsHandler(Broker broker) {
		this.broker = broker;
		this.photodb = broker.photodb2();
	}

	public void handle(String target, Request request, HttpServletResponse response) throws IOException {
		try {
			request.setHandled(true);
			if(target.equals("/")) {
				handleRoot(request, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String id = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				throw new RuntimeException("no sub");
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}		
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "POST":
			handleRoot_POST(request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	/*private static HashSet<String> excludeSpecies = new HashSet<String>();
	static {
		excludeSpecies.add("animal");	
	}*/

	private void handleRoot_POST(Request request, HttpServletResponse response) throws IOException {


		HttpSession session = request.getSession(false);
		//Account account = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readOnly.checkHasNot(roleBits);

		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "create_review_list": {
				log.info("create_review_list");
				HashMap<String, Integer> occurringSpecies = new HashMap<String, Integer>();				
				//String username = account.username;
				long timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();				
				SqlConnector sqlConnector = photodb.getSqlConnector();
				String project = Web.getString(request, "project");		
				//String reviewListId = "temp__" + username + "__" + Math.abs(SecureRandom.getInstanceStrong().nextLong());
				//String reviewListId = "tmp" + Math.abs(SecureRandom.getInstanceStrong().nextLong());
				String reviewListPrefix = "tmp" + Math.abs(ThreadLocalRandom.current().nextLong());

				PreparedStatement insStmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST_ENTRY);
				photodb.foreachId(project, null, photoId -> {
					Photo2 photo = photodb.getPhoto2NotLocked(photoId);
					if(photo != null) {
						PhotoMeta photoMeta = new PhotoMeta(photo.getMeta());
						Vec<Detection> detections = photoMeta.getDetections();
						Map<String, CollectorEntry> collectorMap = new HashMap<String, CollectorEntry>();
						detections.forEach(detection -> {							
							collectClassifications(detection, collectorMap);					
						});
						for(CollectorEntry collectorEntry : collectorMap.values()) {
							try {
								String species = collectorEntry.classification;
								String reviewListId = reviewListPrefix + "__" + species;
								float ranking = collectorEntry.ranking;
								Integer pos = occurringSpecies.get(species);
								if(pos == null) {
									try {
										PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST);
										stmt.setString(1, reviewListId);
										stmt.setString(2, project);
										stmt.setString(3, species);
										stmt.setLong(4, timestamp);
										stmt.executeUpdate();
									} catch (SQLException e) {
										throw new RuntimeException(e);
									}
									pos = 0;
								}
								pos++; // 1 based index
								occurringSpecies.put(species, pos);
								insStmt.setString(1, reviewListId);
								insStmt.setInt(2, pos);
								insStmt.setString(3, photoId);
								insStmt.setString(4, species);
								insStmt.setFloat(5, ranking);
								insStmt.executeUpdate();
							} catch (SQLException e) {
								throw new RuntimeException(e);
							}								
						}


						/*//log.info("process " + photoId);
						HashSet<String> localOccurringSpecies = new HashSet<String>();
						photo.foreachDetection(map -> {
							map.optList("classifications").asMaps().forEach(cmap -> {
								if(cmap.contains("classification")) {
									localOccurringSpecies.add(cmap.getString("classification"));
								}							
							});
						});

						for(String species : localOccurringSpecies) {
							if(excludeSpecies.contains(species)) {
								continue;
							}
							try {
								String reviewListId = reviewListPrefix + "__" + species;
								Integer pos = occurringSpecies.get(species);
								if(pos == null) {
									try {
										PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST);
										stmt.setString(1, reviewListId);
										stmt.setString(2, project);
										stmt.setString(3, species);
										stmt.setLong(4, timestamp);
										stmt.executeUpdate();
									} catch (SQLException e) {
										throw new RuntimeException(e);
									}
									pos = 0;
								}
								pos++; // 1 based index
								occurringSpecies.put(species, pos);
								insStmt.setString(1, reviewListId);
								insStmt.setInt(2, pos);
								insStmt.setString(3, photoId);
								insStmt.setString(4, species);
								insStmt.executeUpdate();
							} catch (SQLException e) {
								throw new RuntimeException(e);
							}	
						}*/
					}
				});

				for(String species : occurringSpecies.keySet()) {
					String reviewListId = reviewListPrefix + "__" + species;
					String reviewListIdOrdered = reviewListPrefix + "__" + species + "__" + "ordered";					
					try {
						PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST);
						stmt.setString(1, reviewListIdOrdered);
						stmt.setString(2, project);
						stmt.setString(3, species);
						stmt.setLong(4, timestamp);
						stmt.executeUpdate();
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
					try {
						PreparedStatement stmt = sqlConnector.getStatement(SQL.QUERY_REVIEW_LIST_ENTRY_BY_ID_ORDER_BY_RANKING);
						stmt.setString(1, reviewListId);
						ResultSet res = stmt.executeQuery();
						int pos = 0;
						while(res.next()) {
							String photoId = res.getString(1);
							String name = res.getString(2);
							float ranking = res.getFloat(3);							

							pos++; // 1 based index
							insStmt.setString(1, reviewListIdOrdered);
							insStmt.setInt(2, pos);
							insStmt.setString(3, photoId);
							insStmt.setString(4, name);
							insStmt.setFloat(5, ranking);
							insStmt.executeUpdate();
						}
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}

					try {
						PreparedStatement stmt = sqlConnector.getStatement(SQL.DELETE_REVIEW_LIST_ENTRY_BY_ID);
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

				log.info("create_review_list done.");
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("result");
		json.value("OK");
		json.endObject();
	}

	static class CollectorEntry {
		public String classification;
		public float ranking;

		public CollectorEntry(String classification, float ranking) {
			this.classification = classification;
			this.ranking = ranking;
		}
	}

	private static void collectClassifications(Detection detection, Map<String, CollectorEntry> collectorMap) {
		Vec<YamlMap> classifications = detection.classifications;
		if(classifications.isEmpty()) {
			return;
		}
		YamlMap megaDetectorClassification = classifications.findLast(classificationEntry -> {
			String classificator = classificationEntry.optString("classificator");
			return "MegaDetector".equals(classificator);
		});
		if(megaDetectorClassification == null) {
			return;
		}
		YamlMap netClassification = classifications.findLast(classificationEntry -> {
			String classificator = classificationEntry.optString("classificator");
			return "EfficientNetB3".equals(classificator);
		});
		if(netClassification == null) {
			return;
		}
		float megaDetectorConf = megaDetectorClassification.optFloat("conf");
		if(!Float.isFinite(megaDetectorConf) || megaDetectorConf < 0.8f) {
			return;
		}
		String netClass = netClassification.optString("classification");
		if(netClass == null) {
			return;
		}
		float netConf = netClassification.optFloat("conf");
		if(!Float.isFinite(netConf) || netConf < 0.8f) {
			return;
		}
		float ranking = netConf;
		CollectorEntry collectorEntry = collectorMap.get(netClass);
		if(collectorEntry == null || ranking > collectorEntry.ranking) {
			collectorEntry = new CollectorEntry(netClass, ranking);
			collectorMap.put(netClass, collectorEntry);
		}
	}
}
