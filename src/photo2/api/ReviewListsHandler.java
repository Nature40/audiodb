package photo2.api;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
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
import util.Web;

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

	private static HashSet<String> excludeSpecies = new HashSet<String>();
	static {
		excludeSpecies.add("animal");	
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
						//log.info("process " + photoId);
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
						}
					}
				});
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
}
