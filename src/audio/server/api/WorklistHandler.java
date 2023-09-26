package audio.server.api;

import java.io.IOException;
import java.util.function.Predicate;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Broker;
import audio.Label;
import audio.Label.LabelStatus;
import audio.Sample2;
import audio.worklist.Worklist;
import audio.worklist.WorklistEntry;
import audio.worklist.WorklistStore;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class WorklistHandler {

	private final Broker broker;
	private final WorklistStore worklistStore;
	
	private final Predicate<WorklistEntry> SKIP_DONE_PREDICATE;
	private static final Predicate<WorklistEntry> TRUE_PREDICATE = e -> true;

	public WorklistHandler(Broker broker) {
		this.broker = broker;
		this.worklistStore = broker.worklistStore();		
		
		SKIP_DONE_PREDICATE = e -> {
			Logger.info(e.sample);
			//Sample2 sample = broker.sampleManager().getById(e.sample);
			Sample2 sample = broker.sampleStorage().getSample(Integer.parseInt(e.sample));
			if(sample == null) {
				return false;
			}
			Logger.info(e.start + "  " + e.end);
			Label label = sample.getLabel(e.start, e.end);
			if(label == null) {
				return true;
			}
			Logger.info(label);
			Logger.info("|" + label.labelStatus + "|");
			return label.labelStatus != LabelStatus.DONE;
			
		};		
	}

	public void handle(String worklistId, String target, Request request, HttpServletResponse response) throws IOException {
		int i = target.indexOf('/', 1);
		if(i == 1) {
			throw new RuntimeException("no name: "+target);
		}			
		String name = i < 0 ? target.substring(1) : target.substring(1, i);
		String next = i < 0 ? "/" : target.substring(i);
		switch(name) {
		case "": 
			handleRoot(worklistId, request, response);
			break;
		case "first":
			handleFirst(worklistId, request, response);
			break;
		case "last":
			handleLast(worklistId, request, response);
			break;			
		default:
			Logger.warn(worklistId + "   " + target + "    ["    + name + "]  [" + next + "]");
			throw new RuntimeException("unknown method on worklist");
		}

	}

	private void handleRoot(String worklistId, Request request, HttpServletResponse response) throws IOException {
		Worklist worklist = worklistStore.getWorklistByd(worklistId);
		if(worklist == null) {
			throw new RuntimeException("worklist not found");
		}
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();

		json.key("size");
		json.value(worklist.size());

		json.endObject();
	}

	private void handleFirst(String worklistId, Request request, HttpServletResponse response) throws IOException {
		Worklist worklist = worklistStore.getWorklistByd(worklistId);
		if(worklist == null) {
			response.setStatus(HttpStatus.NOT_FOUND_404);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("error");
			json.value("worklist not found");
			json.endObject();
			return;
		}
		
		int first = Web.getInt(request, "first", Integer.MIN_VALUE);
		boolean skipDone = Web.getBoolean(request, "skip_done", false);
		WorklistEntry worklistEntry = null;
		if(skipDone) {
			worklistEntry = worklist.find(first, SKIP_DONE_PREDICATE);			
		} else {
			worklistEntry = worklist.find(first, TRUE_PREDICATE);	
		}

		if(worklistEntry == null) {
			response.setStatus(HttpStatus.NOT_FOUND_404);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("error");
			json.value("end of list reached");
			json.endObject();
			return;
		}
		
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();

		json.key("index");
		json.value(worklistEntry.index);
		json.key("sample");
		json.value(worklistEntry.sample);
		json.key("start");
		json.value(worklistEntry.start);
		json.key("end");
		json.value(worklistEntry.end);
		json.key("title");
		json.value(worklistEntry.title);

		json.endObject();
	}
	
	private void handleLast(String worklistId, Request request, HttpServletResponse response) throws IOException {
		Worklist worklist = worklistStore.getWorklistByd(worklistId);
		if(worklist == null) {
			response.setStatus(HttpStatus.NOT_FOUND_404);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("error");
			json.value("worklist not found");
			json.endObject();
			return;
		}
		
		int last = Web.getInt(request, "last", Integer.MAX_VALUE);		
		boolean skipDone = Web.getBoolean(request, "skip_done", false);
		WorklistEntry worklistEntry = null;
		if(skipDone) {
			worklistEntry = worklist.findLast(last, SKIP_DONE_PREDICATE);			
		} else {
			worklistEntry = worklist.findLast(last, TRUE_PREDICATE);	
		}

		if(worklistEntry == null) {
			response.setStatus(HttpStatus.NOT_FOUND_404);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("error");
			json.value("beginning of list reached");
			json.endObject();
			return;
		}
		
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();

		json.key("index");
		json.value(worklistEntry.index);
		json.key("sample");
		json.value(worklistEntry.sample);
		json.key("start");
		json.value(worklistEntry.start);
		json.key("end");
		json.value(worklistEntry.end);
		json.key("title");
		json.value(worklistEntry.title);		

		json.endObject();
	}
}
