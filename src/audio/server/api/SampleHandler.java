package audio.server.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;

import audio.Broker;
import audio.Sample;

public class SampleHandler {

	private final LabelsHandler labelsHandler = new LabelsHandler();

	private final Broker broker;

	public SampleHandler(Broker broker) {
		this.broker = broker;
	}

	public void handle(String sampleText, String target, Request request, HttpServletResponse response) throws IOException {
		Sample sample = broker.samples().getThrow(sampleText);
		if(target.equals("/")) {
			handleRoot(request, response);
		} else {
			int i = target.indexOf('/', 1);
			if(i == 1) {
				throw new RuntimeException("no name: "+target);
			}			
			String name = i < 0 ? target.substring(1) : target.substring(1, i);
			String next = i < 0 ? "/" : target.substring(i);
			switch(name) {
			case "labels":
				labelsHandler.handle(sample, next, request, response);
				break;
			case "data":
				handleData(sample, request, response);
				break;
			default:
				throw new RuntimeException("no call");
			}			
		}		
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		throw new RuntimeException("no call");
	}

	private void handleData(Sample sample, Request request, HttpServletResponse response) throws IOException {
		response.setContentType("audio/wav");
		File file = sample.path.toFile();
		response.setContentLengthLong(file.length());
		try(FileInputStream in = new FileInputStream(file)) {
			IO.copy(in, response.getOutputStream());
		}
	}

}
