package audio.server.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import audio.Account;
import audio.Broker;
import audio.Label;
import audio.Sample;
import audio.review.ReviewedLabel;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class LabelsHandler {

	private static final Path samplesRoot = Paths.get("samples");
	
	private final Broker broker;

	public LabelsHandler(Broker broker) {
		this.broker = broker;
	}

	public void handle(Sample sample, String target, Request request, HttpServletResponse response) throws IOException {
		request.setHandled(true);
		if(target.equals("/")) {
			handleRoot(sample, request, response);
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name: "+target);
			}			
			String name = i < 0 ? target.substring(1) : target.substring(1, i);
			String next = i < 0 ? "/" : target.substring(i);
			throw new RuntimeException("no call");
		}		
	}

	private void handleRoot(Sample sample, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRoot_GET(sample, request, response);
			break;
		case "POST":
			handleRoot_POST(sample, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	private static Path getSamplePath(Sample sample) {
		return samplesRoot.resolve(sample.id);
	}

	private static Path getLabelsPath(Sample sample) {
		Path samplePath = getSamplePath(sample);
		return samplePath.resolve("labels.yaml");
	}

	public static Vec<Label> loadLabels(Sample sample) {
		Path labelsPath = getLabelsPath(sample);
		return loadLabels(labelsPath);		
	}

	private static Vec<Label> loadLabels(Path labelsPath) {
		if(!Files.exists(labelsPath)) {
			return new Vec<Label>();
		}
		YamlMap yamlMap = YamlUtil.readYamlMap(labelsPath);
		return YamlUtil.getVec(yamlMap, "labels", Label::ofYAML);
	}

	private void handleRoot_GET(Sample sample, Request request, HttpServletResponse response) throws IOException {
		sample.getLabels().sort(Label.INTERVAL_COMPARATOR);
		JsonUtil.write(response, json -> JsonUtil.writeArray(json, "labels", sample.getLabels(), Label::toJSON));		
	}

	private void handleRoot_POST(Sample sample, Request request, HttpServletResponse response) throws IOException {		
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readOnly.checkHasNot(roleBits);
		
		
		/*Path samplePath = samplesRoot.resolve(sample.id);
		samplePath.toFile().mkdirs();
		Path labelsPath = samplePath.resolve("labels.yaml");
		Vec<Label> labels = loadLabels(labelsPath);*/
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "add_label": {
				Label label = Label.ofJSON(jsonAction.getJSONObject("label")).withCreator(account.username, LocalDateTime.now().toString());				
				sample.getLabels().add(label);
				break;
			}
			case "remove_label": {
				Label label = Label.ofJSON(jsonAction.getJSONObject("label"));				
				if(sample.getLabels().removeIf(l->{
					return l.start == label.start && l.end == label.end;
				})) {
					// nothing
				} else {
					throw new RuntimeException("label to remove not found");
				}
				break;
			}
			case "replace_label": {
				Label label = Label.ofJSON(jsonAction.getJSONObject("label"));				
				int labelIndex = sample.getLabels().findIndexOf(l -> l.start == label.start && l.end == label.end);
				if(labelIndex < 0) {
					throw new RuntimeException("label to replace not found");
				} else {
					sample.getLabels().setFast(labelIndex, label);
				}
				break;
			}
			case "set_reviewed_label": {
				double start = jsonAction.getDouble("start");
				double end = jsonAction.getDouble("end");
				int labelIndex = sample.getLabels().findIndexOf(l -> l.start == start && l.end == end);
				Label label = sample.getLabels().get(labelIndex);
				
				String reviewer = account.username;
				long timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
				ReviewedLabel reviewedLabel = ReviewedLabel.ofJSON(jsonAction.getJSONObject("reviewed_label")).withReviewer(reviewer, timestamp);				
				label.setReviewedLabel(reviewedLabel);
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}
		
		sample.getLabels().sort(Label.INTERVAL_COMPARATOR);

		sample.writeToFile();		

		JsonUtil.write(response, json -> {
			json.key("massage");
			json.value("ok");
			json.key("labels");
			json.array();
			for(Label label:sample.getLabels()) {
				label.toJSON(json);
			}
			json.endArray();
		});
	}
}