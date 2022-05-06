package task;

import org.json.JSONObject;

import audio.Account;
import audio.Broker;

public class Ctx {

	public final Descriptor descriptor;
	public final JSONObject json;
	public final String id;
	public final Broker broker;
	public final Account account;

	public Ctx(Descriptor descriptor, JSONObject json, String id, Broker broker, Account account) {
		this.descriptor = descriptor;
		this.json = json;
		this.id = id;
		this.broker = broker;
		this.account = account;
	}
}