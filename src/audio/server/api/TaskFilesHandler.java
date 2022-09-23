package audio.server.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.h2.util.IOUtils;
import org.tinylog.Logger;

import audio.Broker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import task.Task;
import task.TaskResult;
import task.TaskResult.File;
import task.Tasks;

public class TaskFilesHandler {

	private final Broker broker;
	private final Tasks tasks;

	public TaskFilesHandler(Broker broker) {
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
				int resultNumber = Integer.parseInt(name);
				handleFile(task, resultNumber, next, request, response);
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
		throw new RuntimeException("missing file");
	}
	
	private void handleFile(Task task, int resultNumber, String next, Request request, HttpServletResponse response) throws FileNotFoundException, IOException {
		TaskResult result = task.getResult(resultNumber);
		if(result instanceof TaskResult.File) {
			File r = (TaskResult.File) result;
			IOUtils.copy(new FileInputStream(r.path.toFile()), response.getOutputStream());
		} else {
			throw new RuntimeException("no file");
		}
	}
}