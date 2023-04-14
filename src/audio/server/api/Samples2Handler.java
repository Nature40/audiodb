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
import audio.SampleManager;
import audio.SampleManagerConnector;
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
				response.setContentType(Web.MIME_TEXT);
				response.getWriter().println("ERROR: " + e.getMessage());
			} catch(Exception e1) {
				Logger.warn(e1);
			}
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		Logger.info("query samples");
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();

		SampleManagerConnector conn = broker.sampleManager().tlSampleManagerConnector.get();

		String location = request.getParameter("location");
		long start = Web.getLong(request, "start", Long.MIN_VALUE);
		long end = Web.getLong(request, "end", Long.MIN_VALUE);
		boolean timerange = end != Long.MIN_VALUE;
		{ // local scope for timestamp
			long timestamp = Web.getLong(request, "timestamp", Long.MIN_VALUE);
			if(timestamp != Long.MIN_VALUE) {
				start = timestamp;
				end = timestamp;
				timerange = true;
			}
		}
		int limit = Web.getInt(request, "limit", Integer.MAX_VALUE);
		int offset = Web.getInt(request, "offset", 0);
		boolean flagCount = Web.getFlagBoolean(request, "count");
		boolean flagDevices = Web.getFlagBoolean(request, "devices");
		boolean flagSamples = Web.getFlagBoolean(request, "samples");

		Consumer<Sample2> sampleWriter = sample -> {
			json.object();
			json.key("id");
			json.value(sample.id);
			if(sample.hasLocation()) {
				json.key("location");
				json.value(sample.location);
			} else if(SampleManager.UNKNOWN_LOCATION_AS_DEVICE && sample.hasDevice()) { // not needed as location as devices already in db
				json.key("location");
				json.value("(device) " + sample.device);
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

		Consumer<String> deviceWriter = device -> {
			json.value(device);				
		};

		if(location == null) { // all locations
			if(flagCount) {
				if(timerange) {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtTimerange(start, end);
					json.value(count);
				} else {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().count();
					json.value(count);
				}
			}
			if(flagDevices) {
				if(timerange) {
					json.key("devices");
					json.array();
					conn.forEachDeviceAtTimerange(start, end, deviceWriter);
					json.endArray();
				} else {
					json.key("devices");
					json.array();
					conn.forEachDevice(deviceWriter);
					json.endArray();
				}
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) { // no limit
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtTimerange(start, end, sampleWriter);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEach(sampleWriter);
						json.endArray();
					}
				} else { // page
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtTimerangePaged(start, end, sampleWriter, limit, offset);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachPaged(sampleWriter, limit, offset);
						json.endArray();
					}
				} // end page
			}
		} else { // one location
			if(location.equalsIgnoreCase("null")) { // convert missing location marker to null
				location = null;
			}
			if(flagCount) {
				if(timerange) {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocationAtTimerange(location, start, end);
					json.value(count);
				} else {
					json.key("count");
					int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocation(location);
					json.value(count);
				}
			}
			if(flagDevices) {
				if(timerange) {
					json.key("devices");
					json.array();
					conn.forEachDeviceAtLocationAtTimerange(location, start, end, deviceWriter);
					json.endArray();
				} else {
					json.key("devices");
					json.array();
					conn.forEachDeviceAtLocation(location, deviceWriter);
					json.endArray();
				}
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) {  // no limit
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtLocationAtTimerange(location, start, end, sampleWriter);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachAtLocation(location, sampleWriter);
						json.endArray();
					}
				} else {   // page
					if(timerange) {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachPagedAtLocationAtTimerange(location, start, end, sampleWriter, limit, offset);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						broker.sampleManager().forEachPagedAtLocation(location, sampleWriter, limit, offset);
						json.endArray();
					}
				}  // end page
			}
		} // end  one location

		json.endObject();
	}
}