package audio.server.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.collections.vec.Vec;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.review.ReviewedLabel;
import audio.review.ReviewedLabel.Reviewed;

public class ReviewStatisticsHandler extends AbstractHandler {
	

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
			Logger.error(e);
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

		HashMap<String, Integer> yesCounterMap = new HashMap<String, Integer>();
		HashMap<String, Integer> unsureCounterMap = new HashMap<String, Integer>();
		HashMap<String, Integer> noCounterMap = new HashMap<String, Integer>();
		HashMap<String, Integer> reviewListOpenCounterMap = new HashMap<String, Integer>();
		HashMap<String, Integer> reviewListUnsureCounterMap = new HashMap<String, Integer>();

		broker.samples().forEach(sample -> {
			sample.forEachLabel(label -> {
				Vec<ReviewedLabel> reviewedLabels = label.reviewedLabels;
				switch(reviewedLabels.size()) {
				case 0: {
					//nothing
					break;
				}
				case 1: {
					ReviewedLabel reviewedLabel = reviewedLabels.get(0);
					Reviewed reviewed = reviewedLabel.reviewed;
					String name = reviewedLabel.name;
					switch(reviewed) {
					case yes: {
						Integer counter = yesCounterMap.get(name);
						yesCounterMap.put(name, counter == null ? 1 : (counter + 1));
						break;
					}
					case unsure: {
						Integer counter = unsureCounterMap.get(name);
						unsureCounterMap.put(name, counter == null ? 1 : (counter + 1));
						break;
					}
					case no: {
						Integer counter = noCounterMap.get(name);
						noCounterMap.put(name, counter == null ? 1 : (counter + 1));
						break;
					}
					}
					/*if(reviewedLabel.reviewed == Reviewed.yes) {
						String name = reviewedLabel.name;
						Integer counter = yesCounterMap.get(name);
						yesCounterMap.put(name, counter == null ? 1 : (counter + 1));
						Logger.info("confirmed1 " + name + "    " + sample.id + "   " + label.start + "  " + label.end);
					}*/
					break;
				}
				default: {
					HashMap<String, Reviewed> reviewedMap = new HashMap<String, Reviewed>();
					reviewedLabels.forEach(reviewedLabel -> {
						reviewedMap.put(reviewedLabel.name, reviewedLabel.reviewed); // overwrite multiple same name entries with newest
					});
					reviewedMap.forEach((String name, Reviewed reviewed) -> {
						switch(reviewed) {
						case yes: {
							Integer counter = yesCounterMap.get(name);
							yesCounterMap.put(name, counter == null ? 1 : (counter + 1));
							break;
						}
						case unsure: {
							Integer counter = unsureCounterMap.get(name);
							unsureCounterMap.put(name, counter == null ? 1 : (counter + 1));
							break;
						}
						case no: {
							Integer counter = noCounterMap.get(name);
							noCounterMap.put(name, counter == null ? 1 : (counter + 1));
							break;
						}
						}
					});

					/*HashSet<String> yesNames = new HashSet<String>();
					reviewedLabels.forEach(reviewedLabel -> {
						if(reviewedLabel.reviewed == Reviewed.yes) {
							yesNames.add(reviewedLabel.name);
						}
					});
					for(String name : yesNames) {
						Integer counter = yesCounterMap.get(name);
						yesCounterMap.put(name, counter == null ? 1 : (counter + 1));
						Logger.info("confirmedN " + name + "    " + sample.id + "   " + label.start + "  " + label.end);
					}*/
				}
				}

			});
		});

		broker.reviewListManager().forEach((name, reviewList) -> {
			int[] count = new int[] {0, 0};
			reviewList.forEach(entry -> {
				if(!entry.classified) {
					count[0]++;
				} else if(entry.latest_review == Reviewed.unsure){
					count[1]++;
				}
			});
			int cnt = count[0];
			if(cnt > 0) {
				reviewListOpenCounterMap.put(name, cnt);
			}
			int unsureCnt = count[1];
			if(unsureCnt > 0) {
				reviewListUnsureCounterMap.put(name, unsureCnt);
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

		json.key("reviewed_unsure_counts");
		json.object();
		unsureCounterMap.forEach((name, count) -> {
			json.key(name);
			json.value(count);
		});
		json.endObject();

		json.key("reviewed_no_counts");
		json.object();
		noCounterMap.forEach((name, count) -> {
			json.key(name);
			json.value(count);
		});
		json.endObject();

		json.key("review_list_open_counts");
		json.object();
		reviewListOpenCounterMap.forEach((name, count) -> {
			json.key(name);
			json.value(count);
		});
		json.endObject();
		
		json.key("review_list_unsure_counts");
		json.object();
		reviewListUnsureCounterMap.forEach((name, count) -> {
			json.key(name);
			json.value(count);
		});
		json.endObject();

		json.endObject();
	}
}