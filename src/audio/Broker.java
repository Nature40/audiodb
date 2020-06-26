package audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import util.yaml.YamlMap;

public class Broker {
	private static final Logger log = LogManager.getLogger();
	
	private Config config;
	private RoleManager roleManager;
	private AccountManager accountManager;
	private LabelDefinitions labelDefinitions;
	private Samples samples;
	private WebAuthn webAuthn;
	
	public Broker() {		
	}
	
	public Config config() {
		return config == null ? loadConfig() : config;
	}
	
	private synchronized Config loadConfig() {
		if(config == null) {
			File file = new File("config.yaml");
			if(file.exists()) {
				try {
				InputStream in = new FileInputStream(file);
				YamlMap yamlMap = YamlMap.ofObject(new Yaml().load(in));
				config = Config.ofYAML(yamlMap);
				} catch(Exception e) {
					config = Config.DEFAULT;
					log.error("error in config, set config to default: " + e);
					e.printStackTrace();
				}
			} else {
				log.info("no config found: config.yaml file missing");
				config = Config.DEFAULT;
			}
		}
		return config;
	}
	
	public RoleManager roleManager() {
		return roleManager == null ? loadRoleManager() : roleManager;
	}

	private synchronized RoleManager loadRoleManager() {
		if(roleManager == null) {
			roleManager = new RoleManager();
		}
		return roleManager;
	}
	
	public AccountManager accountManager() {
		return accountManager == null ? loadAccountManager() : accountManager;
	}

	private synchronized AccountManager loadAccountManager() {
		if(accountManager == null) {
			accountManager = new AccountManager(Paths.get("accounts.yaml"));
		}
		return accountManager;
	}
	
	public LabelDefinitions labelDefinitions() {
		return labelDefinitions == null ? loadLabelDefinitions() : labelDefinitions;
	}

	private synchronized LabelDefinitions loadLabelDefinitions() {
		if(labelDefinitions == null) {
			labelDefinitions = new LabelDefinitions(Paths.get("label_definitions.yaml"));
		}
		return labelDefinitions;
	}
	
	public Samples samples() {
		return samples == null ? loadSamples() : samples;
	}

	private synchronized Samples loadSamples() {
		if(samples == null) {
			samples = new Samples();
		}
		return samples;
	}
	
	public WebAuthn webAuthn() {
		return webAuthn == null ? loadWebAuthn() : webAuthn;
	}

	private synchronized WebAuthn loadWebAuthn() {
		if(webAuthn == null) {
			webAuthn = new WebAuthn();
		}
		return webAuthn;
	}

}
