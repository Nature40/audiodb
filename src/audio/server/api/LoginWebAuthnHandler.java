package audio.server.api;

import java.io.IOException;
import java.util.Arrays;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.Web;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;

import audio.Account;
import audio.Broker;
import audio.WebAuthn;
import audio.server.AccessHandler;

public class LoginWebAuthnHandler extends AbstractHandler {

	private final Broker broker;

	public LoginWebAuthnHandler(Broker broker) throws IOException {		
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			Logger.info("LoginWebAuthnHandler");
			baseRequest.setHandled(true);
			switch(baseRequest.getMethod()) {
			case "GET":
				handleGET(target, baseRequest, request, response);
				break;
			case "POST":
				handlePOST(target, baseRequest, request, response);
				break;
			default: {
				String errorText = "unknown method in " + "login: " + baseRequest.getMethod();
				Logger.error(errorText );
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType(Web.MIME_TEXT);
				response.getWriter().print(errorText);		
			}
			}
		}
		catch(Exception e) {
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("error");
			json.value(e.getMessage());
			json.endObject();
		}
	}
	
	public void handleGET(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());			
			byte[] challengeBytes = WebAuthn.createChallenge();
			Logger.info(Arrays.toString(challengeBytes));
			String challenge = WebAuthn.bytesToBase64(challengeBytes);
			
			json.object();
			json.key("challenge");
			json.value(challenge);
			json.endObject();
		} catch(Exception e) {
			Logger.error(e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	public void handlePOST(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		WebAuthn webAuthn = broker.webAuthn();
		try {
			JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
			
			JSONObject clientDataJSONobject = WebAuthn.base64ToJSON(jsonReq.getString("clientDataJSON"));
			byte[] challengeBytes = WebAuthn.base64UrlToBytes(clientDataJSONobject.getString("challenge"));
			WebAuthn.takeChallenge(challengeBytes);						
			AuthenticationRequest authenticationRequest = webAuthn.createAuthenticationRequest(jsonReq);			
			Account account = broker.accountManager().loadByCredentialId(authenticationRequest.getCredentialId());
			Authenticator authenticator = account.webAuthnAccount().authenticator();
			AuthenticationParameters authenticationParameters = webAuthn.createAuthenticationParameters(jsonReq, authenticator);
			AuthenticationData authenticationData = webAuthn.validateAuthention(jsonReq, authenticationRequest, authenticationParameters);
			
			HttpSession session = request.getSession(true);
			AccessHandler.injectSameSite(response);
			session.setAttribute("authentication", "WebAuthn");
			session.setAttribute("account", account);
			session.setAttribute("roles", broker.roleManager().getRoleBits(account.roles));			
			
			response.setContentType(Web.MIME_TEXT);		
			response.getWriter().write("Validated identity: " + WebAuthn.bytesToString(authenticationData.getUserHandle()));
		} catch(Exception e) {
			Logger.error(e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}
}
