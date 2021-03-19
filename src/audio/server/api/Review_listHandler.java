package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import audio.Account;
import audio.Broker;
import audio.Label;
import audio.ReviewList;
import audio.ReviewListEntry;
import audio.ReviewedLabel;
import audio.ReviewedLabel.Reviewed;
import audio.Sample;
import util.JsonUtil;

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
		sendReview_list(review_list_id, reviewList, response);
	}

	private void handleRoot_POST(String review_list_id, ReviewList reviewList, Request request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readonly.checkHasNot(roleBits);
		
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "set_reviewed_label": {
				String req_sample_id = jsonAction.getString("sample_id");
				double req_label_start = jsonAction.getDouble("label_start");					
				double req_label_end = jsonAction.getDouble("label_end");
				String req_label_name = jsonAction.getString("label_name");
				Reviewed reviewed = JsonUtil.getString(jsonAction, "reviewed", Reviewed::parse);
				reviewList.mutate(entries -> {
					Predicate<ReviewListEntry> keyFunc = ReviewListEntry.getKeyFunc(req_sample_id, req_label_name, req_label_start, req_label_end);
					int label_index = entries.findIndexOfUnsync(keyFunc);
					if(label_index < 0) {
						throw new RuntimeException("label not found");
					}
					ReviewListEntry entry = entries.getUnsync(label_index);
					Sample s = broker.samples().sampleMap.get(entry.sample_id);
					if(s == null) {
						throw new RuntimeException("sample not found: " + entry.sample_id);
					}						
					s.mutate(sample -> {
						int sample_label_index = sample.findLabelIndexOf(entry.label_start, entry.label_end);
						Label label = sample_label_index < 0 ? new Label(entry.label_start, entry.label_end) : sample.getLabel(sample_label_index);					
						String reviewer = account.username;
						long timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
						ReviewedLabel reviewedLabel = new ReviewedLabel(entry.label_name, reviewed, reviewer, timestamp);							
						label.addReview(reviewedLabel);
						if(sample_label_index >= 0) {
							sample.setLabel(sample_label_index, label);
						} else {
							log.info("add new label " + label);
							sample.addLabel(label);
						}
					});					
					entries.setUnsync(label_index, entry.withClassified(true));				
				});
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}		

		sendReview_list(review_list_id, reviewList, response);
	}

	void sendReview_list(String review_list_id, ReviewList reviewList, HttpServletResponse response) throws IOException {
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
			json.key("label_start");
			json.value(entry.label_start);
			json.key("label_end");
			json.value(entry.label_end);
			json.key("label_name");
			json.value(entry.label_name);
			json.key("classified");
			json.value(entry.classified);			
			json.endObject();
		});
		json.endArray();
		json.endObject();
		json.endObject();
	}
}
