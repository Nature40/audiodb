package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.yaml.snakeyaml.Yaml;

import audio.Account;
import audio.Broker;
import audio.Sample;
import audio.SampleUserLocked;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SampleHandler {
	static final Logger log = LogManager.getLogger();

	private final LabelsHandler labelsHandler;
	private final SpectrumHandler spectrumHandler;
	private final AudioHandler audioHandler;

	private final Broker broker;
	
	public SampleHandler(Broker broker) {
		this.broker = broker;
		labelsHandler = new LabelsHandler(broker);
		spectrumHandler = new SpectrumHandler(broker);
		audioHandler = new AudioHandler(broker);
	}

	public void handle(String sampleText, String target, Request request, HttpServletResponse response) throws IOException {
		Sample sample = broker.samples().getThrow(sampleText);
		if(target.equals("/")) {
			handleRoot(sample, request, response);
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name: "+target);
			}			
			String name = i < 0 ? target.substring(1) : target.substring(1, i);
			String next = i < 0 ? "/" : target.substring(i);
			switch(name) {
			case "labels":
				labelsHandler.handle(sample, next, request, response);
				break;
			case "spectrum":
				if(sample.isSampleUserLocked()) {
					return;
				}
				spectrumHandler.handle(sample, request, response);
				break;
			case "data":
				if(sample.isSampleUserLocked()) {
					return;
				}
				audioHandler.handle(sample, request, response);
				break;
			case "meta":
				handleMeta(sample, request, response);
				break;
			default:
				throw new RuntimeException("no call");
			}			
		}		
	}

	private void handleRoot(Sample sample, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "POST":
			handleRoot_POST(sample, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	private void handleRoot_POST(Sample sample, Request request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readOnly.checkHasNot(roleBits);

		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "set_locked": {					
				String username = account.username;
				long timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
				SampleUserLocked sampleUserLocked = new SampleUserLocked(username, timestamp);
				sample.setSampleUserLocked(sampleUserLocked);
				break;
			}
			default:
				throw new RuntimeException("unknown action:" + actionName);
			}
		}		

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("result");
		json.value("OK");
		json.endObject();	
	}




	

	




	private void handleMeta(Sample sample, Request request, HttpServletResponse response) throws IOException {
		//response.setContentType("text/yaml; charset=utf-8");
		response.setContentType("text/plain; charset=utf-8");

		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();

		LinkedHashMap<String, Object> yamlMapSample = new LinkedHashMap<String, Object>();
		yamlMap.put("sample", yamlMapSample);
		yamlMapSample.put("id", sample.id);
		yamlMapSample.put("directory", sample.directoryPath.toString());
		yamlMapSample.put("audio_file_name", sample.getAudioFileName());
		yamlMapSample.put("audio_file_size", sample.getAudioFile().length());

		yamlMap.put("meta", sample.getMetaMap().getInternalMap());

		new Yaml().dump(yamlMap, response.getWriter());

	}
}
