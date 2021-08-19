package photo2.api;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import photo2.ClassificationDefinition;
import photo2.Photo2;
import photo2.PhotoDB2;
import photo2.SqlConnector;
import photo2.SqlConnector.SQL;
import util.Web;

public class PhotoDB2Handler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB2 photodb;
	private final Photos2Handler photos2Handler;
	private final LocationsHandler locationsHandler;
	private final ReviewListsHandler reviewListsHandler;

	public PhotoDB2Handler(Broker broker) {
		this.broker = broker;
		this.photodb = broker.photodb2();
		this.photos2Handler = new Photos2Handler(broker);
		this.locationsHandler = new LocationsHandler(broker);
		this.reviewListsHandler = new ReviewListsHandler(broker);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);		

		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				handleRoot(baseRequest, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				switch(name) {
				case "photos":
					photos2Handler.handle(next, baseRequest, response);					
					break;
				case "locations":
					locationsHandler.handle(next, baseRequest, response);					
					break;
				case "review_lists":
					reviewListsHandler.handle(next, baseRequest, response);					
					break;						
				default:
					throw new RuntimeException("unknown url");
				}

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
		boolean projects = Web.getFlagBoolean(request, "projects");
		String project = Web.getString(request, "project", null);
		boolean locations = Web.getFlagBoolean(request, "locations");
		boolean classification_definitions = Web.getFlagBoolean(request, "classification_definitions");
		boolean review_lists = Web.getFlagBoolean(request, "review_lists");	

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();

		if(projects) {
			json.key("projects");
			json.array();
			photodb.foreachProject(projectConfig -> {
				json.value(projectConfig.project);
			});
			json.endArray();
		}

		if(project != null) {
			if(!photodb.config.projectMap.containsKey(project)) {
				throw new RuntimeException("project not found");
			}
			json.key("project");
			json.value(project);
			if(locations) {
				locationsHandler.writeLocationsJSON(project, json);	
			}
			if(classification_definitions) {				
				json.key("classification_definitions");
				json.array();				
				photodb.foreachClassificationDefinition(project, ClassificationDefinition.toJsonConsumer(json));
				json.endArray();
			}

			if(review_lists) {
				json.key("review_lists");
				json.array();
				photodb.foreachReviewListByProject(project, (id, name) -> {
					json.object();
					json.key("id");
					json.value(id);
					json.key("name");
					json.value(name);
					json.endObject();
				});
				json.endArray();
			}
		} else if(locations) {
			throw new RuntimeException("locations needs project parameter");
		}

		json.endObject();		
	}
}
