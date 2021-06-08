package audio.server.api;

import java.io.IOException;
import java.util.HashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.GeneratorLabel;
import audio.review.ReviewedLabel;
import util.collections.vec.Vec;

public class ReviewStatisticsDetailedHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	private final SampleHandler sampleHandler;

	private final Broker broker;

	public ReviewStatisticsDetailedHandler(Broker broker) {
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

	private static class NameStat {
		public int noCounter;
		public int unsureCounter;
		public int yesCounter;
		
		public void addNo() {
			noCounter++;
		}
		
		public void addUnsure() {
			unsureCounter++;
		}
		
		public void addYes() {
			yesCounter++;
		}
	}

	private static class NameModelStat extends NameStat {
		public HashMap<String, NameStat> modelStatMap = new HashMap<String, NameStat>();
		
		public void addNo(Iterable<GeneratorLabel> generatorLabels) {
			addNo();
			for(GeneratorLabel generatorLabel : generatorLabels) {
				NameStat stat = modelStatMap.get(generatorLabel.generator);
				if(stat == null) {
					stat = new NameStat();
					modelStatMap.put(generatorLabel.generator, stat);
				}
				stat.addNo();
			}
		}
		
		public void addUnsure(Iterable<GeneratorLabel> generatorLabels) {
			addUnsure();
			for(GeneratorLabel generatorLabel : generatorLabels) {
				NameStat stat = modelStatMap.get(generatorLabel.generator);
				if(stat == null) {
					stat = new NameStat();
					modelStatMap.put(generatorLabel.generator, stat);
				}
				stat.addUnsure();
			}
		}
		
		public void addYes(Iterable<GeneratorLabel> generatorLabels) {
			addYes();
			for(GeneratorLabel generatorLabel : generatorLabels) {
				NameStat stat = modelStatMap.get(generatorLabel.generator);
				if(stat == null) {
					stat = new NameStat();
					modelStatMap.put(generatorLabel.generator, stat);
				}
				stat.addYes();	
			}
		}
	}

	private static class StatCollector {

		HashMap<String, NameModelStat> nameStatMap = new HashMap<String, NameModelStat>();
		
		HashMap<String, Integer> reviewerCountMap = new HashMap<String, Integer>();

		double threshold = 0.8;

		StatCollector() {
		}

		public void collectWithDublicates(Vec<ReviewedLabel> reviewedLabels, Vec<GeneratorLabel> generatorLabels) {
			switch(reviewedLabels.size()) {
			case 0: {
				//nothing
				break;
			}
			case 1: {
				collect(reviewedLabels.first(), generatorLabels);
				break;
			}
			default: {
				HashMap<String, ReviewedLabel> namesMap = new HashMap<String, ReviewedLabel>();
				reviewedLabels.forEach(reviewedLabel -> {
					namesMap.put(reviewedLabel.name, reviewedLabel);
				});
				for(ReviewedLabel reviewedLabel : namesMap.values()) {
					collect(reviewedLabel, generatorLabels);
				}
			}
			}

		}

		public void collect(ReviewedLabel reviewedLabel, Vec<GeneratorLabel> generatorLabels) {			
			Integer counter = reviewerCountMap.get(reviewedLabel.reviewer);			
			reviewerCountMap.put(reviewedLabel.reviewer, counter == null ? 1 : (counter + 1));
			
			String name = reviewedLabel.name;
			double thr = threshold;
			Iterable<GeneratorLabel> gl = generatorLabels.filteredIterableWeakView(generatorLabel -> name.equals(generatorLabel.name) && generatorLabel.reliability >= thr);

			switch(reviewedLabel.reviewed) {
			case no: {
				NameModelStat nameStat = nameStatMap.get(name);
				if(nameStat == null) {
					nameStat = new NameModelStat();
					nameStatMap.put(name, nameStat);
				}
				nameStat.addNo(gl);
				break;
			}
			case unsure: {
				NameModelStat nameStat = nameStatMap.get(name);
				if(nameStat == null) {
					nameStat = new NameModelStat();
					nameStatMap.put(name, nameStat);
				}
				nameStat.addUnsure(gl);
				break;
			}
			case yes: {
				NameModelStat nameStat = nameStatMap.get(name);
				if(nameStat == null) {
					nameStat = new NameModelStat();
					nameStatMap.put(name, nameStat);
				}
				nameStat.addYes(gl);
				break;
			}
			}
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		
		String thresholdText = request.getParameter("threshold");
		double threshold = Double.parseDouble(thresholdText);

		StatCollector statCollector = new StatCollector();
		statCollector.threshold = threshold;

		broker.samples().forEach(sample -> {
			sample.forEachLabel(label -> {
				statCollector.collectWithDublicates(label.reviewedLabels, label.generatorLabels);
			});
		});

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		
		json.key("reviewer_stats");
		json.array();
		statCollector.reviewerCountMap.forEach((name, cnt) -> {
			json.object();
			json.key("name");
			json.value(name);
			json.key("count");
			json.value(cnt);
			json.endObject();
		});
		json.endArray();

		json.key("name_stats");
		json.array();
		statCollector.nameStatMap.forEach((name, nameStat) -> {
			json.object();
			json.key("name");
			json.value(name);
			json.key("total");
			int total = nameStat.noCounter + nameStat.unsureCounter + nameStat.yesCounter;
			json.value(total);			
			json.key("no");
			json.value(nameStat.noCounter);
			json.key("unsure");
			json.value(nameStat.unsureCounter);
			json.key("yes");
			json.value(nameStat.yesCounter);
			json.key("nopp");
			json.value((int)Math.round((nameStat.noCounter * 100d) / total));
			json.key("unsurepp");
			json.value((int)Math.round((nameStat.unsureCounter * 100d) / total));
			json.key("yespp");
			json.value((int)Math.round((nameStat.yesCounter * 100d) / total));

			nameStat.modelStatMap.forEach((modelName, modelStat) -> {
				json.key("total_" + modelName);
				int totalm = modelStat.noCounter + modelStat.unsureCounter + modelStat.yesCounter;
				json.value(totalm);			
				json.key("no_" + modelName);
				json.value(modelStat.noCounter);
				json.key("unsure_" + modelName);
				json.value(modelStat.unsureCounter);
				json.key("yes_" + modelName);
				json.value(modelStat.yesCounter);
				json.key("nopp_" + modelName);
				json.value((int)Math.round((modelStat.noCounter * 100d) / totalm));
				json.key("unsurepp_" + modelName);
				json.value((int)Math.round((modelStat.unsureCounter * 100d) / totalm));
				json.key("yespp_" + modelName);
				json.value((int)Math.round((modelStat.yesCounter * 100d) / totalm));
			});

			json.endObject();
		});
		json.endArray();

		json.endObject();
	}
}