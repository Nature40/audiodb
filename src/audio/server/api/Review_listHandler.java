package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import audio.Broker;
import audio.Label;
import audio.ReviewList;
import audio.ReviewListEntry;
import audio.ReviewedLabel;

public class Review_listHandler {
	static final Logger log = LogManager.getLogger();

	private final Broker broker;

	public Review_listHandler(Broker broker) {
		this.broker = broker;
	}

	public void handle(String review_list_id, String target, Request request, HttpServletResponse response) throws IOException {
		ReviewList reviewList = broker.reviewListManager().getThrow(review_list_id);
		if(target.equals("/")) {
			handleRoot(review_list_id, reviewList, request, response);
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name: "+target);
			}			
			String name = i < 0 ? target.substring(1) : target.substring(1, i);
			String next = i < 0 ? "/" : target.substring(i);
			switch(name) {
			default:
				throw new RuntimeException("no call");
			}			
		}		
	}

	private void handleRoot(String review_list_id, ReviewList reviewList, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRoot_GET(review_list_id, reviewList, request, response);
			break;
		case "POST":
			handleRoot_POST(review_list_id, reviewList, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	private void handleRoot_GET(String review_list_id, ReviewList reviewList, Request request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("review_list");
		json.object();
		json.key("id");
		json.value(review_list_id);
		json.key("entries");
		json.array();
		reviewList.forEach((ReviewListEntry entry) -> {
			json.object();
			json.key("sample_id");
			json.value(entry.sample_id);
			json.key("sample_start");
			json.value(entry.sample_start);
			json.key("sample_end");
			json.value(entry.sample_end);
			json.key("label_name");
			json.value(entry.label_name);
			json.endObject();
		});
		json.endArray();
		json.endObject();
		json.endObject();
	}
	
	private void handleRoot_POST(String review_list_id, ReviewList reviewList, Request request, HttpServletResponse response) throws IOException {		
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "set_reviewed_label": {
				// TODO
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}		
		
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("review_list");
		json.object();
		json.key("id");
		json.value(review_list_id);
		json.key("entries");
		json.array();
		reviewList.forEach((ReviewListEntry entry) -> {
			json.object();
			json.key("sample_id");
			json.value(entry.sample_id);
			json.key("sample_start");
			json.value(entry.sample_start);
			json.key("sample_end");
			json.value(entry.sample_end);
			json.key("label_name");
			json.value(entry.label_name);
			json.endObject();
		});
		json.endArray();
		json.endObject();
		json.endObject();
	}
}
