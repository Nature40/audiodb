package audio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.aelstad.keccakj.fips202.SHA3_512;

import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class AccountManager {
	private static final Logger log = LogManager.getLogger();

	private final Path accountsPath;
	private String salt = null;

	private volatile List<Account> accounts = new ArrayList<Account>();

	ConcurrentHashMap<String, Boolean> serverNonceMap = new ConcurrentHashMap<String, Boolean>();

	public String createServerNonce() {
		String nonce = Nonce.get(8);
		while(serverNonceMap.putIfAbsent(nonce, Boolean.TRUE) != null) {
			nonce = Nonce.get(8);
		}
		return nonce;
	}

	public AccountManager(Path accountsPath) {
		this.accountsPath = accountsPath;
		if(!read()) {
			salt = Nonce.get(8);
			//addAccount(new Account("user", "password", salt, new String[] {"admin"}));
			write();
			if(!read()) {
				throw new RuntimeException("could not create accounts storage file");
			}
		}
	}

	public Account validate(String server_nonce, String client_nonce, String client_hash) {
		List<Account> acc = accounts;
		
		if(!serverNonceMap.replace(server_nonce, Boolean.TRUE, Boolean.FALSE)) {
			return null;
		}

		byte[] server_nonce_bytes = server_nonce.getBytes();
		byte[] client_nonce_bytes = client_nonce.getBytes();

		int accountsLen = acc.size();
		for (int i = 0; i < accountsLen; i++) {
			Account account = acc.get(i);
			SHA3_512 md = new SHA3_512();
			md.update(server_nonce_bytes);
			md.update(client_nonce_bytes);
			md.update(account.hash_bytes);
			md.update(client_nonce_bytes);
			md.update(server_nonce_bytes);
			String server_hash = Hex.bytesToHex(md.digest());
			if(server_hash.equals(client_hash)) {
				log.info("valid: " + account.username);
				return account;
			}
		}
		return null;		
	}

	public synchronized void write() {
		List<Account> acc = accounts;
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		yamlMap.put("salt", salt);
		Vec<Map<String, Object>> vec = new Vec<Map<String, Object>>();
		acc.forEach(account -> vec.add(account.toMap()));
		yamlMap.put("accounts", vec);
		YamlUtil.writeSafeYamlMap(accountsPath, yamlMap);
	}

	public synchronized boolean read() {
		if(!Files.exists(accountsPath)) {
			log.info("no accounts file: " + accountsPath);
			return false;			
		}
		YamlMap yamlMap = YamlUtil.readYamlMap(accountsPath);
		String salt = yamlMap.getString("salt");
		List<YamlMap> accountList = yamlMap.optList("accounts").asMaps();
		List<Account> accounts = accountList.stream().map(m -> Account.ofYAML(m)).collect(Collectors.toList());
		if(salt.length() < 8) {
			throw new RuntimeException("invalid salt");
		}
		this.salt = salt;
		this.accounts = accounts;
		return true;
	}
	
	public String salt() {
		return salt;
	}

	public synchronized void addAccount(Account account) {
		List<Account> accOld = accounts;
		for(Account a:accOld) {
			if(a.username.equals(account.username)) {
				throw new RuntimeException("account already exists: " + account.username);
			}
		}
		ArrayList<Account> acc2 = new ArrayList<Account>(accOld);
		acc2.add(account);
		this.accounts = acc2;
	}

	public Account getAccount(String username) {
		for(Account account:accounts) {
			if(account.username.equals(username)) {
				return account;
			}
		}
		return null;
	}
}
