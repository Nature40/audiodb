package audio.server.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;

import audio.Account;
import audio.Broker;
import audio.WebAuthn;
import audio.WebAuthnAccount;

public class WebAuthnHandler extends AbstractHandler {
	

	private final Broker broker;	

	public WebAuthnHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Logger.info(target);
		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				handleRoot(baseRequest, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				Logger.info(name);
				switch(name) {
				case "register": {
					switch (baseRequest.getMethod()) {
					case "POST":
						handleRegisterPOST(baseRequest, response);
						break;
					default:
						throw new RuntimeException("no request");
					}
					break;
				}
				case "verify": {
					switch (baseRequest.getMethod()) {
					case "POST":
						handleVerifyPOST(baseRequest, response);
						break;
					default:
						throw new RuntimeException("no request");
					}
					break;
				}
				default:
					throw new RuntimeException("no request");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		response.setContentType("text/plain;charset=utf-8");		
		response.getWriter().write("WebAuthn");
	}

	private void handleRegisterPOST(Request request, HttpServletResponse response) throws IOException {
		WebAuthn webAuthn = broker.webAuthn();
		try{
			JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));

			byte[] clientDataJSON = Base64.getDecoder().decode(jsonReq.getString("clientDataJSON"));
			JSONObject clientDataJSONobject = new JSONObject(new JSONTokener(new String(clientDataJSON, StandardCharsets.UTF_8)));
			byte[] challengeBytes = WebAuthn.base64UrlToBytes(clientDataJSONobject.getString("challenge"));
			Logger.info(Arrays.toString(challengeBytes));
			WebAuthn.takeChallenge(challengeBytes);
			Challenge challenge = new DefaultChallenge(challengeBytes);
			byte[] attestationObject = Base64.getDecoder().decode(jsonReq.getString("attestationObject"));		
			Logger.info("clientDataJSON " + new String(clientDataJSON, StandardCharsets.UTF_8));
			Logger.info("attestationObject " + Arrays.toString(attestationObject));
			String clientExtensionJSON = null;  /* set clientExtensionJSON */;
			Set<String> transports = null /* set transports */;
			RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON, clientExtensionJSON, transports);

			// Server properties
			Origin origin = new Origin(clientDataJSONobject.getString("origin"));
			String rpId = jsonReq.getString("rpId");

			byte[] tokenBindingId = null /* set tokenBindingId */;
			ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);

			// expectations
			boolean userVerificationRequired = false;
			boolean userPresenceRequired = true;
			RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, userVerificationRequired, userPresenceRequired);


			RegistrationData registrationData = webAuthn.webAuthnManager.parse(registrationRequest);
			webAuthn.webAuthnManager.validate(registrationData, registrationParameters);

			Logger.info("clientDataJSONobject " + clientDataJSONobject);


			WebAuthnAccount webAuthnAccount = new WebAuthnAccount(registrationData.getAttestationObjectBytes());

			HttpSession session = request.getSession(false);
			Account account = (Account) session.getAttribute("account");
			account = Account.withWebAuthn(account, webAuthnAccount);
			broker.accountManager().setAccount(account, true);
			session.setAttribute("account", account);

		}
		catch (Exception e){			
			Logger.error(e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
		response.setContentType("text/plain;charset=utf-8");		
		response.getWriter().write("WebAuthn");
	}	

	private void handleVerifyPOST(Request request, HttpServletResponse response) throws IOException {
		WebAuthn webAuthn = broker.webAuthn();
		Logger.info(request.getOriginalURI());
		try{
			JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
			JSONObject clientDataJSONobject = WebAuthn.base64ToJSON(jsonReq.getString("clientDataJSON"));
			byte[] challengeBytes = WebAuthn.base64UrlToBytes(clientDataJSONobject.getString("challenge"));
			WebAuthn.takeChallenge(challengeBytes);	
			AuthenticationRequest authenticationRequest = webAuthn.createAuthenticationRequest(jsonReq);
			Account account = broker.accountManager().loadByCredentialId(authenticationRequest.getCredentialId());
			Authenticator authenticator = account.webAuthnAccount().authenticator();
			AuthenticationParameters authenticationParameters = webAuthn.createAuthenticationParameters(jsonReq, authenticator);
			AuthenticationData authenticationData = webAuthn.validateAuthention(jsonReq, authenticationRequest, authenticationParameters);
			response.setContentType("text/plain;charset=utf-8");		
			response.getWriter().write("Validated identity: " + account.username);
		}
		catch (Exception e){			
			Logger.error(e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}		
	}

}