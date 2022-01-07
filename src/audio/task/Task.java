package audio.task;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.tinylog.Logger;

import audio.Broker;
import audio.task.Tasks.Descriptor;

public abstract class Task extends RecursiveAction implements Comparable<Task> {

	public enum State {
		INITIAL,
		READY,
		RUNNING,
		ERROR,
		DONE
	}

	public static class Ctx {

		public final Descriptor descriptor;
		public final JSONObject json;
		public final String id;
		public final Broker broker;

		public Ctx(Descriptor descriptor, JSONObject json, String id, Broker broker) {
			this.descriptor = descriptor;
			this.json = json;
			this.id = id;
			this.broker = broker;
		}
	}
	
	private static final AtomicLong counter = new AtomicLong(0);

	protected Ctx ctx;
	private volatile State state = State.INITIAL;
	public final long cnt = counter.getAndIncrement();
	public final long tstart = System.currentTimeMillis();
	protected volatile String message = "";

	public final void setCtxAndInit(Ctx ctx) throws Exception {
		this.ctx = ctx;
		try {
			init();
			state = State.INITIAL;
		} catch (Exception e) {
			state = State.ERROR;
			throw e;
		}
	}

	protected void init() throws Exception {
	}

	protected abstract void run() throws Exception;

	@Override
	protected final void compute() {		
		try {
			state = State.RUNNING;
			run();
			state = State.DONE;
		} catch (Exception e) {
			state = State.ERROR;
			message = e.getMessage();
			Logger.warn(e);
		}		
	}

	@Override
	public int compareTo(Task o) {
		return Long.compare(this.cnt, o.cnt);
	}
	
	public Ctx geCtx() {
		return ctx;
	}

	public State getState() {
		return state;
	}
	
	public String getMessage() {
		return message;
	}
}
