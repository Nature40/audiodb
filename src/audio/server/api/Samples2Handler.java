package audio.server.api;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class Samples2Handler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	private final Broker broker;

	private final Sample2Handler sampleHandler;

	public Samples2Handler(Broker broker) {
		this.broker = broker;
		sampleHandler = new Sample2Handler(broker);
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
			log.error(e);
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: " + e.getMessage());
			} catch(Exception e1) {
				log.warn(e1);
			}
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		log.info("query samples");
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();


		String location = request.getParameter("location");
		int limit = Web.getInt(request, "limit", Integer.MAX_VALUE);
		int offset = Web.getInt(request, "offset", 0);
		boolean flagCount = Web.getFlagBoolean(request, "count");
		boolean flagSamples = Web.getFlagBoolean(request, "samples");

		if(location == null) {
			if(flagCount) {
				json.key("count");
				int count = broker.sampleManager().tlSampleManagerConnector.get().count();
				json.value(count);
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) {
					json.key("samples");
					json.array();
					broker.sampleManager().forEach(sample -> {
						json.object();
						json.key("id");
						json.value(sample.id);
						if(sample.hasLocation()) {
							json.key("location");
							json.value(sample.location);
						}
						if(sample.hasTimestamp()) {
							json.key("timestamp");
							json.value(sample.timestamp);
						}
						json.endObject();
					});
					json.endArray();
				} else {
					json.key("samples");
					json.array();
					broker.sampleManager().forEachPaged(sample -> {					
						json.object();
						json.key("id");
						json.value(sample.id);
						if(sample.hasLocation()) {
							json.key("location");
							json.value(sample.location);
						}
						if(sample.hasTimestamp()) {
							json.key("timestamp");
							json.value(sample.timestamp);
						}
						json.endObject();
					}, limit, offset);
					json.endArray();
				}
			}
		} else {
			if(location.equalsIgnoreCase("null")) { // convert missing location marker to null
				location = null;
			}
			if(flagCount) {
				json.key("count");
				int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocation(location);
				json.value(count);
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) {
					json.key("samples");
					json.array();
					broker.sampleManager().forEachAtLocation(location, sample -> {
						json.object();
						json.key("id");
						json.value(sample.id);
						if(sample.hasLocation()) {
							json.key("location");
							json.value(sample.location);
						}
						if(sample.hasTimestamp()) {
							json.key("timestamp");
							json.value(sample.timestamp);
						}
						json.endObject();
					});
					json.endArray();
				} else {
					json.key("samples");
					json.array();
					broker.sampleManager().forEachPagedAtLocation(location, sample -> {
						json.object();
						json.key("id");
						json.value(sample.id);
						if(sample.hasLocation()) {
							json.key("location");
							json.value(sample.location);
						}
						if(sample.hasTimestamp()) {
							json.key("timestamp");
							json.value(sample.timestamp);
						}
						json.endObject();
					}, limit, offset);
					json.endArray();
				}
			}
		}

		json.endObject();
	}
}