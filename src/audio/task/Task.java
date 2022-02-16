package audio.task;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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
	public long tend = -1;
	private volatile String message = "";
	private BoundedLog boundedLog = new BoundedLog(1000);

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
		} finally {
			tend = System.currentTimeMillis();
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
	
	public void setMessage(String message) {
		this.message = message;
		boundedLog.add(message);
	}
	
	public String getMessage() {
		return message;
	}
	
	public void foreachLog(Consumer<String> consumer) {
		boundedLog.foreach(consumer);	
	}
	
	public long getCurrentTimeOrEnd() {
		return tend < 0 ? System.currentTimeMillis() : tend;
	}
	
	public long getRuntime() {
		return getCurrentTimeOrEnd() - tstart;
	}

	public String getRuntimeText() {
		long tdiff = getRuntime();
		//Duration duration = Duration.ofMillis(tdiff);
		//return duration.toString();
		long seconds = tdiff / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long secondsPart = seconds % 60;
		long minutesPart = minutes % 60;
		String s = hours + ":" +  String.format("%02d", minutesPart) + ":" + String.format("%02d", secondsPart);
		return s;
	}
}
