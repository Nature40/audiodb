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
import audio.ReviewedLabel;
import audio.ReviewedLabel.Reviewed;

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

		HashMap<String, Integer> counterMap = new HashMap<String, Integer>();

		broker.samples().forEach(sample -> {
			/*sample.forEachReviewed(reviewedLabel -> {
				if(reviewedLabel.reviewed == Reviewed.yes) {
					String name = reviewedLabel.name;
				    Integer counter = counterMap.get(name);
				    counterMap.put(name, counter == null ? 1 : (counter + 1));
				}
			});*/
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
						Integer counter = counterMap.get(name);
						counterMap.put(name, counter == null ? 1 : (counter + 1));
						log.info("confirmed1 " + name + "    " + sample.id + "   " + label.start + "  " + label.end);
					}
					break;
				}
				default: {
					HashSet<String> names = new HashSet<String>();
					label.reviewedLabels.forEach(reviewedLabel -> {
						if(reviewedLabel.reviewed == Reviewed.yes) {
							names.add(reviewedLabel.name);
						}
					});
					for(String name : names) {
						Integer counter = counterMap.get(name);
						counterMap.put(name, counter == null ? 1 : (counter + 1));
						log.info("confirmedN " + name + "    " + sample.id + "   " + label.start + "  " + label.end);
					}
				}
				}

			});
		});

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("reviewed_yes_counts");
		json.object();
		counterMap.forEach((name, count) -> {
			json.key(name);
			json.value(count);
		});
		json.endObject();
		json.endObject();
	}
}