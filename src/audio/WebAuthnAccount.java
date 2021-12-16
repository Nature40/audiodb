package audio;

import java.util.LinkedHashMap;


import org.tinylog.Logger;

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.AttestationObjectConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;

import util.yaml.YamlMap;

public class WebAuthnAccount {
	
	private static final AttestationObjectConverter attestationObjectConverter = new AttestationObjectConverter(new ObjectConverter());
	
	private Authenticator authenticator;
	private byte[] attestationObjectBytes;
	
	public WebAuthnAccount(byte[] attestationObjectBytes) {
		this.attestationObjectBytes = attestationObjectBytes;
		AttestationObject attestationObject = attestationObjectConverter.convert(attestationObjectBytes);
		this.authenticator = createAuthenticator(attestationObject);
	}
	
	private static AuthenticatorImpl createAuthenticator(AttestationObject attestationObject) {
		AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = attestationObject.getAuthenticatorData();
		AttestedCredentialData AttestedCredentialData = authenticatorData.getAttestedCredentialData();
		AttestationStatement attestationStatement = attestationObject.getAttestationStatement();
		long signCount = authenticatorData.getSignCount();
		return new AuthenticatorImpl(AttestedCredentialData, attestationStatement, signCount);
	}
	
	public Authenticator authenticator() {
		return authenticator;
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("attestationObjectBytes", attestationObjectBytes);
		return map;
	}

	public static WebAuthnAccount ofYAML(YamlMap yamlWebAuthn) {
		Object object = yamlWebAuthn.getObject("attestationObjectBytes");
		Logger.info(object.getClass().getName());
		byte[] attestationObjectBytes = (byte[]) object;
		return new WebAuthnAccount(attestationObjectBytes);
	}

}
