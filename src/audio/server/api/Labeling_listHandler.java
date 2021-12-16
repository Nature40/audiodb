package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import audio.Account;
import audio.Broker;
import audio.Label;
import audio.review.ReviewList;
import audio.review.ReviewListEntry;
import audio.review.ReviewedLabel;
import audio.review.ReviewedLabel.Reviewed;
import audio.Sample;
import audio.UserLabel;
import audio.labeling.LabelingList;
import audio.labeling.LabelingListEntry;
import util.JsonUtil;
import util.collections.vec.Vec;

public class Labeling_listHandler {
	

	private final Broker broker;

	public Labeling_listHandler(Broker broker) {
		this.broker = broker;
	}

	public void handle(String labeling_list_id, String target, Request request, HttpServletResponse response) throws IOException {
		LabelingList labelingList = broker.labelingListManager().getThrow(labeling_list_id);
		if(target.equals("/")) {
			handleRoot(labeling_list_id, labelingList, request, response);
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

	private void handleRoot(String labeling_list_id, LabelingList labelingList, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRoot_GET(labeling_list_id, labelingList, request, response);
			break;
		case "POST":
			handleRoot_POST(labeling_list_id, labelingList, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	private void handleRoot_GET(String labeling_list_id, LabelingList labelingList, Request request, HttpServletResponse response) throws IOException {
		//HttpSession session = request.getSession(false);
		sendLabeling_list(labeling_list_id, labelingList, response);
	}

	private void handleRoot_POST(String review_list_id, LabelingList labelingList, Request request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");
		
		Sample[] responseSample = new Sample[] {null};

		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "set_labeling": {
				String req_sample_id = jsonAction.getString("sample_id");
				double req_label_start = jsonAction.getDouble("label_start");					
				double req_label_end = jsonAction.getDouble("label_end");
				String[] req_label_names = JsonUtil.optStrings(jsonAction, "label_names");
				labelingList.mutate(entries -> {
					Predicate<LabelingListEntry> keyFunc = LabelingListEntry.getKeyFunc(req_sample_id, req_label_start, req_label_end);
					int labeling_index = entries.findIndexOfUnsync(keyFunc);
					if(labeling_index < 0) {
						throw new RuntimeException("labeleling entry not found");
					}
					LabelingListEntry entry = entries.getUnsync(labeling_index);
					Sample s = broker.samples().sampleMap.get(entry.sample_id);
					if(s == null) {
						throw new RuntimeException("sample not found: " + entry.sample_id);
					}
					s.mutate(sample -> {
						int sample_label_index = sample.findLabelIndexOf(entry.label_start, entry.label_end);
						Label label = sample_label_index < 0 ? new Label(entry.label_start, entry.label_end) : sample.getLabel(sample_label_index);
						String username = account.username;
						String timestamp = LocalDateTime.now().toString();
						Vec<UserLabel> userLabels = new Vec<UserLabel>();
						for(String label_name : req_label_names) {
							UserLabel userLabel = label.userLabels.find((UserLabel us) -> label_name.equals(us.name));
							if(userLabel == null) {
								userLabel = new UserLabel(label_name, username, timestamp);
							}
							userLabels.add(userLabel);
						}						
						label.setUserLabels(userLabels);
						if(sample_label_index >= 0) {
							sample.setLabel(sample_label_index, label);
						} else {
							Logger.info("add new label " + label);
							sample.addLabel(label);
						}
					});					
					entries.setUnsync(labeling_index, entry.withLabeled(true));
					responseSample[0] = s;
				});
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}		

		sendLabelingListAndSampleLabels(review_list_id, labelingList, responseSample[0], response);
	}
	
	public void sendLabelingListAndSampleLabels(String labeling_list_id, LabelingList labelingList, Sample sample, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		writeLabeling_list(labeling_list_id, labelingList, json);
		if(sample != null) {			
			writeSampleLabels(sample, json);
		}
		json.endObject();
	}

	public void sendLabeling_list(String labeling_list_id, LabelingList labelingList, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		writeLabeling_list(labeling_list_id, labelingList, json);
		json.endObject();
	}
	
	public void writeLabeling_list(String labeling_list_id, LabelingList labelingList, JSONWriter json) {
		json.key("labeling_list");
		json.object();
		json.key("id");
		json.value(labeling_list_id);
		json.key("entries");
		json.array();
		labelingList.forEach((LabelingListEntry entry) -> {
				json.object();
				json.key("sample_id");
				json.value(entry.sample_id);
				json.key("label_start");
				json.value(entry.label_start);
				json.key("label_end");
				json.value(entry.label_end);
				json.key("labeled");
				json.value(entry.labeled);			
				json.endObject();
		});
		json.endArray();
		json.endObject();
	}
	
	public void writeSampleLabels(Sample sample, JSONWriter json) {
		json.key("sample_labels");
		json.object();
		json.key("sample_id");
		json.value(sample.id);
		LabelsHandler.writeLabels(sample, json);
		json.endObject();
	}
}
