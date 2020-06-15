package audio.server.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.yaml.snakeyaml.Yaml;

import audio.Broker;
import audio.Sample;

public class SampleHandler {

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
			handleRoot(request, response);
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
				spectrumHandler.handle(sample, request, response);
				break;
			case "data":
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

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		throw new RuntimeException("no call");
	}

	private void handleData(Sample sample, Request request, HttpServletResponse response) throws IOException {
		response.setContentType("audio/wav");
		File file = sample.path.toFile();
		response.setContentLengthLong(file.length());
		try(FileInputStream in = new FileInputStream(file)) {
			IO.copy(in, response.getOutputStream());
		}
	}
	
	private void handleMeta(Sample sample, Request request, HttpServletResponse response) throws IOException {
		//response.setContentType("text/yaml; charset=utf-8");
		response.setContentType("text/plain; charset=utf-8");
		
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		yamlMap.put("id", sample.id);
		yamlMap.put("audio_file_name", sample.fileName().toString());
		yamlMap.put("audio_file_path", sample.filePath().toString());
		yamlMap.put("audio_file_size", sample.file().length());
		new Yaml().dump(yamlMap, response.getWriter());

	}

}
