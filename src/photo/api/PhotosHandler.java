package photo.api;

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
import util.Web;
import util.collections.vec.Vec;

public class PhotosHandler {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;
	private final PhotoDB photoDB;
	
	private final PhotoHandler photoHandler;

	public PhotosHandler(Broker broker) {
		this.broker = broker;
		this.photoDB = broker.photoDB();
		this.photoHandler = new PhotoHandler(broker);
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
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				photoHandler.handle(name, next, request, response);
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
		
		Vec<Photo> photos = new Vec<Photo>();
		
		if(location != null) {
			LocationPhotoDB locationPhotoDB = photoDB.getLocationPhotoDB(location);
			locationPhotoDB.foreachPhoto(photos::add);
		} else {
			photoDB.foreachPhoto(photos::add);
		}
		
		photos.sort(Photo.comparator);
		
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("photos");
		json.array();
		
		photos.forEach(photo -> {
			json.object();
			json.key("id");
			json.value(photo.id);
			json.endObject();	
		});		
		
		json.endArray();
		json.endObject();	
	}
}
