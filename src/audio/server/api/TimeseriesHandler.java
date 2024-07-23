package audio.server.api;

import java.io.IOException;
import java.time.LocalDateTime;

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

import audio.Broker;
import audio.Sample;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import util.AudioTimeUtil;
import util.Web;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class TimeseriesHandler extends AbstractHandler {
	

	private final Broker broker;

	String[] DEFINED_INDICES = new String[] {"ndsi_left",  "biophony_left",  "anthrophony_left",  "ADI_0.1000.Hz",  "ADI_1000.2000.Hz",  "ADI_2000.3000.Hz", "ADI_3000.4000.Hz",  "ADI_4000.5000.Hz",  "ADI_5000.6000.Hz",  "ADI_6000.7000.Hz",  "ADI_7000.8000.Hz",  "ADI_8000.9000.Hz",  "ADI_9000.10000.Hz",  "ADI",  "bioacoustic_index",  "acoustic_evenness",  "acoustic_complexity"};


	public TimeseriesHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse httpResponse) throws IOException, ServletException {
		Response response = (Response) httpResponse;
		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				switch(baseRequest.getMethod()) {
				case "GET":
					handleRootGET(baseRequest, response);
					break;				
				case "POST":
					handleRootPOST(baseRequest, response);
					break;
				default: {
					String errorText = "unknown method in " + baseRequest.getMethod();
					Logger.error(errorText);
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.setContentType(Web.MIME_TEXT);
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
				case "indices":
					switch(baseRequest.getMethod()) {
					case "GET":
						handleIndices(baseRequest, response);
						break;
					default: {
						String errorText = "unknown method in " + baseRequest.getMethod();
						Logger.error(errorText);
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						response.setContentType(Web.MIME_TEXT);
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
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	private void handleIndices(Request request, Response response) throws IOException {
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("indices");
		json.array();
		for(String name : DEFINED_INDICES) {
			json.object();
			json.key("name");
			json.value(name);
			json.endObject();	
		}
		json.endArray();
		json.endObject();		
	}

	static final int HEADER_META_ROWS = 2;

	private void handleRootPOST(Request request, HttpServletResponse response) throws IOException {
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonIndices = jsonReq.getJSONArray("indices");
		int jsonIndicesLen = jsonIndices.length();
		Vec<String> indices = new Vec<String>();
		for(int i=0; i<jsonIndicesLen; i++) {
			JSONObject jsonIndex = jsonIndices.getJSONObject(i);
			String name = jsonIndex.getString("name");
			indices.add(name);
		}
		process(indices, response);
	}

	private void handleRootGET(Request request, HttpServletResponse response) throws IOException {
		Vec<String> indices = new Vec<String>();
		indices.addAll(DEFINED_INDICES);
		process(indices, response);
	}

	private void process(Vec<String> indices, HttpServletResponse response) throws IOException {
		response.setContentType(Web.MIME_CSV);		
		try (
				CsvWriter csv = CsvWriter.builder()
				.fieldSeparator(',')
				.quoteCharacter('"')
				.quoteStrategy(null) // quote when needed only
				.commentCharacter('#')
				.lineDelimiter(LineDelimiter.CRLF)
				.build(response.getWriter())
				) {
			String[] header = new String[HEADER_META_ROWS + indices.size()];
			header[0] = "plotID";
			header[1] = "datetime";
			for (int i = 0; i < indices.size(); i++) {
				header[HEADER_META_ROWS + i] = indices.get(i);
			}
			csv.writeRecord(header);		    
			process(csv, indices);	    
		}
	}

	private void process(CsvWriter csv, Vec<String> indices) {
		/*broker.samples().sampleMap.values().stream().parallel().limit(20).forEach(sample -> {
			processSample(writer, sample, indices);
		});*/
		for(Sample sample : broker.samples().sampleMap.values()) {
			processSample(csv, sample, indices);
		}
	}

	private Vec<String[]> processSample(Sample sample, Vec<String> indices) {
		
		long unixTimestamp = sample.getMetaMap().getNumber("timestamp").longValue();
		LocalDateTime timestamp = AudioTimeUtil.ofAudiotime(unixTimestamp);

		YamlMap indicesMap = sample.getMetaMap().getMap("indices");

		Vec<String[]> rows = new Vec<String[]>();

		String[] row = new String[HEADER_META_ROWS + indices.size()];
		row[0] = sample.getMetaMap().getString("location");
		row[1] = AudioTimeUtil.toTextMinutes(timestamp);

		for (int i = 0; i < indices.size(); i++) {
			String value = Double.toString(indicesMap.optDouble(indices.get(i), Double.NaN));
			row[HEADER_META_ROWS + i] = value;
		}
		rows.add(row);
		//writer.writeNext(row, false);

		return rows;
	}

	private void processSample(CsvWriter csv, Sample sample, Vec<String> indices) {		
		Vec<String[]> rows = processSample(sample, indices);
		synchronized (csv) {
			for(String[] row:rows) {
				csv.writeRecord(row);
			}			
		}
	}
}