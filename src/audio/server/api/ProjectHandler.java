package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;

import audio.AudioProjectConfig;
import audio.Broker;
import audio.SampleManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.AudioTimeUtil;
import util.Web;

public class ProjectHandler {
	static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final SampleManager sampleManager;

	public ProjectHandler(Broker broker) {
		this.broker = broker;
		this.sampleManager = broker.sampleManager();
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
				throw new RuntimeException("unknown: "+target);
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

	private void handleRoot(String project, Request request, HttpServletResponse response) throws IOException {
		boolean fLocations = Web.getFlagBoolean(request, "locations");
		boolean fTimestamps = Web.getFlagBoolean(request, "timestamps");
		AudioProjectConfig config = broker.config().audioConfig;		
		if(!config.project.equals(project)) {
			throw new RuntimeException("unknown project");
		}		
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("project");		
		json.object();
		json.key("project");
		json.value(config.project);
		json.key("player_spectrum_threshold");
		json.value(config.player_spectrum_threshold);
		json.key("player_playbackRate");
		json.value(config.player_playbackRate);
		json.key("player_preservesPitch");
		json.value(config.player_preservesPitch);
		json.key("player_overwriteSamplingRate");
		json.value(config.player_overwriteSamplingRate);
		json.key("player_samplingRate");
		json.value(config.player_samplingRate);
		if(fLocations) {
			json.key("locations");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachLocation(location -> json.value(location));
			json.endArray();
		}
		if(fTimestamps) {
			json.key("timestamps");
			json.array();
			sampleManager.tlSampleManagerConnector.get().forEachTimestamp(timestamp -> {
				json.object();
				json.key("timestamp");
				json.value(timestamp);
				if(timestamp > 0) {
					LocalDateTime dateTime = AudioTimeUtil.ofAudiotime(timestamp);
					json.key("date");
					json.value(dateTime.toLocalDate());
					json.key("time");
					json.value(dateTime.toLocalTime());
				}				
				json.endObject();
			});
			json.endArray();
		}
		json.endObject(); // audio_config
		json.endObject(); // full JSON
	}
}