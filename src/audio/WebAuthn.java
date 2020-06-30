package audio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;

public class WebAuthn {
	static final Logger log = LogManager.getLogger();
	
	public final static ConcurrentSkipListSet<byte[]> challenges = new ConcurrentSkipListSet<byte[]>(AccountManager.BYTES_COMPARATOR);
	
	public final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
	
	WebAuthn() {
	}
	
	public static final Encoder BASE64_ENCODER = Base64.getEncoder();
	public static final Decoder BASE64_DECODER = Base64.getDecoder();
	
	public static final Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder();
	public static final Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
	
	
	public static byte[] createChallenge() {
		byte[] challengeBytes = new byte[26];
		do {
			ThreadLocalRandom.current().nextBytes(challengeBytes);
		} while (!challenges.add(challengeBytes));
		log.info("create challenge");
		log.info(Arrays.toString(challengeBytes));
		log.info("existings challenges");
		for(byte[] cmpChallenge:challenges) {
			log.info(Arrays.toString(cmpChallenge));
		}
		return challengeBytes;
	}
	
	public static void takeChallenge(byte[] challengeBytes) {
		log.info("take challenge");
		new RuntimeException().printStackTrace();
		log.info(Arrays.toString(challengeBytes));
		log.info("existings challenges");
		for(byte[] cmpChallenge:challenges) {
			log.info(Arrays.toString(cmpChallenge));
		}
		if(!challenges.remove(challengeBytes)) {						
			throw new RuntimeException("not valid challenge");
		}		
	}	

	public static String bytesToBase64(byte[] bytes) {
		return BASE64_ENCODER.encodeToString(bytes);
	}


	public static String bytesToString(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static byte[] base64ToBytes(String base64) {
		return BASE64_DECODER.decode(base64);
	}
	
	public static byte[] base64UrlToBytes(String base64Url) {
		return BASE64_URL_DECODER.decode(base64Url);
	}

	public static String base64ToString(String base64) {
		return bytesToString(base64ToBytes(base64));
	}

	public static JSONObject bytesToJSON(byte[] bytes) {
		return new JSONObject(new JSONTokener(bytesToString(bytes)));
	}

	public static JSONObject base64ToJSON(String base64) {
		return bytesToJSON(base64ToBytes(base64));
	}
	
	public static JSONObject base64UrlToJSON(String base64Url) {
		return bytesToJSON(base64UrlToBytes(base64Url));
	}

	public AuthenticationRequest createAuthenticationRequest(JSONObject jsonReq) {
		byte[] credentialId = base64ToBytes(jsonReq.getString("credentialId"));
		log.info("credentialId " + bytesToBase64(credentialId));
		byte[] userHandle = base64ToBytes(jsonReq.getString("userHandle"));
		String userID = bytesToString(userHandle);
		log.info("userID " + userID);
		byte[] authenticatorData = base64ToBytes(jsonReq.getString("authenticatorData"));
		byte[] clientDataJSON = base64ToBytes(jsonReq.getString("clientDataJSON"));
		log.info("clientDataJSON " + bytesToString(clientDataJSON));
		String clientExtensionJSON = null;
		byte[] signature = base64ToBytes(jsonReq.getString("signature"));
		return new AuthenticationRequest(credentialId, userHandle, authenticatorData, clientDataJSON, clientExtensionJSON, signature);
	}

	public AuthenticationParameters createAuthenticationParameters(JSONObject jsonReq, Authenticator authenticator) {
		JSONObject clientDataJSONobject = base64ToJSON(jsonReq.getString("clientDataJSON"));
		Origin origin = new Origin(clientDataJSONobject.getString("origin"));
		String rpId = jsonReq.getString("rpId");
		byte[] challengeBytes = base64UrlToBytes(clientDataJSONobject.getString("challenge"));
		Challenge challenge = new DefaultChallenge(challengeBytes);
		byte[] tokenBindingId = null;
		ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);
		boolean userVerificationRequired = true;
		boolean userPresenceRequired = true;
		List<String> expectedExtensionIds = Collections.emptyList();
		return new AuthenticationParameters(serverProperty, authenticator, userVerificationRequired, userPresenceRequired, expectedExtensionIds);
	}

	public AuthenticationData validateAuthention(JSONObject jsonReq, AuthenticationRequest authenticationRequest, AuthenticationParameters authenticationParameters) throws JSONException, IOException {
		AuthenticationData	authenticationData = webAuthnManager.parse(authenticationRequest);	
		return webAuthnManager.validate(authenticationData, authenticationParameters);		
	}
}
