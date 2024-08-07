package photo.api;

import java.io.IOException;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import photo.ClassificationDefinition;
import photo.PhotoDB2;
import util.Web;

public class PhotoDB2Handler extends AbstractHandler {

	private final Broker broker;
	private final PhotoDB2 photodb;
	private final Photos2Handler photos2Handler;
	private final LocationsHandler locationsHandler;
	private final DatesHandler datesHandler;
	private final ReviewListsHandler reviewListsHandler;

	public PhotoDB2Handler(Broker broker) {
		this.broker = broker;
		this.photodb = broker.photodb2();
		this.photos2Handler = new Photos2Handler(broker);
		this.locationsHandler = new LocationsHandler(broker);
		this.datesHandler = new DatesHandler(broker);
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
				case "dates":
					datesHandler.handle(next, baseRequest, response);					
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
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		boolean projects = Web.getFlagBoolean(request, "projects");
		String project = Web.getString(request, "project", null);
		boolean locations = Web.getFlagBoolean(request, "locations");
		boolean dates = Web.getFlagBoolean(request, "dates");
		boolean classification_definitions = Web.getFlagBoolean(request, "classification_definitions");
		boolean review_lists = Web.getFlagBoolean(request, "review_lists");	

		response.setContentType(Web.MIME_JSON);
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
			if(dates) {
				datesHandler.writeDatesJSON(project, json);	
			}
			if(classification_definitions) {				
				json.key("classification_definitions");
				json.array();				
				photodb.foreachClassificationDefinition(project, ClassificationDefinition.toJsonConsumer(json));
				json.endArray();
			}
			if(review_lists) {
				json.key("review_list_sets");
				json.array();
				photodb.foreachReviewListSetByProject(project, (id, project2, name, recipe) -> {
					json.object();
					json.key("id");
					json.value(id);
					json.key("project");
					json.value(project2);
					json.key("name");
					json.value(name);
					json.key("recipe");
					json.value(recipe);
					json.endObject();
				});
				json.endArray();
				json.key("review_lists");
				json.array();
				photodb.foreachReviewListByProject(project, (id, set, name) -> {					
					int count = photodb.reviewListEntryByIdEntriesCount(id);					
					json.object();
					json.key("id");
					json.value(id);
					json.key("project");
					json.value(project);
					json.key("set");
					json.value(set);
					json.key("name");
					json.value(name);
					json.key("count");
					json.value(count);
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
