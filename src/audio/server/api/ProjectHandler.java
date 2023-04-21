package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.LongConsumer;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;

import audio.AudioProjectConfig;
import audio.Broker;
import audio.DeviceInventory;
import audio.SampleManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.AudioTimeUtil;
import util.Web;

public class ProjectHandler {


	private final Broker broker;
	private final SampleManager sampleManager;
	private final LabelDefinitionsHandler labelDefinitionsHandler;

	public ProjectHandler(Broker broker) {
		this.broker = broker;
		this.sampleManager = broker.sampleManager();
		this.labelDefinitionsHandler = new LabelDefinitionsHandler(broker);
	}

	public void handle(String project, String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				handleRoot(project, baseRequest, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				switch(name) {
				case "label_definitions": {
					labelDefinitionsHandler.handle(next, baseRequest, request, response);
					break;
				}
				default:
					throw new RuntimeException("unknown: "+target);
				}
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

	private void handleRoot(String project, Request request, HttpServletResponse response) throws IOException {
		boolean fLocations = Web.getFlagBoolean(request, "locations");
		boolean fDevices = Web.getFlagBoolean(request, "devices");
		boolean fTimestamps = Web.getFlagBoolean(request, "timestamps");
		boolean fDates = Web.getFlagBoolean(request, "dates");
		String timestamps_of_location = Web.getString(request, "timestamps_of_location", null);
		String dates_of_location = Web.getString(request, "dates_of_location", null);
		boolean fInventory = Web.getFlagBoolean(request, "inventory");
		boolean fSamplesTableCount = Web.getFlagBoolean(request, "samples_table_count");
		String reqTimeZone = Web.getString(request, "tz", "UTC");
		int timeZoneOffsetSeconds = AudioTimeUtil.getTimeZoneOffsetSeconds(reqTimeZone);
		Logger.info(reqTimeZone + " --> " + timeZoneOffsetSeconds);
		
		AudioProjectConfig config = broker.config().audioConfig;		
		if(!config.project.equals(project)) {
			throw new RuntimeException("unknown project");
		}		
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("project");		
		json.object();
		json.key("project");
		json.value(config.project);
		json.key("player_spectrum_threshold");
		json.value(config.player_spectrum_threshold);
		json.key("player_fft_intensity_max");
		json.value(config.player_fft_intensity_max);
		json.key("player_playbackRate");
		json.value(config.player_playbackRate);
		json.key("player_preservesPitch");
		json.value(config.player_preservesPitch);
		json.key("player_overwriteSamplingRate");
		json.value(config.player_overwriteSamplingRate);
		json.key("player_samplingRate");
		json.value(config.player_samplingRate);
		json.key("player_fft_window");
		json.value(config.player_fft_window);
		json.key("player_fft_window_step_factor");
		json.value(config.player_fft_window_step_factor);
		json.key("player_spectrum_shrink_Factor");
		json.value(config.player_spectrum_shrink_Factor);		
		json.key("player_time_expansion_factor");
		json.value(config.player_time_expansion_factor);
		if(config.player_static_lines_frequency != null) {
			json.key("player_static_lines_frequency");
			json.value(config.player_static_lines_frequency);
		}
		json.key("player_fft_cutoff_lower_frequency");
		json.value(config.player_fft_cutoff_lower_frequency);
		json.key("player_fft_cutoff_upper_frequency");
		json.value(config.player_fft_cutoff_upper_frequency);
		json.key("player_mouse_move_factor");
		json.value(config.player_mouse_move_factor);
		json.key("detail_fft_window_overlap_percent");
		json.value(config.detail_fft_window_overlap_percent);
		json.key("time_zone");
		json.value(config.time_zone);		
		
		if(config.hasProfiles()) {
			json.key("profiles");
			json.object();
			config.forEachProfile((id, p) -> {
				json.key(id);
				json.object();
				json.key("player_spectrum_threshold");
				json.value(p.player_spectrum_threshold);
				json.key("player_fft_intensity_max");
				json.value(p.player_fft_intensity_max);
				json.key("player_playbackRate");
				json.value(p.player_playbackRate);
				json.key("player_preservesPitch");
				json.value(p.player_preservesPitch);
				json.key("player_overwriteSamplingRate");
				json.value(p.player_overwriteSamplingRate);
				json.key("player_samplingRate");
				json.value(p.player_samplingRate);
				json.key("player_fft_window");
				json.value(p.player_fft_window);
				json.key("player_fft_window_step_factor");
				json.value(p.player_fft_window_step_factor);
				json.key("player_spectrum_shrink_Factor");
				json.value(p.player_spectrum_shrink_Factor);	
				json.key("player_time_expansion_factor");
				json.value(p.player_time_expansion_factor);
				if(p.player_static_lines_frequency != null) {
					json.key("player_static_lines_frequency");
					json.value(p.player_static_lines_frequency);
				}
				json.key("player_fft_cutoff_lower_frequency");
				json.value(p.player_fft_cutoff_lower_frequency);
				json.key("player_fft_cutoff_upper_frequency");
				json.value(p.player_fft_cutoff_upper_frequency);
				json.key("player_mouse_move_factor");
				json.value(p.player_mouse_move_factor);
				json.key("detail_fft_window_overlap_percent");
				json.value(p.detail_fft_window_overlap_percent);				
				json.endObject();
			});
			json.endObject();
		}
		
		if(fLocations) {
			json.key("locations");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachLocation(location -> json.value(location));
			json.endArray();
		}
		if(fDevices) {
			json.key("devices");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachDevice(device -> json.value(device));
			json.endArray();
		}		
		if(fSamplesTableCount) {
			int samples_table_count = sampleManager.tlSampleManagerConnector.get().getTableSize();
			json.key("samples_table_count");
			json.value(samples_table_count);
		}

		LongConsumer timestampDateTimeWriter = AudioTimeUtil.timestampDateTimeWriter(json, timeZoneOffsetSeconds); 		
		LongConsumer timestampDateWriter = AudioTimeUtil.timestampDateWriter(json, timeZoneOffsetSeconds);

		if(fTimestamps) {
			json.key("timestamps");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachTimestamp(timestampDateTimeWriter);
			json.endArray();
		}
		if(fDates) {
			json.key("dates");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachZonedDate(timeZoneOffsetSeconds, timestampDateWriter);
			json.endArray();
		}
		if(timestamps_of_location != null) {
			String loc = timestamps_of_location.equals("null") ? null : timestamps_of_location;
			json.key("timestamps_of_location");
			json.object();
			json.key("location");
			json.value(loc);
			json.key("timestamps");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachTimestamp(loc, timestampDateTimeWriter);
			json.endArray();
			json.endObject();
		}
		if(dates_of_location != null) {
			String loc = dates_of_location.equals("null") ? null : dates_of_location;
			json.key("dates_of_location");
			json.object();
			json.key("location");
			json.value(loc);
			json.key("timestamps");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachZonedDate(loc, timeZoneOffsetSeconds, timestampDateWriter);
			json.endArray();
			json.endObject();
		}
		if(fInventory) {
			json.key("inventory");
			json.array();
			DeviceInventory di = sampleManager.deviceInventory;
			di.forEach(entry -> {
				json.object();
				json.key("device");
				json.value(entry.device);
				json.key("location");
				json.value(entry.location);
				if(entry.start != Long.MIN_VALUE) {
					json.key("start");
					AudioTimeUtil.writeTimestampDateTime(json, entry.start, timeZoneOffsetSeconds);					
				}
				if(entry.end != Long.MAX_VALUE) {
					json.key("end");
					AudioTimeUtil.writeTimestampDateTime(json, entry.end, timeZoneOffsetSeconds);	
				}
				json.endObject();
			});
			json.endArray();
		}
		json.endObject(); // audio_config
		json.endObject(); // full JSON
	}
}