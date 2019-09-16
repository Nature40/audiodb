package audio.server.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import audio.Account;
import audio.Label;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class LabelsHandler {

	private final Path samplesRoot = Paths.get("samples");

	public void handle(String sample, String target, Request request, HttpServletResponse response) throws IOException {
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

	private void handleRoot(String sample, Request request, HttpServletResponse response) throws IOException {
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

	private static Vec<Label> loadLabels(Path labelsPath) {
		if(!Files.exists(labelsPath)) {
			return new Vec<Label>();
		}
		YamlMap yamlMap = YamlUtil.readYamlMap(labelsPath);
		return YamlUtil.getVec(yamlMap, "labels", Label::ofYAML);
	}

	private void handleRoot_GET(String sample, Request request, HttpServletResponse response) throws IOException {
		Path samplePath = samplesRoot.resolve(sample);
		Path labelsPath = samplePath.resolve("labels.yaml");
		Vec<Label> labels = loadLabels(labelsPath);		
		JsonUtil.write(response, json -> JsonUtil.writeArray(json, "labels", labels, Label::toJSON));		
	}

	private void handleRoot_POST(String sample, Request request, HttpServletResponse response) throws IOException {
		Account account = (Account) request.getSession(false).getAttribute("account");
		Path samplePath = samplesRoot.resolve(sample);
		samplePath.toFile().mkdirs();
		Path labelsPath = samplePath.resolve("labels.yaml");
		Vec<Label> labels = loadLabels(labelsPath);
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "add_label": {				;
				Label label = Label.ofJSON(jsonAction.getJSONObject("label")).withCreator(account.username, LocalDateTime.now());				
				labels.add(label);
				break;
			}
			case "remove_label": {				;
			Label label = Label.ofJSON(jsonAction.getJSONObject("label"));				
			if(labels.removeIf(l->{
				return l.start == label.start && l.end == label.end;
			})) {
				// nothing
			} else {
				throw new RuntimeException("label to remove not found");
			}
			break;
		}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}
		
		YamlUtil.writeSafe(labelsPath, map -> YamlUtil.putArray(map, "labels", labels, Label::toMap));
		
		JsonUtil.write(response, json -> {
			json.key("massage");
			json.value("ok");
			json.key("labels");
			json.array();
			for(Label label:labels) {
				label.toJSON(json);
			}
			json.endArray();
		});
	}
}