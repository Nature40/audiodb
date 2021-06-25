package audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import audio.labeling.LabelingListManager;
import audio.review.ReviewListManager;
import photo.PhotoDB;
import photo2.PhotoDB2;
import util.yaml.YamlMap;

public class Broker {
	private static final Logger log = LogManager.getLogger();

	private Config config;
	private RoleManager roleManager;
	private AccountManager accountManager;
	private LabelDefinitions labelDefinitions;
	private Samples samples;

	private ReviewListManager reviewListManager;
	private volatile ReviewListManager reviewListManagerVolatile;
	private Object reviewListManagerLock = new Object();
	
	private LabelingListManager labelingListManager;
	private volatile LabelingListManager labelingListManagerVolatile;
	private Object labelingListManagerLock = new Object();

	private WebAuthn webAuthn;

	private PhotoDB photoDB;

	private PhotoDB2 photodb2;
	private volatile PhotoDB2 photodb2Volatile;
	private Object photodb2Lock = new Object();
	
	private SampleManager sampleManager;
	private volatile SampleManager sampleManagerVolatile;
	private Object sampleManagerLock = new Object();

	public Broker() {
		//samples(); // preload sample metadata
		//reviewListManager();  // preload review_list metadata
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
			samples = new Samples(this);
		}
		return samples;
	}

	public ReviewListManager reviewListManager() {		
		return reviewListManager != null ? reviewListManager : loadReviewListManager();
	}

	private ReviewListManager loadReviewListManager() {
		ReviewListManager r = reviewListManagerVolatile;
		if(r != null) {
			return r;
		}
		synchronized (reviewListManagerLock) {
			r = reviewListManagerVolatile;
			if(r != null) {
				return r;
			}
			r = new ReviewListManager(Paths.get("review_lists"));
			r.updateReviewLists(samples());
			reviewListManagerVolatile = r;
			reviewListManager = r;
			return r;
		}
	}

	public LabelingListManager labelingListManager() {		
		return labelingListManager != null ? labelingListManager : loadLabelingListManager();
	}

	private LabelingListManager loadLabelingListManager() {
		LabelingListManager r = labelingListManagerVolatile;
		if(r != null) {
			return r;
		}
		synchronized (labelingListManagerLock) {
			r = labelingListManagerVolatile;
			if(r != null) {
				return r;
			}
			r = new LabelingListManager(Paths.get("labeling_lists"));
			r.updateLabelingLists(samples());
			labelingListManagerVolatile = r;
			labelingListManager = r;
			return r;
		}
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

	public PhotoDB photoDB() {
		return photoDB == null ? loadPhotoDB() : photoDB;
	}

	private synchronized PhotoDB loadPhotoDB() {
		if(photoDB == null) {
			photoDB = new PhotoDB(this);
		}
		return photoDB;
	}	

	public PhotoDB2 photodb2() {		
		return photodb2 != null ? photodb2 : loadPhotoDB2();
	}

	private PhotoDB2 loadPhotoDB2() {
		PhotoDB2 r = photodb2Volatile;
		if(r != null) {
			return r;
		}
		synchronized (photodb2Lock) {
			r = photodb2Volatile;
			if(r != null) {
				return r;
			}
			r = new PhotoDB2(this);
			photodb2Volatile = r;
			photodb2 = r;
			return r;
		}
	}
	
	public SampleManager sampleManager() {		
		return sampleManager != null ? sampleManager : loadSampleManager();
	}

	private SampleManager loadSampleManager() {
		SampleManager r = sampleManagerVolatile;
		if(r != null) {
			return r;
		}
		synchronized (sampleManagerLock) {
			r = sampleManagerVolatile;
			if(r != null) {
				return r;
			}
			r = new SampleManager(this);
			sampleManagerVolatile = r;
			sampleManager = r;
			return r;
		}
	}
}
