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
	
	@SuppressWarnings("unchecked")
	private Config() {
		login = true;
		default_account = new Account("anonymous", "", "", new String[] {"admin"});
		this.jwsConfigs = ReadonlyList.EMPTY;
		this.http_port = 8080;
	}
	
	public Config(boolean login, Account default_account, ReadonlyList<JwsConfig> jwsConfigs, int http_port) {
		this.login = login;
		this.default_account = default_account;
		this.jwsConfigs = jwsConfigs;
		this.http_port = http_port;
	}
	
	public static Config ofYAML(YamlMap yamlMap) {
		boolean login = yamlMap.optBoolean("login", DEFAULT.login);
		Account default_account = DEFAULT.default_account;
		int http_port = yamlMap.optInt("http_port", DEFAULT.http_port);
		
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
		
		return new Config(login, default_account, jwsConfigs.readonlyWeakView(), http_port);
	}

}
