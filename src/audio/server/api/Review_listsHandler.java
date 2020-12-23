package audio.server.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.ReviewList;

public class Review_listsHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	private final Review_listHandler review_listHandler;
	
	private final Broker broker;

	public Review_listsHandler(Broker broker) {
		this.broker = broker;
		review_listHandler = new Review_listHandler(broker);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
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
				review_listHandler.handle(name, next, baseRequest, response);
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
		json.key("review_lists");
		json.array();
		broker.reviewListManager().forEach((String id, ReviewList reviewList) -> {
			json.object();
			json.key("id");
			json.value(id);
			json.endObject();
		});
		json.endArray();
		json.endObject();
	}
}