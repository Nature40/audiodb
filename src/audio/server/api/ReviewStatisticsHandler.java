package audio.server.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.review.ReviewedLabel;
import audio.review.ReviewedLabel.Reviewed;

public class ReviewStatisticsHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	private final SampleHandler sampleHandler;

	private final Broker broker;

	public ReviewStatisticsHandler(Broker broker) {
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

		HashMap<String, Integer> yesCounterMap = new HashMap<String, Integer>();
		HashMap<String, Integer> openCounterMap = new HashMap<String, Integer>();

		broker.samples().forEach(sample -> {
			sample.forEachLabel(label -> {
				switch(label.reviewedLabels.size()) {
				case 0: {
					//nothing
					break;
				}
				case 1: {
					ReviewedLabel reviewedLabel = label.reviewedLabels.get(0);
					if(reviewedLabel.reviewed == Reviewed.yes) {
						String name = reviewedLabel.name;
						Integer counter = yesCounterMap.get(name);
						yesCounterMap.put(name, counter == null ? 1 : (counter + 1));
						log.info("confirmed1 " + name + "    " + sample.id + "   " + label.start + "  " + label.end);
					}
					break;
				}
				default: {
					HashSet<String> yesNames = new HashSet<String>();
					label.reviewedLabels.forEach(reviewedLabel -> {
						if(reviewedLabel.reviewed == Reviewed.yes) {
							yesNames.add(reviewedLabel.name);
						}
					});
					for(String name : yesNames) {
						Integer counter = yesCounterMap.get(name);
						yesCounterMap.put(name, counter == null ? 1 : (counter + 1));
						log.info("confirmedN " + name + "    " + sample.id + "   " + label.start + "  " + label.end);
					}
				}
				}

			});
		});
		
		broker.reviewListManager().forEach((name, reviewList) -> {
			int[] count = new int[] {0};
			reviewList.forEach(entry -> {
				if(!entry.classified) {
					count[0]++;
				}
			});
			int cnt = count[0];
			if(cnt > 0) {
				openCounterMap.put(name, cnt);
			}
		});

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		
		json.key("reviewed_yes_counts");
		json.object();
		yesCounterMap.forEach((name, count) -> {
			json.key(name);
			json.value(count);
		});
		json.endObject();
		
		json.key("open_counts");
		json.object();
		openCounterMap.forEach((name, count) -> {
			json.key(name);
			json.value(count);
		});
		json.endObject();
		
		json.endObject();
	}
}