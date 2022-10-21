package audio.server.api;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Broker;
import audio.worklist.WorklistStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class WorklistsHandler extends AbstractHandler {
	private final Broker broker;

	private final WorklistHandler worklistHandler;
	private final WorklistStore worklistStore;

	public WorklistsHandler(Broker broker) {
		this.broker = broker;
		worklistHandler = new WorklistHandler(broker);
		this.worklistStore = broker.worklistStore();
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {		
		try {
			Logger.info(target);
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
				worklistHandler.handle(name, next, baseRequest, response);
			}
		} catch(Exception e) {
			Logger.error(target);
			Logger.error(e);
			e.printStackTrace();
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: " + e.getMessage());
			} catch(Exception e1) {
				Logger.warn(e1);
			}
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();

		json.key("worklists");
		json.array();
		worklistStore.forEachWorklist((worklistId, worklist) -> {
			json.object();
			json.key("id");
			json.value(worklistId);
			json.key("count");
			json.value(worklist.size());
			json.endObject();
		});
		json.endArray();

		json.endObject();
	}
}
