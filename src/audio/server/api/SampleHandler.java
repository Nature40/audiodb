package audio.server.api;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

public class SampleHandler {
	
	private final LabelsHandler labelsHandler = new LabelsHandler();
	
	public void handle(String sample, String target, Request request, HttpServletResponse response) throws IOException {
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
			default:
				throw new RuntimeException("no call");
			}			
		}		
	}
	
	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		throw new RuntimeException("no call");
	}

}
