package photo2.api;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import photo2.PhotoDB2;
import util.Web;

public class PhotoDB2Handler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;
	private final PhotoDB2 photodb;
	private final Photos2Handler photos2Handler;
	private final LocationsHandler locationsHandler;

	public PhotoDB2Handler(Broker broker) {
		this.broker = broker;
		this.photodb = broker.photodb2();
		this.photos2Handler = new Photos2Handler(broker);
		this.locationsHandler = new LocationsHandler(broker);
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
		boolean locations = Web.getFlagBoolean(request, "locations");
		boolean projects = Web.getFlagBoolean(request, "projects");
		String project = Web.getString(request, "project", null);
		
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
		} else if(locations) {
			throw new RuntimeException("locations needs project parameter");
		}
		
		json.endObject();		
	}
}
