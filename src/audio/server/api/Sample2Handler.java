package audio.server.api;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;

import audio.Broker;
import audio.Sample2;
import audio.SampleManager;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class Sample2Handler {
	static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final SampleManager sampleManager;
	private final SpectrumHandler spectrumHandler;

	public Sample2Handler(Broker broker) {
		this.broker = broker;
		this.sampleManager = broker.sampleManager();
		this.spectrumHandler = new SpectrumHandler(broker);
	}

	public void handle(String sampleId, String target, Request request, HttpServletResponse response) throws IOException {
		Sample2 sample = sampleManager.getById(sampleId);
		if(sample == null) {
			throw new RuntimeException("sample not found");
		}
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
			case "spectrogram": {
				spectrumHandler.handle(sample, request, response);
				break;
			}
			default:
				throw new RuntimeException("no call");
			}			
		}		
	}

	private void handleRoot(Sample2 sample, Request request, HttpServletResponse response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRoot_GET(sample, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}

	private void handleRoot_GET(Sample2 sample, Request request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		
		boolean reqSamples = Web.getFlagBoolean(request, "samples");
		
		json.object();
		json.key("sample");		
		json.object();
		json.key("id");
		json.value(sample.id);
		json.key("project");
		json.value(sample.project);
		if(sample.hasLocation()) {
			json.key("location");
			json.value(sample.location);
		}
		if(sample.hasTimestamp()) {
			json.key("timestamp");
			json.value(sample.timestamp);
		}
		if(reqSamples && sample.hasSamples()) {
			json.key("Samples");
			json.value(sample.samples());
		}
		json.endObject();
		json.endObject();
	}
}
