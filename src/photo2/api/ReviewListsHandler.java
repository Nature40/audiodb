package photo2.api;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Broker;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import photo2.SqlConnector;
import photo2.SqlConnector.SQL;
import photo2.Photo2;
import photo2.PhotoDB2;
import util.JsonUtil;
import util.Web;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class ReviewListsHandler {

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
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(Web.MIME_TEXT);
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

	private AtomicInteger reviewListIdPrefixCounter = new AtomicInteger();

	private String getCategoryName(String groupBy, String classification, String location) {
		switch(groupBy) {
		case "classification":
			return classification;
		case "location":
			return location;
		case "classification_location":
			return classification + '_' + location;
		case "location_classification":
			return location + '_' + classification;
		default:
			throw new RuntimeException("unknown group by");
		}		
	}

	private String getListName(String groupBy, String classification, String location) {
		switch(groupBy) {
		case "classification":
			return classification;
		case "location":
			return location;
		case "classification_location":
			return classification + " (location: " + location + ")";
		case "location_classification":
			return location + " (classification: " + classification + ")";
		default:
			throw new RuntimeException("unknown group by");
		}		
	}

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
			case "create_review_list_set": {
				String project = Web.getString(request, "project");	
				String setName = jsonAction.optString("set_name", null);
				String recipe = jsonAction.toString();
				createReviewList(project, setName, jsonAction, recipe);
				break;
			}
			case "refresh_review_list_set": {
				String[] sets = JsonUtil.optStrings(jsonAction, "sets");
				for(String setId : sets) {
					Logger.info("refresh set " + setId);					
					photodb.forReviewListSetById(setId, (id, project, name, recipe) -> {
						JSONObject jsonRecipe = new JSONObject(recipe);
						photodb.deleteReviewListSet(id);
						createReviewList(project, name, jsonRecipe, recipe);
					});
				}
				break;
			}
			case "remove_review_list_set": {
				String[] sets = JsonUtil.optStrings(jsonAction, "sets");
				for(String setId : sets) {
					Logger.info("remove set " + setId);
					photodb.deleteReviewListSet(setId);
				}
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("result");
		json.value("OK");
		json.endObject();
	}
	
	public void createReviewList(String project, String setName_, JSONObject jsonAction, String recipe) {
		
		String filterClassificator = jsonAction.optString("prefilter_classificator", null);
		float filterConf = jsonAction.optFloat("prefilter_threshold");
		String classClassificator = jsonAction.optString("classification_classificator", null);
		float classConf = jsonAction.optFloat("classification_threshold");
		boolean sortedByRanking = jsonAction.optBoolean("sorted_by_ranking", false);
		boolean omitExpertClassified = jsonAction.optBoolean("omit_expert_classified", false);
		String groupBy = jsonAction.optString("group_by", null);
		Logger.info("create_review_list");
		HashMap<String, Integer> occurringListCategry = new HashMap<String, Integer>();				
		//String username = account.username;
		long timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();				
		SqlConnector sqlConnector = photodb.getSqlConnector();	
		//String reviewListId = "temp__" + username + "__" + Math.abs(SecureRandom.getInstanceStrong().nextLong());
		//String reviewListId = "tmp" + Math.abs(SecureRandom.getInstanceStrong().nextLong());
		//String reviewListPrefix = "tmp" + Math.abs(ThreadLocalRandom.current().nextLong());
		int cnt = reviewListIdPrefixCounter.incrementAndGet();				
		if(setName_ == null || setName_.isBlank()) {
			setName_ = ""+cnt;
		}
		final String setName = setName_;				
		String setId = project + "__" + setName;

		try {
			PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST_COLLECTION);
			stmt.setString(1, setId);
			stmt.setString(2, project);
			stmt.setString(3, setName);
			stmt.setString(4, recipe);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		PreparedStatement insStmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST_ENTRY);
		photodb.foreachIdSortDate(project, null, photoId -> {
			Photo2 photo = photodb.getPhoto2(photoId, true);
			if(photo != null) {
				String location = photo.location;
				PhotoMeta photoMeta = new PhotoMeta(photo.getMeta());
				Vec<Detection> detections = photoMeta.getDetections();
				Map<String, CollectorEntry> collectorMap = new HashMap<String, CollectorEntry>();
				if(detections.isEmpty()) {
					collectClassifications(photo, new Detection(null), collectorMap, filterClassificator, filterConf, classClassificator, classConf);
				} else {							
					boolean processPhoto = true;
					if(omitExpertClassified) {
						boolean hasExpert = detections.some(detection -> {								
							return detection.classifications.some(classificationEntry -> {
								String classificator = classificationEntry.optString("classificator");
								return "Expert".equals(classificator);
							});
						});
						processPhoto = !hasExpert;
					}
					if(processPhoto) {
						detections.forEach(detection -> {
							collectClassifications(photo, detection, collectorMap, filterClassificator, filterConf, classClassificator, classConf);					
						});
					}
				}
				for(CollectorEntry collectorEntry : collectorMap.values()) {
					try {
						String listCategory = getCategoryName(groupBy, collectorEntry.classification, location);
						String reviewListId = setId + "__" + listCategory;
						String reviewListName = getListName(groupBy, collectorEntry.classification, location);
						float ranking = collectorEntry.ranking;
						Integer pos = occurringListCategry.get(listCategory);
						if(pos == null) {
							try {
								PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST);
								stmt.setString(1, reviewListId);
								stmt.setString(2, project);
								stmt.setString(3, setId);
								stmt.setString(4, reviewListName);
								stmt.setLong(5, timestamp);
								stmt.executeUpdate();
							} catch (SQLException e) {
								throw new RuntimeException(e);
							}
							pos = 0;
						}
						pos++; // 1 based index
						occurringListCategry.put(listCategory, pos);
						insStmt.setString(1, reviewListId);
						insStmt.setInt(2, pos);
						insStmt.setString(3, photoId);
						insStmt.setString(4, listCategory);
						insStmt.setFloat(5, ranking);
						insStmt.executeUpdate();
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}								
				}
			}
		});

		if(sortedByRanking) {
			for(String listCategory : occurringListCategry.keySet()) {

				String reviewListId = setId + "__" + listCategory;
				String reviewListName = null;
				try {
					PreparedStatement stmt = sqlConnector.getStatement(SQL.QUERY_REVIEW_LIST_BY_ID);
					stmt.setString(1, reviewListId);
					ResultSet res = stmt.executeQuery();
					if(!res.next()) {
						throw new RuntimeException("review list not found");
					}
					reviewListName = res.getString(3);
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}

				String reviewListIdOrdered = reviewListId + "__" + "ordered";
				String reviewListIdOrderedName = reviewListName;

				try {
					PreparedStatement stmt = sqlConnector.getStatement(SQL.INSERT_REVIEW_LIST);
					stmt.setString(1, reviewListIdOrdered);
					stmt.setString(2, project);
					stmt.setString(3, setId);
					stmt.setString(4, reviewListIdOrderedName);
					stmt.setLong(5, timestamp);
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
				photodb.deleteReviewList(reviewListId);						
			}
		}

		Logger.info("create_review_list done.");
	}

	static class CollectorEntry {
		private Photo2 photo;
		public String classification;
		public float ranking;

		public CollectorEntry(Photo2 photo, String classification, float ranking) {
			this.photo = photo;
			this.classification = classification;
			this.ranking = ranking;
		}
	}

	private static void collectClassifications(Photo2 photo, Detection detection, Map<String, CollectorEntry> collectorMap, String filterClassificator, float filterConf, String classClassificator, float classConf) {
		Logger.info(photo.id);
		Vec<YamlMap> classifications = detection.classifications;
		/*if(classifications.isEmpty()) {
			return;
		}*/
		/*if(omitExpertClassified) {
			if(classifications.some(classificationEntry -> {
				String classificator = classificationEntry.optString("classificator");
				return "Expert".equals(classificator);
			})) {
				return;
			}
		}*/
		if(filterClassificator != null) {
			Logger.info("filter");
			YamlMap prefilterClassification = classifications.findLast(classificationEntry -> {
				String classificator = classificationEntry.optString("classificator");
				return filterClassificator.equals(classificator);
			});
			if(prefilterClassification == null) {
				return;
			}
			if(Float.isFinite(filterConf)) {
				float prefilterConf = prefilterClassification.optFloat("conf");
				if(!Float.isFinite(prefilterConf) || prefilterConf < filterConf) {
					return;
				}
			}
		}
		Logger.info("filter pass");

		if(classClassificator != null) {		
			YamlMap netClassification = classifications.findLast(classificationEntry -> {
				String classificator = classificationEntry.optString("classificator");
				return classClassificator.equals(classificator);
			});
			if(netClassification == null) {
				return;
			}

			String netClass = netClassification.optString("classification", "unnamed");
			float netConf = netClassification.optFloat("conf");
			if(Float.isFinite(classConf)) {
				if(!Float.isFinite(netConf) || netConf < classConf) {
					return;
				}
			}
			float ranking = netConf;
			CollectorEntry collectorEntry = collectorMap.get(netClass);
			if(collectorEntry == null || ranking > collectorEntry.ranking) {
				collectorEntry = new CollectorEntry(photo, netClass, ranking);
				collectorMap.put(netClass, collectorEntry);
			}
		} else {
			String netClass = "unnamed";
			float ranking = Float.NaN;
			CollectorEntry collectorEntry = collectorMap.get(netClass);
			if(collectorEntry == null || ranking > collectorEntry.ranking) {
				collectorEntry = new CollectorEntry(photo, netClass, ranking);
				collectorMap.put(netClass, collectorEntry);
			}
		}
	}
}
