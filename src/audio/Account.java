package audio;

import java.util.LinkedHashMap;

import com.github.aelstad.keccakj.fips202.SHA3_512;

import util.yaml.YamlMap;

public class Account {
	
	public final String username;
	public final byte[] hash_bytes;
	public final String[] roles;
	
	public Account(String username, String password, String salt, String[] roles) {
		this.username = username;
		this.hash_bytes = doHash(username, password, salt);
		this.roles = roles;
	}
	
	public Account(String username, byte[] hash_bytes, String[] roles) {
		this.username = username;
		this.hash_bytes = hash_bytes;
		this.roles = roles;
	}
	
	public Account(String username, String[] roles) {
		this.username = username;
		this.hash_bytes = new byte[] {};
		this.roles = roles;
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
		return new Account(user, hash, roles);
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("user", username);
		map.put("hash", Hex.bytesToHex(hash_bytes));
		map.put("roles", roles);
		return map;
	}
}
