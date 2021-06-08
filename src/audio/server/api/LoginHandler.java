package audio.server.api;

import java.io.IOException;
import java.util.HashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import audio.Account;
import audio.Broker;
import audio.Hex;
import audio.Nonce;
import audio.server.AccessHandler;
import util.TemplateUtil;

public class LoginHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;

	public LoginHandler(Broker broker) throws IOException {		
		this.broker = broker;
	}

	private static class LoginException extends RuntimeException {
		public LoginException(String message) {
			super(message);
		}
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {		
		try {
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			String server_nonce = baseRequest.getParameter("server_nonce");
			if(!Nonce.isValid(server_nonce, 8)) {
				throw new LoginException("missing server_nonce");
			}
			String client_nonce = baseRequest.getParameter("client_nonce");
			if(!Nonce.isValid(client_nonce, 8)) {
				throw new LoginException("missing client_nonce");
			}
			String client_hash = baseRequest.getParameter("hash");
			if(!Hex.isValid(client_hash, 128)) {
				throw new LoginException("missing hash");
			}

			Account account = broker.accountManager().validate(server_nonce, client_nonce, client_hash);

			if(account != null) {
				HttpSession session = request.getSession(true);
				AccessHandler.injectSameSite(response);
				session.setAttribute("authentication", "login");
				session.setAttribute("account", account);
				session.setAttribute("roles", broker.roleManager().getRoleBits(account.roles));			
				baseRequest.setHandled(true);
				response.setHeader(HttpHeader.LOCATION.asString(), "/");
				response.setStatus(HttpServletResponse.SC_FOUND);
				response.setContentLength(0);
			} else {
				throw new LoginException("invalid credentials");
			}
		} catch(LoginException e) {
			log.warn(e);
			baseRequest.setHandled(true);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			HashMap<String, Object> ctx = new HashMap<>();
			ctx.put("error", e.getMessage());
			String redirect_target = "/";
			ctx.put("redirect_target", redirect_target);
			TemplateUtil.getTemplate("login_local_error.mustache", true).execute(ctx, response.getWriter());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

}
