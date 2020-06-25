package audio.server.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
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

import audio.Broker;

public class WebAuthnHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();

	private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

	private final SampleHandler sampleHandler;

	private final Broker broker;

	public WebAuthnHandler(Broker broker) {
		this.broker = broker;
		sampleHandler = new SampleHandler(broker);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		log.info(target);
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
				log.info(name);
				switch(name) {
				case "register":
					handleRegister(baseRequest, response);
					break;
				case "verify":
					handleVerify(baseRequest, response);
					break;					
				default:
					throw new RuntimeException("no request");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	private void handleRoot(Request request, HttpServletResponse response) throws IOException {
		response.setContentType("text/plain;charset=utf-8");		
		response.getWriter().write("WebAuthn");
	}

	private void handleRegister(Request request, HttpServletResponse response) throws IOException {
		try{
			JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));

			byte[] clientDataJSON = Base64.getDecoder().decode(jsonReq.getString("clientDataJSON"));
			JSONObject clientDataJSONobject = new JSONObject(new JSONTokener(new String(clientDataJSON, StandardCharsets.UTF_8)));
			byte[] attestationObject = Base64.getDecoder().decode(jsonReq.getString("attestationObject"));		
			log.info("clientDataJSON " + new String(clientDataJSON, StandardCharsets.UTF_8));
			log.info("attestationObject " + Arrays.toString(attestationObject));
			String clientExtensionJSON = null;  /* set clientExtensionJSON */;
			Set<String> transports = null /* set transports */;
			RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON, clientExtensionJSON, transports);

			// Server properties
			Origin origin = new Origin(clientDataJSONobject.getString("origin"));
			String rpId = jsonReq.getString("rpId");
			Challenge challenge = new DefaultChallenge(Base64.getDecoder().decode(clientDataJSONobject.getString("challenge")));
			byte[] tokenBindingId = null /* set tokenBindingId */;
			ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);

			// expectations
			boolean userVerificationRequired = false;
			boolean userPresenceRequired = true;
			List<String> expectedExtensionIds = Collections.emptyList();
			RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, userVerificationRequired, userPresenceRequired, expectedExtensionIds);


			RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
			webAuthnManager.validate(registrationData, registrationParameters);

			Authenticator authenticator =
					new AuthenticatorImpl( // You may create your own Authenticator implementation to save friendly authenticator name
							registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData(),
							registrationData.getAttestationObject().getAttestationStatement(),
							registrationData.getAttestationObject().getAuthenticatorData().getSignCount()
							);
			save(authenticator);
		}
		catch (Exception e){			
			log.error(e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}


		response.setContentType("text/plain;charset=utf-8");		
		response.getWriter().write("WebAuthn");
	}

	private void handleVerify(Request request, HttpServletResponse response) throws IOException {
		try{
			JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));

			// Client properties
			byte[] credentialId = Base64.getDecoder().decode(jsonReq.getString("credentialId"));
			byte[] userHandle = Base64.getDecoder().decode(jsonReq.getString("userHandle"));
			byte[] authenticatorData = Base64.getDecoder().decode(jsonReq.getString("authenticatorData"));
			byte[] clientDataJSON = Base64.getDecoder().decode(jsonReq.getString("clientDataJSON"));
			JSONObject clientDataJSONobject = new JSONObject(new JSONTokener(new String(clientDataJSON, StandardCharsets.UTF_8)));
			String clientExtensionJSON = null /* set clientExtensionJSON */;
			byte[] signature = Base64.getDecoder().decode(jsonReq.getString("signature"));
			
			log.info("clientDataJSON " + new String(clientDataJSON, StandardCharsets.UTF_8));

			// Server properties
			Origin origin = new Origin(clientDataJSONobject.getString("origin"));
			String rpId = jsonReq.getString("rpId");
			Challenge challenge = new DefaultChallenge(Base64.getDecoder().decode(clientDataJSONobject.getString("challenge")));
			byte[] tokenBindingId = null /* set tokenBindingId */;
			ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);

			// expectations
			boolean userVerificationRequired = true;
			boolean userPresenceRequired = true;
			List<String> expectedExtensionIds = Collections.emptyList();

			Authenticator authenticator = load(credentialId);

			AuthenticationRequest authenticationRequest =
					new AuthenticationRequest(
							credentialId,
							userHandle,
							authenticatorData,
							clientDataJSON,
							clientExtensionJSON,
							signature
							);
			AuthenticationParameters authenticationParameters =
					new AuthenticationParameters(
							serverProperty,
							authenticator,
							userVerificationRequired,
							userPresenceRequired,
							expectedExtensionIds
							);

			AuthenticationData	authenticationData = webAuthnManager.parse(authenticationRequest);	
			webAuthnManager.validate(authenticationData, authenticationParameters);
		}
		catch (Exception e){			
			log.error(e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
		response.setContentType("text/plain;charset=utf-8");		
		response.getWriter().write("WebAuthn");
	}
	
	
	Authenticator authenticator = null;
	
	private void save(Authenticator authenticator) {
		this.authenticator = authenticator;		
	}

	private Authenticator load(byte[] credentialId) {
		return this.authenticator;
	}
}