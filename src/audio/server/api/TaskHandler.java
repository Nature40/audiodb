package audio.server.api;

import java.io.IOException;
import java.time.Duration;

import org.eclipse.jetty.server.Request;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Broker;
import audio.task.Task;
import audio.task.Tasks;
import audio.task.Task.Ctx;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

public class TaskHandler {

	private final Broker broker;
	private final Tasks tasks;

	public TaskHandler(Broker broker) {
		this.broker = broker;
		this.tasks = broker.tasks();
	}

	public void handle(Task task, String target, Request request, HttpServletResponse response) throws IOException, ServletException {
		try {
			request.setHandled(true);
			if(target.equals("/")) {
				handleRoot(task, request, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				//taskHanlder.handle(name, next, baseRequest, request, response);
				throw new RuntimeException("no page");
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

	private void handleRoot(Task task, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRoot_GET(task, request, response);
			break;
		case "POST":
			handleRoot_POST(task, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}
	
	private static void taskToJSON(Task task, JSONWriter json) {
		Ctx ctx = task.geCtx();
		json.object();
		json.key("task");
		json.value(ctx.json);
		json.key("id");
		json.value(ctx.id);
		json.key("name");
		json.value(ctx.descriptor.name);
		json.key("state");
		json.value(task.getState());
		json.key("message");
		json.value(task.getMessage());
		json.key("runtime");
		json.value(task.getRuntimeText());
		json.key("log");
		json.array();
		task.foreachLog(json::value);
		json.endArray();
		json.endObject();		
	}

	private void handleRoot_GET(Task task, Request request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		taskToJSON(task, json);		
	}

	private void handleRoot_POST(Task task, Request request, HttpServletResponse response) throws IOException {
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONObject jsonAction = jsonReq.getJSONObject("action");
		String actionName = jsonAction.getString("action");
		switch(actionName) {
		default:
			throw new RuntimeException("unknown action:" + actionName);
		}	
	}	
}