package audio.server.api;

import java.io.IOException;
import java.util.BitSet;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Account;
import audio.Broker;
import audio.role.RoleManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import task.Ctx;
import task.Descriptor.Param;
import task.Task;
import task.Tasks;
import util.AudioTimeUtil;
import util.Web;

public class TasksHandler extends AbstractHandler {

	private final Broker broker;
	private final Tasks tasks;
	private final TaskHandler taskHandler;
	private final RoleManager roleManager;

	public TasksHandler(Broker broker) {
		this.broker = broker;
		this.tasks = broker.tasks();
		this.taskHandler = new TaskHandler(broker);
		this.roleManager = broker.roleManager();
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
				Task task = tasks.getTask(name);
				if(task == null) {
					throw new RuntimeException("task not found");
				}
				taskHandler.handle(task, next, baseRequest, response);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType(Web.MIME_TEXT);
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

	private static void taskToJSON(Task task, JSONWriter json) {
		Ctx ctx = task.geCtx();
		json.object();
		json.key("id");
		json.value(ctx.id);
		json.key("task");
		json.value(ctx.descriptor.name);
		json.key("state");
		json.value(task.getState().toString());
		json.key("start");
		json.value(AudioTimeUtil.DATE_SPACE_TIME_FORMATTER.format(task.startDateTime));
		json.key("runtime");
		json.value(task.getRuntimeText());
		json.key("identity");
		json.value(task.geCtx().account == null ? "unknown" : task.geCtx().account.username);
		json.key("message");
		json.value(task.getMessage());
		json.endObject();		
	}

	private void handleRoot_GET(Request request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		//Account sessionAccount = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");

		boolean fDescriptors = Web.getFlagBoolean(request, "descriptors");
		boolean fTasks = Web.getFlagBoolean(request, "tasks");

		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		if(fDescriptors) {
			json.key("descriptors");
			json.object();
			Tasks.foreachDescriptor((name, descriptor) -> {
				if(roleManager.hasRoles(roleBits, descriptor.roles)) {
					json.key(name);
					json.object();
					json.key("name");
					json.value(name);
					if(descriptor.description != null) {
						json.key("description");
						json.value(descriptor.description);
					}
					if(descriptor.tags != null) {
						json.key("tags");
						json.array();
						for(String tag : descriptor.tags) {
							json.value(tag);
						}
						json.endArray();
					}
					json.key("cancelable");
					json.value(descriptor.cancelable);
					if(descriptor.params != null) {
						json.key("params");
						json.array();
						for(Param param : descriptor.params) {
							json.object();
							json.key("name");
							json.value(param.name);
							json.key("type");
							json.value(param.type);
							json.key("preset");
							json.value(param.preset);
							if(param.description != null) {
								json.key("description");
								json.value(param.description);
							}
							json.endObject();
						}
						json.endArray();
					}
					json.endObject();
				}
			});
			json.endObject();			
		}
		if(fTasks) {
			json.key("tasks");
			json.array();
			for(Task task:tasks.getTasks()) {
				taskToJSON(task, json);
			}
			json.endArray();
		}
		json.endObject();
	}

	private void handleRoot_POST(Request request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		Account sessionAccount = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		//if(request.getCharacterEncoding() == null) { // no direct getter
		request.setCharacterEncoding("utf-8");
		//}
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONObject jsonAction = jsonReq.getJSONObject("action");
		String actionName = jsonAction.getString("action");
		switch(actionName) {
		case "submit": {
			JSONObject jsonTask = jsonAction.getJSONObject("task");
			String id = tasks.submit(jsonTask, sessionAccount, roleBits);
			response.setContentType(Web.MIME_JSON);
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