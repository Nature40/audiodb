package audio.server.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.yaml.snakeyaml.Yaml;

import audio.Account;
import audio.Broker;
import audio.Label;
import audio.ReviewListEntry;
import audio.ReviewedLabel;
import audio.Sample;
import audio.SampleUserLocked;
import audio.ReviewedLabel.Reviewed;
import util.JsonUtil;

public class SampleHandler {
	static final Logger log = LogManager.getLogger();

	private final LabelsHandler labelsHandler = new LabelsHandler();
	private final SpectrumHandler spectrumHandler;

	private final Broker broker;

	public SampleHandler(Broker broker) {
		this.broker = broker;
		spectrumHandler = new SpectrumHandler(broker);
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
				handleData(sample, request, response);
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
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "set_locked": {
				Account account = (Account) request.getSession(false).getAttribute("account");							
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

	private void handleData(Sample sample, Request request, HttpServletResponse response) throws IOException {
		File file = sample.getAudioFile();
		long fileLen = file.length();

		String rangeText = request.getHeader("Range");
		if(rangeText == null) {
			response.setContentType("audio/wave");
			response.setContentLengthLong(fileLen);
			try(FileInputStream in = new FileInputStream(file)) {
				IO.copy(in, response.getOutputStream());
			}
		} else {
			if(rangeText.startsWith("bytes=")) {
				String rangeIntervalText = rangeText.substring(6);
				//log.info("rangeIntervalText |" + rangeIntervalText + "|");
				if(rangeIntervalText.contains(",")) {
					throw new RuntimeException("unknown Range header, multiple ranges not supported: " + rangeText);
				}
				int rangeIntervalTextSeperatorIndex = rangeIntervalText.indexOf("-");
				if(rangeIntervalTextSeperatorIndex < 0) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				String rangeStartText = rangeIntervalText.substring(0, rangeIntervalTextSeperatorIndex);
				String rangeEndText = rangeIntervalText.substring(rangeIntervalTextSeperatorIndex + 1);
				//log.info("rangeIntervalText |" + rangeStartText + "|" + rangeEndText + "|");
				if(rangeStartText.isEmpty()) {
					throw new RuntimeException("unknown Range header, suffix-length not supported: " + rangeText);
				}
				long rangeStart = Long.parseLong(rangeStartText);
				if(rangeStart < 0) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				long rangeEnd = rangeEndText.isEmpty() ? (fileLen - 1) : Long.parseLong(rangeEndText);
				if(rangeEnd < rangeStart) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				if(rangeEnd >= fileLen) {
					throw new RuntimeException("unknown Range header: " + rangeText);
				}
				long rangeLen = rangeEnd - rangeStart + 1;
				try(FileInputStream in = new FileInputStream(file)) {
					if(rangeStart != 0) {					
						in.skip(rangeStart);
					}
					response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
					response.setContentType("audio/wave");
					response.setContentLengthLong(fileLen);
					response.setHeader("Content-Range", "bytes "+ rangeStart +"-" + rangeEnd + "/" + fileLen);
					IO.copy(in, response.getOutputStream(), rangeLen);
				}
			} else {
				throw new RuntimeException("unknown Range header: " + rangeText);
			}
		}


		/*if(rangeText == null) {
			byteCount = len;
		} else {
			log.info("rangeText " + rangeText);
			if(rangeText.equals("bytes=0-")) {
				log.info("full range ");
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				response.setHeader("Content-Range", "bytes 0-" + (len-1) + "/" + len );
			} else {
				throw new RuntimeException("partial ranges not implemented");
			}
		}*/
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

		yamlMap.put("meta", sample.getMetaMap().getRootMap());

		new Yaml().dump(yamlMap, response.getWriter());

	}
}
