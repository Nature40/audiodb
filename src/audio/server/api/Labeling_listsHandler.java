package audio.server.api;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.labeling.LabelingList;

public class Labeling_listsHandler extends AbstractHandler {
	

	private final Labeling_listHandler labeling_listHandler;
	
	private final Broker broker;

	public Labeling_listsHandler(Broker broker) {
		this.broker = broker;
		labeling_listHandler = new Labeling_listHandler(broker);
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
				labeling_listHandler.handle(name, next, baseRequest, response);
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
		json.key("labeling_lists");
		json.array();
		broker.labelingListManager().forEach((String id, LabelingList labelingList) -> {
			json.object();
			json.key("id");
			json.value(id);
			json.endObject();
		});
		json.endArray();
		json.endObject();
	}
}