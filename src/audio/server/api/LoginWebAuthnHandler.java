package audio.server.api;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;

import audio.Account;
import audio.Broker;
import audio.Hex;
import audio.Nonce;
import audio.WebAuthn;
import audio.server.AccessHandler;
import util.TemplateUtil;

public class LoginWebAuthnHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;

	public LoginWebAuthnHandler(Broker broker) throws IOException {		
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		baseRequest.setHandled(true);
		log.info("LoginWebAuthnHandler");
		WebAuthn webAuthn = broker.webAuthn();
		try {
			JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
			AuthenticationRequest authenticationRequest = webAuthn.createAuthenticationRequest(jsonReq);
			Authenticator authenticator = webAuthn.load(authenticationRequest.getCredentialId());
			AuthenticationParameters authenticationParameters = webAuthn.createAuthenticationParameters(jsonReq, authenticator);
			AuthenticationData authenticationData = webAuthn.validateAuthention(jsonReq, authenticationRequest, authenticationParameters);
			
			String userHandle = webAuthn.bytesToString(authenticationData.getUserHandle());
			
			Account account = broker.accountManager().getAccount(userHandle);
			HttpSession session = request.getSession(true);
			AccessHandler.injectSameSite(response);
			session.setAttribute("authentication", "WebAuthn");
			session.setAttribute("account", account);
			session.setAttribute("roles", broker.roleManager().getRoleBits(account.roles));			
			
			response.setContentType("text/plain;charset=utf-8");		
			response.getWriter().write("Validated identity: " + WebAuthn.bytesToString(authenticationData.getUserHandle()));
		} catch(Exception e) {
			log.error(e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

}
