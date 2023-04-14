package photo2.api;

import java.io.IOException;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;

import audio.Broker;
import jakarta.servlet.http.HttpServletResponse;
import photo2.PhotoDB2;
import util.Web;

public class LocationsHandler {

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
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("ERROR: " + e.getMessage());
		}		
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {		
		response.setContentType(Web.MIME_JSON);
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
