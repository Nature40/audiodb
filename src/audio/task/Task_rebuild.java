package audio.task;

public class Task_rebuild extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().refresh(true);		
	}
}
