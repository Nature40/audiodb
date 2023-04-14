package audio.server.api;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.Sample;

public class SamplesHandler extends AbstractHandler {
	

	private final SampleHandler sampleHandler;

	private final Broker broker;

	public SamplesHandler(Broker broker) {
		this.broker = broker;
		sampleHandler = new SampleHandler(broker);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				handleRoot(baseRequest, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				sampleHandler.handle(name, next, baseRequest, response);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType(Web.MIME_TEXT);
				response.getWriter().println("ERROR: " + e.getMessage());
			} catch(Exception e1) {
				Logger.warn(e1);
			}
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("samples");
		json.array();
		Path root = Paths.get("data");		
		/*for(Path path:Webserver.getPaths(root)) {
			if(path.toFile().isFile()) {
				json.object();
				json.key("name");
				json.value(root.relativize(path).toString());
				json.endObject();
			}
		}*/
		/*ArrayList<Path> paths = Webserver.getAudioPaths(root, null);
		for(Path path:paths) {
			json.object();
			json.key("name");
			json.value(root.relativize(path).toString());
			json.endObject();
		}*/
		for(Sample sample:broker.samples().sampleMap.values()) {
			json.object();
			json.key("id");
			json.value(sample.id);
			json.key("location");
			json.value(sample.getMetaMap().optString("location",""));
			json.key("timestamp");
			long timestamp = sample.getMetaMap().optLong("timestamp", 0);		
			json.value(timestamp);
			json.endObject();
		}
		json.endArray();
		json.endObject();
	}
}