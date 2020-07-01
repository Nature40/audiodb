package audio.server.api;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.opencsv.CSVWriter;

import audio.Broker;
import audio.Label;
import audio.Sample;
import audio.processing.Metric;
import audio.processing.Metric_duration;
import audio.processing.Metric_fmax;
import audio.processing.Metric_intensity;
import audio.processing.SampleProcessor;
import util.collections.vec.Vec;

public class QueryHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	private final SampleHandler sampleHandler;

	private final Broker broker;

	public QueryHandler(Broker broker) {
		this.broker = broker;
		sampleHandler = new SampleHandler(broker);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				switch(baseRequest.getMethod()) {
				case "POST":
					handleRootPOST(baseRequest, response);
					break;
				default: {
					String errorText = "unknown method in " + "label_definitions: " + baseRequest.getMethod();
					log.error(errorText);
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
				throw new RuntimeException("no request");
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	static final int HEADER_META_ROWS = 5;

	private void handleRootPOST(Request request, HttpServletResponse response) throws IOException {
		//JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		//JSONArray jsonSamples = jsonReq.getJSONArray("samples");


		Metric[] metrics = new Metric[] {Metric_duration.INSATNCE, Metric_intensity.INSATNCE, Metric_fmax.INSATNCE};


		response.setContentType("text/csv;charset=utf-8");		
		try(CSVWriter writer = new CSVWriter(
				response.getWriter(), 
				CSVWriter.DEFAULT_SEPARATOR, 
				CSVWriter.DEFAULT_QUOTE_CHARACTER,
				CSVWriter.DEFAULT_ESCAPE_CHARACTER,
				CSVWriter.DEFAULT_LINE_END
				)) {
			String[] header = new String[HEADER_META_ROWS + metrics.length];
			header[0] = "sample";
			header[1] = "start";
			header[2] = "end";
			header[3] = "generated_label";
			header[4] = "label";
			//header[5] = "comment";

			for (int i = 0; i < metrics.length; i++) {
				header[HEADER_META_ROWS + i] = metrics[i].name;
			}

			writer.writeNext(header, false);
			process(writer, metrics);
		}
	}

	private void process(CSVWriter writer, Metric[] metrics) {
		for(Sample sample:broker.samples().sampleMap.values()) {
			processSample(writer, sample, metrics);
		}
	}

	private void processSample(CSVWriter writer, Sample sample, Metric[] metrics) {

		SampleProcessor sampleProcessor = new SampleProcessor(sample);
		sampleProcessor.loadData(0);	
		sampleProcessor.transform(1024, 256);

		Vec<Label> labels = sample.getLabels();
		for(Label label:labels) {					
			String[] row = new String[HEADER_META_ROWS + metrics.length];
			row[0] = sample.id;
			row[1] = Double.toString(label.start);
			row[2] = Double.toString(label.end);
			row[3] = Arrays.toString(label.generated_labels);
			row[4] = Arrays.toString(label.labels);
			//row[5] = label.comment;

			int posStart = sampleProcessor.secondsToPos(label.start);
			int posEnd = sampleProcessor.secondsToPos(label.end);

			for (int i = 0; i < metrics.length; i++) {
				try {
					row[HEADER_META_ROWS + i] = Double.toString(metrics[i].calc(sampleProcessor, posStart, posEnd));
				} catch (Exception e) {
					log.warn(e);
					row[HEADER_META_ROWS + i] = "NA";
				}

			}
			writer.writeNext(row, false);
		}	
	}
}