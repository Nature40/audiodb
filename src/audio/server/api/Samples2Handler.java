package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.Sample2;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.AudioTimeUtil;
import util.Web;

public class Samples2Handler extends AbstractHandler {
	

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
			Logger.error(e);
			e.printStackTrace();
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: " + e.getMessage());
			} catch(Exception e1) {
				Logger.warn(e1);
			}
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		Logger.info("query samples");
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();


		String location = request.getParameter("location");
		long timestamp = Web.getLong(request, "timestamp", Long.MIN_VALUE);
		long start = Web.getLong(request, "start", Long.MIN_VALUE);
		long end = Web.getLong(request, "end", Long.MIN_VALUE);
		boolean timerange = end != Long.MIN_VALUE;
		int limit = Web.getInt(request, "limit", Integer.MAX_VALUE);
		int offset = Web.getInt(request, "offset", 0);
		boolean flagCount = Web.getFlagBoolean(request, "count");
		boolean flagSamples = Web.getFlagBoolean(request, "samples");
		
		Consumer<Sample2> sampleWriter = sample -> {
			json.object();
			json.key("id");
			json.value(sample.id);
			if(sample.hasLocation()) {
				json.key("location");
				json.value(sample.location);
			}
			if(sample.hasTimestamp()) {
				//json.key("timestamp");
				//json.value(sample.timestamp);
				//Logger.info(sample.timestamp);
				LocalDateTime dateTime = AudioTimeUtil.ofAudiotime(sample.timestamp);
				json.key("date");
				json.value(dateTime.toLocalDate());
				json.key("time");
				json.value(dateTime.toLocalTime());
			}
			if(sample.hasDevice()) {
				json.key("device");
				json.value(sample.device);
			}
			json.endObject();			
		};

		if(location == null) {
			if(flagCount) {
				if(timerange) {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtTimerange(start, end);
					json.value(count);
				} else if(timestamp == Long.MIN_VALUE) {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().count();
					json.value(count);
				} else {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtTimestamp(timestamp);
					json.value(count);
				}
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) {
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtTimerange(start, end, sampleWriter);
						json.endArray();
					} else if(timestamp == Long.MIN_VALUE) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEach(sampleWriter);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtTimestamp(timestamp, sampleWriter);
						json.endArray();	
					}
				} else {
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtTimerangePaged(start, end, sampleWriter, limit, offset);
						json.endArray();
					} else if(timestamp == Long.MIN_VALUE) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachPaged(sampleWriter, limit, offset);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtTimestampPaged(timestamp, sampleWriter, limit, offset);
						json.endArray();
					}
				}
			}
		} else {
			if(location.equalsIgnoreCase("null")) { // convert missing location marker to null
				location = null;
			}
			if(flagCount) {
				if(timerange) {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocationAtTimerange(location, start, end);
					json.value(count);
				} else if(timestamp == Long.MIN_VALUE) {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocation(location);
					json.value(count);
				} else {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocationAtTimestamp(location, timestamp);
					json.value(count);
				}
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) {
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtLocationAtTimerange(location, start, end, sampleWriter);
						json.endArray();
					} else if(timestamp == Long.MIN_VALUE) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtLocation(location, sampleWriter);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtLocationAtTimestamp(location, timestamp, sampleWriter);
						json.endArray();
					}
				} else {
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachPagedAtLocationAtTimerange(location, start, end, sampleWriter, limit, offset);
						json.endArray();
					} else if(timestamp == Long.MIN_VALUE) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachPagedAtLocation(location, sampleWriter, limit, offset);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachPagedAtLocationAtTimestamp(location, timestamp, sampleWriter, limit, offset);
						json.endArray();
					}
				}
			}
		}

		json.endObject();
	}
}