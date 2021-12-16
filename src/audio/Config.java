package audio;


import org.tinylog.Logger;

import photo2.PhotoConfig;
import util.collections.ReadonlyList;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class Config {
	
	public static final Config DEFAULT = new Config();

	public final boolean login;
	public final Account default_account;	
	public final ReadonlyList<JwsConfig> jwsConfigs;
	public final int http_port;
	public final int https_port;
	public final String keystore_path;
	public final String keystore_password;
	//public final AudioConfig audioConfig;
	public final AudioProjectConfig audioConfig;
	public final PhotoConfig photoConfig;
	
	@SuppressWarnings("unchecked")
	private Config() {
		login = true;
		default_account = Account.ofPassword("anonymous", "", "", new String[] {"admin"});
		this.jwsConfigs = ReadonlyList.EMPTY;
		this.http_port = 8080;
		this.https_port = 8000;
		this.keystore_path = "keystore.jks";
		this.keystore_password = "";
		//this.audioConfig = AudioConfig.ofYAML(YamlMap.EMPTY_MAP);
		this.audioConfig = AudioProjectConfig.DEFAULT;
		this.photoConfig = PhotoConfig.ofYAML(YamlMap.EMPTY_MAP);
	}
	
	public Config(boolean login, Account default_account, ReadonlyList<JwsConfig> jwsConfigs, int http_port, int https_port, String keystore_path, String keystore_password, AudioProjectConfig audioConfig, PhotoConfig photoConfig) {
		this.login = login;
		this.default_account = default_account;
		this.jwsConfigs = jwsConfigs;
		this.http_port = http_port;
		this.https_port = https_port;
		this.keystore_path = keystore_path;
		this.keystore_password = keystore_password;
		this.audioConfig = audioConfig;
		this.photoConfig = photoConfig;
	}
	
	public static Config ofYAML(YamlMap yamlMap) {
		boolean login = yamlMap.optBoolean("login", DEFAULT.login);
		Account default_account = DEFAULT.default_account;
		int http_port = yamlMap.optInt("http_port", DEFAULT.http_port);
		int https_port = yamlMap.optInt("https_port", DEFAULT.https_port);
		String keystore_path = yamlMap.optString("keystore_path", DEFAULT.keystore_path);
		String keystore_password = yamlMap.optString("keystore_password", DEFAULT.keystore_password);
		
		Vec<JwsConfig> jwsConfigs = new Vec<JwsConfig>();
		
		try {
			for(YamlMap yamlJws:yamlMap.optList("jws").asMaps()) {
				try {
					JwsConfig jwsConfig = JwsConfig.ofYAML(yamlJws);
					jwsConfigs.add(jwsConfig);
				} catch (Exception e) {
					Logger.warn(e);
				}
			}
		} catch (Exception e) {
			Logger.warn(e);
		}
		
		//AudioConfig audioConfig = AudioConfig.ofYAML(yamlMap.optMap("audio"));
		AudioProjectConfig audioConfig = new AudioProjectConfig(new AudioProjectConfig.Builder(yamlMap.optMap("audio")));
		PhotoConfig photoConfig = PhotoConfig.ofYAML(yamlMap.optMap("photo"));
		
		return new Config(login, default_account, jwsConfigs.readonlyWeakView(), http_port, https_port, keystore_path, keystore_password, audioConfig, photoConfig);
	}
	
	public boolean enableHttps() {
		return !keystore_password.isEmpty();
	}
}
