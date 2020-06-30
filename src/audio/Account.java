package audio;

import java.util.LinkedHashMap;

import com.github.aelstad.keccakj.fips202.SHA3_512;

import util.yaml.YamlMap;

public class Account {
	
	public final String username;
	public final byte[] hash_bytes;
	public final String[] roles;
	private final WebAuthnAccount webAuthnAccount;
	
	private Account(String username, byte[] hash_bytes, String[] roles, WebAuthnAccount webAuthnAccount) {
		this.username = username;
		this.hash_bytes = hash_bytes;
		this.roles = roles;
		this.webAuthnAccount = webAuthnAccount;
	}
	
	public static Account ofPassword(String username, String password, String salt, String[] roles) {
		byte[] hash_bytes = doHash(username, password, salt);
		return new Account(username, hash_bytes, roles, null);
	}
	
	public static Account ofHash(String username, byte[] hash_bytes, String[] roles) {
		return new Account(username, hash_bytes, roles, null);
	}
	
	public static Account ofEmpty(String username, String[] roles) {
		byte[] hash_bytes = new byte[] {};
		return new Account(username, hash_bytes, roles, null);
	}
	
	public static Account withWebAuthn(Account account, WebAuthnAccount webAuthnAccount) {
		return new Account(account.username, account.hash_bytes, account.roles, webAuthnAccount); 
	}
	
	public static byte[] doHash(String username, String password, String salt) {
		byte[] user_bytes = username.getBytes();
		byte[] password_bytes = password.getBytes();
		byte[] salt_bytes = salt.getBytes();	
		SHA3_512 md = new SHA3_512();
		md.update(salt_bytes);
		md.update(user_bytes);
		md.update(salt_bytes);
		md.update(password_bytes);
		md.update(salt_bytes);
		byte[] digest = md.digest();
		return Hex.bytesToHex(digest).getBytes();
	}
	
	public static Account ofYAML(YamlMap yamlMap) {
		String user = yamlMap.getString("user");
		byte[] hash = Hex.hexToBytes(yamlMap.getString("hash"));
		String[] roles = yamlMap.optList("roles").asStringArray();
		WebAuthnAccount webAuthnAccount = null;
		if(yamlMap.contains("WebAuthn")) {
			YamlMap yamlWebAuthn = yamlMap.getMap("WebAuthn");
			webAuthnAccount = WebAuthnAccount.ofYAML(yamlWebAuthn);
		}		
		return new Account(user, hash, roles, webAuthnAccount);
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("user", username);
		map.put("hash", Hex.bytesToHex(hash_bytes));
		map.put("roles", roles);
		if(webAuthnAccount != null) {
			map.put("WebAuthn", webAuthnAccount.toMap());
		}
		return map;
	}
	
	public WebAuthnAccount webAuthnAccount() {
		return webAuthnAccount;
	}


}
