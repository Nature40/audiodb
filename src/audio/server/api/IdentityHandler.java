package audio.server.api;

import java.io.IOException;
import java.util.BitSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
		String authentication = (String) session.getAttribute("authentication");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		String[] roleNames = broker.roleManager().getRoleNames(roleBits);
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("authentication");
		json.value(authentication);
		json.key("user");
		json.value(account.username);
		json.key("roles");
		json.value(roleNames);
		json.endObject();
	}
}
