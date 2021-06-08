package photo.api;

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
import photo.PhotoDB;
import util.Web;

public class PhotoDBHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;
	private final PhotoDB photoDB;
	private final PhotosHandler photosHandler;

	public PhotoDBHandler(Broker broker) {
		this.broker = broker;
		this.photoDB = broker.photoDB();
		this.photosHandler = new PhotosHandler(broker);
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
					photosHandler.handle(next, baseRequest, response);					
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
		
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		
		if(locations) {
			json.key("locations");
			json.array();
			photoDB.foreachLocation(location -> {
				json.object();
				json.key("id");
				json.value(location.id);
				json.endObject();	
			});	
			json.endArray();
		}
		
		json.endObject();		
	}
}
