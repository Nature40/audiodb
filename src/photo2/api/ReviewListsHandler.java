package photo2.api;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import audio.Broker;
import jakarta.servlet.http.HttpServletResponse;
import photo2.PhotoDB2;
import util.Web;

public class ReviewListsHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB2 photodb;

	public ReviewListsHandler(Broker broker) {
		this.broker = broker;
		this.photodb = broker.photodb2();
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
				throw new RuntimeException("no sub");
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
		String project = Web.getString(request, "project");
	}
}
