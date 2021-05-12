package audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.collections.ReadonlyList;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class Config {
	private static final Logger log = LogManager.getLogger();
	
	public static final Config DEFAULT = new Config();

	public final boolean login;
	public final Account default_account;	
	public final ReadonlyList<JwsConfig> jwsConfigs;
	public final int http_port;
	public final int https_port;
	public final String keystore_path;
	public final String keystore_password;
	public final String photo_root_path;
	
	@SuppressWarnings("unchecked")
	private Config() {
		login = true;
		default_account = Account.ofPassword("anonymous", "", "", new String[] {"admin"});
		this.jwsConfigs = ReadonlyList.EMPTY;
		this.http_port = 8080;
		this.https_port = 8000;
		this.keystore_path = "keystore.jks";
		this.keystore_password = "";
		this.photo_root_path = "photo_data";
	}
	
	public Config(boolean login, Account default_account, ReadonlyList<JwsConfig> jwsConfigs, int http_port, int https_port, String keystore_path, String keystore_password, String photo_root_path) {
		this.login = login;
		this.default_account = default_account;
		this.jwsConfigs = jwsConfigs;
		this.http_port = http_port;
		this.https_port = https_port;
		this.keystore_path = keystore_path;
		this.keystore_password = keystore_password;
		this.photo_root_path = photo_root_path;
	}
	
	public static Config ofYAML(YamlMap yamlMap) {
		boolean login = yamlMap.optBoolean("login", DEFAULT.login);
		Account default_account = DEFAULT.default_account;
		int http_port = yamlMap.optInt("http_port", DEFAULT.http_port);
		int https_port = yamlMap.optInt("https_port", DEFAULT.https_port);
		String keystore_path = yamlMap.optString("keystore_path", DEFAULT.keystore_path);
		String keystore_password = yamlMap.optString("keystore_password", DEFAULT.keystore_password);
		String photo_root_path = yamlMap.optString("photo_root_path", DEFAULT.photo_root_path);
		
		Vec<JwsConfig> jwsConfigs = new Vec<JwsConfig>();
		
		try {
			for(YamlMap yamlJws:yamlMap.optList("jws").asMaps()) {
				try {
					JwsConfig jwsConfig = JwsConfig.ofYAML(yamlJws);
					jwsConfigs.add(jwsConfig);
				} catch (Exception e) {
					log.warn(e);
				}
			}
		} catch (Exception e) {
			log.warn(e);
		}
		log.info("photo_root_path: |" + photo_root_path + "|");
		return new Config(login, default_account, jwsConfigs.readonlyWeakView(), http_port, https_port, keystore_path, keystore_password, photo_root_path);
	}
	
	public boolean enableHttps() {
		return !keystore_password.isEmpty();
	}

}
