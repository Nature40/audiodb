package audio.server.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import com.opencsv.CSVWriter;

import audio.Label;
import audio.server.Webserver;
import util.collections.vec.Vec;

public class QueryHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	private final SampleHandler sampleHandler = new SampleHandler();

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

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		response.setContentType("text/csv;charset=utf-8");		
		try(CSVWriter writer = new CSVWriter(response.getWriter(), 
				CSVWriter.DEFAULT_SEPARATOR, 
				CSVWriter.DEFAULT_QUOTE_CHARACTER,
				CSVWriter.DEFAULT_ESCAPE_CHARACTER,
				CSVWriter.DEFAULT_LINE_END
				)) {
			String[] header = new String[]{"sample", "start", "end", "label", "comment"};
			writer.writeNext(header, false);
			Path root = Paths.get("data");
			for(Path path:Webserver.getPaths(root)) {
				String sample = root.relativize(path).toString();
				Vec<Label> labels = LabelsHandler.loadLabels(sample);
				for(Label label:labels) {					
					String[] row = new String[]{sample, Double.toString(label.start), Double.toString(label.end), Arrays.toString(label.labels), label.comment};
					writer.writeNext(row, false);
				}
			}
		}
	}
}