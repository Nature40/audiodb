package audio.server.api;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Broker;

public class AccountHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;

	public AccountHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();
		json.key("salt");
		json.value(broker.accountManager().salt());
		json.endObject();
	}
}
