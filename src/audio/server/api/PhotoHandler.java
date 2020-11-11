package audio.server.api;

import java.io.IOException;
import java.util.BitSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Account;
import audio.Broker;
import photo.PhotoDB;

public class PhotoHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;

	public PhotoHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		
		PhotoDB phototDB = broker.photoDB();

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("photos");
		json.array();
		
		phototDB.foreach(photo -> {
			json.object();
			json.key("id");
			json.value(photo.id);
			json.endObject();	
		});		
		
		json.endArray();
		json.endObject();		

	}
}
