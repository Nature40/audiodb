package photo2.api;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;

import audio.Broker;
import photo.LocationPhotoDB;
import photo.Photo;
import photo.PhotoDB;
import photo2.PhotoDB2;
import util.Web;
import util.collections.vec.Vec;

public class LocationsHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB2 photodb2;

	public LocationsHandler(Broker broker) {
		this.broker = broker;
		this.photodb2 = broker.photodb2();
	}

	public void handle(String target, Request request, HttpServletResponse response) throws IOException {
		try {
			request.setHandled(true);
			if(target.equals("/")) {
				handleRoot(request, response);
			} else {
				throw new RuntimeException("unknown url");
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
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		writeLocationsJSON(json);		
		json.endObject();
	}

	public void writeLocationsJSON(JSONWriter json) {
		json.key("locations");
		json.array();		
		photodb2.foreachProject(project -> {
			photodb2.foreachLocation(project.project, location -> {
				json.value(location);
			});
		});
		json.endArray();
	}

	public void writeLocationsJSON(String project, JSONWriter json) {
		json.key("locations");
		json.array();		
		photodb2.foreachLocation(project, location -> {
			json.value(location);
		});
		json.endArray();
	}
}
