package photo2.api;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;

import audio.Broker;
import jakarta.servlet.http.HttpServletResponse;
import photo2.PhotoDB2;
import util.Web;

public class Photos2Handler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB2 photodb;

	private final Photo2Handler photo2Handler;

	public Photos2Handler(Broker broker) {
		this.broker = broker;
		this.photodb = broker.photodb2();
		this.photo2Handler = new Photo2Handler(broker);
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
				photo2Handler.handle(id, next, request, response);
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
		if(Web.has(request, "review_list")) {
			String review_list = Web.getString(request, "review_list");

			response.setContentType("application/json");
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("photos");
			json.array();		
			photodb.foreachReviewListEntryById(review_list, (int pos, String photoId, String name) -> {
				json.value(photoId);
			});		
			json.endArray();
			json.endObject();			
		} else if(Web.has(request, "project")) {
			String project = Web.getString(request, "project");
			String location = Web.getString(request, "location");

			response.setContentType("application/json");
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("photos");
			json.array();		
			photodb.foreachId(project, location, id -> {
				json.value(id);
			});		
			json.endArray();
			json.endObject();
		} else {
			response.setContentType("application/json");
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("photos");
			json.array();	
			json.endArray();
			json.endObject();
		}
	}
}
