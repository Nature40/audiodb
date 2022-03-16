package audio.task;

public class Task_refresh extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().refresh(false);		
	}
}
