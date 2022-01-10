package audio.server.api;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Broker;
import audio.task.Task;
import audio.task.Task.Ctx;
import audio.task.Tasks;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class TasksHandler extends AbstractHandler {

	private final Broker broker;
	private final Tasks tasks;

	public TasksHandler(Broker broker) {
		this.broker = broker;
		this.tasks = broker.tasks();
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
	
	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRoot_GET(request, response);
			break;
		case "POST":
			handleRoot_POST(request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	private void handleRoot_GET(Request request, HttpServletResponse response) throws IOException {
		boolean fDescriptors = Web.getFlagBoolean(request, "descriptors");
		boolean fTasks = Web.getFlagBoolean(request, "tasks");

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		if(fDescriptors) {
			json.key("descriptors");
			json.object();
			Tasks.foreachDescriptor((name, descriptor) -> {
				json.key("name");
				json.value(name);
			});
			json.endObject();
		}
		if(fTasks) {
			json.key("tasks");
			json.array();
			for(Task task:tasks.getTasks()) {
				Ctx ctx = task.geCtx();
				json.object();
				json.key("id");
				json.value(ctx.id);
				json.key("task");
				json.value(ctx.descriptor.name);
				json.key("state");
				json.value(task.getState().toString());
				json.key("message");
				json.value(task.getMessage());
				json.endObject();
			}
			json.endArray();
		}
		json.endObject();
	}
	
	private void handleRoot_POST(Request request, HttpServletResponse response) throws IOException {
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONObject jsonAction = jsonReq.getJSONObject("action");
		String actionName = jsonAction.getString("action");
		switch(actionName) {
		case "submit": {
			JSONObject jsonTask = jsonAction.getJSONObject("task");
			String id = tasks.submit(jsonTask);
			response.setContentType("application/json");
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("result");
			json.object();
			json.key("id");
			json.value(id);
			json.endObject();
			json.endObject();
			break;
		}
		default:
			throw new RuntimeException("unknown action:" + actionName);
		}	
	}	
	
}