package audio.server.api;

import java.io.IOException;

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

public class IdentityHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;

	public IdentityHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");		
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("user");
		json.value(account.username);
		json.key("roles");
		json.value(account.roles);
		json.endObject();
	}
}
