package photo.api;

import java.io.IOException;
import java.time.LocalDate;
import java.util.TreeSet;

import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Broker;
import jakarta.servlet.http.HttpServletResponse;
import photo.PhotoDB2;
import util.Web;

public class DatesHandler {

	private final Broker broker;
	private final PhotoDB2 photodb2;

	public DatesHandler(Broker broker) {
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

		String project = Web.getString(request, "project", null);
		String location = Web.getString(request, "location", null);

		if(project != null) {
			if(!photodb2.config.projectMap.containsKey(project)) {
				throw new RuntimeException("project not found");
			}
			json.key("project");
			json.value(project);
			if(location != null) {
				writeDatesJSON(project, location, json);
			} else {
				writeDatesJSON(project, json);
			}
		} else {
			throw new RuntimeException("needs project parameter");
		}		

		json.endObject();
	}

	public void writeDatesJSON(String project, JSONWriter json) {
		json.key("dates");
		json.array();
		TreeSet<LocalDate> dates = new TreeSet<LocalDate>();
		photodb2.foreachDate(project, null, date -> {
			dates.add(date.toLocalDate());			
		});
		for(LocalDate date:dates) {			
			json.value(date);
		}
		json.endArray();
	}
	
	public void writeDatesJSON(String project, String location, JSONWriter json) {
		json.key("location");
		json.value(location);
		json.key("dates");
		json.array();
		TreeSet<LocalDate> dates = new TreeSet<LocalDate>();
		photodb2.foreachDate(project, location, date -> {
			dates.add(date.toLocalDate());			
		});
		for(LocalDate date:dates) {
			json.value(date);
		}
		json.endArray();
	}

}
