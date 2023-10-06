package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Broker;
import audio.Sample2;
import audio.SampleManager;
import audio.SampleStorage;
import audio.SampleStorageConnector;
import audio.SampleStorageConnector.StorageSample;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.AudioTimeUtil;
import util.Web;

public class Samples2Handler extends AbstractHandler {

	private final Broker broker;
	private final SampleStorage sampleStorage;

	private final Sample2Handler sampleHandler;

	public Samples2Handler(Broker broker) {
		this.broker = broker;
		this.sampleStorage = broker.sampleStorage();
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

		//SampleManagerConnector conn = broker.sampleManager().tlSampleManagerConnector.get();
		SampleStorageConnector sampleStorageConnector = sampleStorage.tlSampleStorageConnector.get();

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
		String reqTimeZone = Web.getString(request, "tz", "UTC");
		int timeZoneOffsetSeconds = AudioTimeUtil.getTimeZoneOffsetSeconds(reqTimeZone);

		Consumer<StorageSample> sampleWriter = sample -> {
			json.object();
			json.key("id");
			json.value(Integer.toString(sample.sampleId));
			json.key("location");
			json.value(sample.locationName);
			if(sample.timestamp != 0) {
				LocalDateTime dateTime = AudioTimeUtil.ofAudiotime(sample.timestamp, timeZoneOffsetSeconds);
				json.key("date");
				json.value(dateTime.toLocalDate());
				json.key("time");
				json.value(dateTime.toLocalTime());
			}
			json.key("device");
			json.value(sample.deviceName);
			json.key("folder");
			json.value(sample.folderName);
			json.key("file");
			json.value(sample.fileName);

			json.endObject();			
		};

		Consumer<String> deviceWriter = device -> {
			json.value(device);				
		};

		if(location == null) { // all locations
			if(flagCount) {
				if(timerange) {
					json.key("count");
					//int count = broker.sampleManager().tlSampleManagerConnector.get().countAtTimerange(start, end);
					int count = sampleStorageConnector.getSampleCount(start, end);
					json.value(count);
				} else {
					json.key("count");
					//int count = broker.sampleManager().tlSampleManagerConnector.get().count();
					int count = sampleStorageConnector.getSampleCount(Long.MIN_VALUE, Long.MAX_VALUE);
					json.value(count);
				}
			}
			if(flagDevices) {
				if(timerange) {
					throw new RuntimeException("not implemented");
					/*json.key("devices");
					json.array();
					conn.forEachDeviceAtTimerange(start, end, deviceWriter);
					json.endArray();*/
				} else {
					json.key("devices");
					json.array();
					//conn.forEachDevice(deviceWriter);
					sampleStorageConnector.forEachDevice(deviceWriter);
					json.endArray();
				}
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) { // no limit
					if(timerange) {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEachAtTimerange(start, end, sampleWriter);
						sampleStorageConnector.forEachOrderedSampleId(start, end, sampleId -> {
							StorageSample sample = sampleStorage.getStorageSample(sampleId);
							sampleWriter.accept(sample);
						}, Integer.MAX_VALUE, 0);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEach(sampleWriter);
						sampleStorageConnector.forEachOrderedSampleId(Long.MIN_VALUE, Long.MAX_VALUE, sampleId -> {
							StorageSample sample = sampleStorage.getStorageSample(sampleId);
							sampleWriter.accept(sample);
						}, Integer.MAX_VALUE, 0);
						json.endArray();
					}
				} else { // page
					if(timerange) {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEachAtTimerangePaged(start, end, sampleWriter, limit, offset);
						sampleStorageConnector.forEachOrderedSampleId(start, end, sampleId -> {
							StorageSample sample = sampleStorage.getStorageSample(sampleId);
							sampleWriter.accept(sample);
						}, limit, offset);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEachPaged(sampleWriter, limit, offset);
						sampleStorageConnector.forEachOrderedSampleId(Long.MIN_VALUE, Long.MAX_VALUE, sampleId -> {
							StorageSample sample = sampleStorage.getStorageSample(sampleId);
							sampleWriter.accept(sample);
						}, limit, offset);
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
					//int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocationAtTimerange(location, start, end);
					int count = sampleStorage.getSampleCountAtLocationName(start, end, location);
					json.value(count);
				} else {
					json.key("count");
					//int count = broker.sampleManager().tlSampleManagerConnector.get().countAtLocation(location);
					int count = sampleStorage.getSampleCountAtLocationName(Long.MIN_VALUE, Long.MAX_VALUE, location);
					json.value(count);
				}
			}
			if(flagDevices) {
				throw new RuntimeException("not implemented");
				/*if(timerange) {
					json.key("devices");
					json.array();
					conn.forEachDeviceAtLocationAtTimerange(location, start, end, deviceWriter);
					json.endArray();
				} else {
					json.key("devices");
					json.array();
					conn.forEachDeviceAtLocation(location, deviceWriter);
					json.endArray();
				}*/
			}
			if(flagSamples) {
				if(limit == Integer.MAX_VALUE && offset == 0) {  // no limit
					if(timerange) {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEachAtLocationAtTimerange(location, start, end, sampleWriter);
						sampleStorage.forEachOrderedSampleAtLocationName(start, end, location, sampleWriter, Integer.MAX_VALUE, 0);
						json.endArray();
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEachAtLocation(location, sampleWriter);
						sampleStorage.forEachOrderedSampleAtLocationName(Long.MIN_VALUE, Long.MAX_VALUE, location, sampleWriter, Integer.MAX_VALUE, 0);
						json.endArray();
					}
				} else {   // page
					if(timerange) {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEachPagedAtLocationAtTimerange(location, start, end, sampleWriter, limit, offset);
						sampleStorage.forEachOrderedSampleAtLocationName(start, end, location, sampleWriter, limit, offset);
						json.endArray();
					} else {
						json.key("samples");
						json.array();
						//broker.sampleManager().forEachPagedAtLocation(location, sampleWriter, limit, offset);
						sampleStorage.forEachOrderedSampleAtLocationName(Long.MIN_VALUE, Long.MAX_VALUE, location, sampleWriter, limit, offset);
						json.endArray();
					}
				}  // end page
			}
		} // end  one location

		json.endObject();
	}
}