package audio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import org.tinylog.Logger;

import com.github.aelstad.keccakj.fips202.SHA3_512;
import com.webauthn4j.authenticator.Authenticator;

import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class AccountManager {

	private final Path accountsPath;
	private String salt = null;

	private volatile Vec<Account> accounts = new Vec<Account>();

	public static final Comparator<byte[]> BYTES_COMPARATOR = new Comparator<byte[]>() {
		@Override
		public int compare(byte[] o1, byte[] o2) {
			int len = o1.length;
			int lenCmp = len - o2.length;
			if(lenCmp != 0) {
				return lenCmp;
			}
			for (int i = 0; i < len; i++) {
				int cmp = o1[i] - o2[i];
				if(cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}		
	};

	private volatile ConcurrentSkipListMap<byte[], Account> webAuthnCredentialIdMap = new ConcurrentSkipListMap<byte[], Account>(BYTES_COMPARATOR);


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
				Logger.info("valid: " + account.username);
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
			Logger.info("no accounts file: " + accountsPath);
			return false;			
		}
		YamlMap yamlMap = YamlUtil.readYamlMap(accountsPath);
		String salt = yamlMap.getString("salt");
		List<YamlMap> accountList = yamlMap.optList("accounts").asMaps();
		Vec<Account> accounts = accountList.stream().map(m -> Account.ofYAML(m)).collect(Vec.collector());
		if(salt.length() < 8) {
			throw new RuntimeException("invalid salt");
		}
		this.salt = salt;
		this.accounts = accounts;
		refreshWebAuthn();
		return true;
	}

	public String salt() {
		return salt;
	}

	public synchronized void addAccount(Account account, boolean write) {
		Vec<Account> accOld = accounts;
		for(Account a:accOld) {
			if(a.username.equals(account.username)) {
				throw new RuntimeException("account already exists: " + account.username);
			}
		}
		Vec<Account> acc2 = accOld.copy();
		acc2.add(account);
		this.accounts = acc2;
		if(write) {
			write();
		}
		refreshWebAuthn();
	}

	public synchronized void removeAccount(Account account, boolean write) {
		Vec<Account> accOld = accounts;
		Vec<Account> acc2 = new Vec<Account>();
		for(Account a:accOld) {
			if(!a.username.equals(account.username)) {
				acc2.add(a);
			}
		}		
		this.accounts = acc2;
		refreshWebAuthn();
		if(write) {
			write();
		}		
	}

	public void setAccount(Account account, boolean write) {
		Vec<Account> accOld = accounts;
		Vec<Account> acc2 = new Vec<Account>();
		for(Account a:accOld) {
			if(!a.username.equals(account.username)) {
				acc2.add(a);
			}
		}		
		acc2.add(account);
		this.accounts = acc2;
		refreshWebAuthn();
		if(write) {
			write();
		}		
	}

	public Account getAccount(String username) {
		for(Account account:accounts) {
			if(account.username.equals(username)) {
				return account;
			}
		}
		return null;
	}

	private void refreshWebAuthn() {
		ConcurrentSkipListMap<byte[], Account> m = new ConcurrentSkipListMap<byte[], Account>(BYTES_COMPARATOR);
		for(Account account:accounts) {
			WebAuthnAccount webAuthnAccount = account.webAuthnAccount();
			if(webAuthnAccount != null) {
				Authenticator authenticator = webAuthnAccount.authenticator();
				byte[] credentialId = authenticator.getAttestedCredentialData().getCredentialId();
				m.put(credentialId, account);
				//Logger.info("Aaguid " + authenticator.getAttestedCredentialData().getAaguid());
				//Logger.info("COSEKey " + authenticator.getAttestedCredentialData().getCOSEKey());
				//Logger.info("CredentialId " + WebAuthn.bytesToBase64(credentialId) + " for " + account.username);
			}
		}
		webAuthnCredentialIdMap = m;
	}



	public Account loadByCredentialId(byte[] credentialId) {
		return webAuthnCredentialIdMap.get(credentialId);
	}

	public void forEach(Consumer<Account> action) {
		accounts.forEach(action);		
	}




}
