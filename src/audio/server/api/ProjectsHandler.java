package audio.server.api;

import java.io.IOException;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.AudioProjectConfig;
import audio.Broker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ProjectsHandler extends AbstractHandler {
	

	private final Broker broker;
	private final ProjectHandler projectHanlder;

	public ProjectsHandler(Broker broker) {
		this.broker = broker;
		this.projectHanlder = new ProjectHandler(broker);
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
				projectHanlder.handle(name, next, baseRequest, request, response);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
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
		json.key("projects");
		json.array();
		AudioProjectConfig audioProjectConfig = broker.config().audioConfig;
		if(audioProjectConfig != null) {
			String project = audioProjectConfig.project;
			if(project != null && !project.isEmpty()) {
				json.object();
				json.key("id");
				json.value(project);						
				json.endObject();
			}
		}
		json.endArray();

		json.endObject();
	}
}