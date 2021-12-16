package audio.server.api;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.opencsv.CSVWriter;

import audio.Broker;
import audio.Label;
import audio.Sample;
import audio.processing.Metric;
import audio.processing.Metrics;
import audio.processing.SampleProcessor;
import util.collections.vec.Vec;

public class QueryHandler extends AbstractHandler {
	

	private final SampleHandler sampleHandler;

	private final Broker broker;

	public QueryHandler(Broker broker) {
		this.broker = broker;
		sampleHandler = new SampleHandler(broker);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse httpResponse) throws IOException, ServletException {
		Response response = (Response) httpResponse;
		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				switch(baseRequest.getMethod()) {
				case "POST":
					handleRootPOST(baseRequest, response);
					break;
				default: {
					String errorText = "unknown method in " + baseRequest.getMethod();
					Logger.error(errorText);
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.setContentType("text/plain");
					response.getWriter().print(errorText);		
				}
				}
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				switch(name) {
				case "metrics":
					switch(baseRequest.getMethod()) {
					case "GET":
						handleMetrics(baseRequest, response);
						break;
					default: {
						String errorText = "unknown method in " + baseRequest.getMethod();
						Logger.error(errorText);
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.setContentType("text/plain");
						response.getWriter().print(errorText);		
					}
					}
					break;
				default:
					throw new RuntimeException("no request");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	private void handleMetrics(Request request, Response response) throws IOException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("metrics");
		json.array();
		for(Metric metric:Metrics.metrics) {
			json.object();
			json.key("name");
			json.value(metric.name);
			json.endObject();	
		}
		json.endArray();
		json.endObject();		
	}

	static final int HEADER_META_ROWS = 1;

	private void handleRootPOST(Request request, HttpServletResponse response) throws IOException {
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonMetrics = jsonReq.getJSONArray("metrics");
		int jsonMetricsLen = jsonMetrics.length();
		Vec<Metric> metrics = new Vec<Metric>();
		for(int i=0; i<jsonMetricsLen; i++) {
			JSONObject jsonMetric = jsonMetrics.getJSONObject(i);
			String name = jsonMetric.getString("name");
			Metric metric = Metrics.metricsMap.get(name);
			if(metric == null) {
				throw new RuntimeException("Metric not found: " + name);
			}
			metrics.add(metric);
		}

		response.setContentType("text/csv;charset=utf-8");		
		try(CSVWriter writer = new CSVWriter(
				response.getWriter(), 
				CSVWriter.DEFAULT_SEPARATOR, 
				CSVWriter.DEFAULT_QUOTE_CHARACTER,
				CSVWriter.DEFAULT_ESCAPE_CHARACTER,
				CSVWriter.DEFAULT_LINE_END
				)) {
			String[] header = new String[HEADER_META_ROWS + metrics.size()];
			header[0] = "sample";

			for (int i = 0; i < metrics.size(); i++) {
				header[HEADER_META_ROWS + i] = metrics.get(i).name;
			}

			writer.writeNext(header, false);
			process(writer, metrics);
		}
	}

	private void process(CSVWriter writer, Vec<Metric> metrics) {
		broker.samples().sampleMap.values().stream().parallel().limit(20).forEach(sample -> {
			processSample(writer, sample, metrics);
		});
	}

	private Vec<String[]> processSample(Sample sample, Vec<Metric> metrics) {
		
		Vec<String[]> rows = new Vec<String[]>();

		SampleProcessor sampleProcessor = new SampleProcessor(sample);
		sampleProcessor.loadData(0);	
		sampleProcessor.transform();
		sampleProcessor.calcBins();

		Vec<Label> labels = sample.getLabels();
		
		String[] row = new String[HEADER_META_ROWS + metrics.size()];
		row[0] = sample.id;


		int posStart = 0;
		int posEnd = sampleProcessor.dataLength - 1;

		int colStart = sampleProcessor.timeToCol(posStart);
		int colEnd = sampleProcessor.timeToCol(posEnd);
		
		if(colStart >= sampleProcessor.fqCols) {
			colStart = sampleProcessor.fqCols - 1;
		}
		
		if(colEnd >= sampleProcessor.fqCols) {
			colEnd = sampleProcessor.fqCols - 1;
		}

		//Logger.info(sampleProcessor.frameLength + "  "  + posStart + "  "  + posEnd);

		for (int i = 0; i < metrics.size(); i++) {
			try {
				row[HEADER_META_ROWS + i] = Double.toString(metrics.get(i).apply(sampleProcessor, posStart, posEnd, colStart, colEnd));
			} catch (Exception e) {
				//Logger.warn(e);
				row[HEADER_META_ROWS + i] = "NA";
			}

		}
		rows.add(row);
		//writer.writeNext(row, false);
		
		return rows;
	}
	
	private void processSample(CSVWriter writer, Sample sample, Vec<Metric> metrics) {		
		Vec<String[]> rows = processSample(sample, metrics);
		synchronized (writer) {
			for(String[] row:rows) {
				writer.writeNext(row, false);
			}			
		}
	}
}