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

public class Photos2Handler {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;
	private final PhotoDB2 photodb2;
	
	private final Photo2Handler photo2Handler;

	public Photos2Handler(Broker broker) {
		this.broker = broker;
		this.photodb2 = broker.photodb2();
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
		String location = Web.getString(request, "location", null);
		
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("photos");
		json.array();		
		photodb2.foreachId(location, id -> {
			json.value(id);
		});		
		json.endArray();
		json.endObject();
	}
}
