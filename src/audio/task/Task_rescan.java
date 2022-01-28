package audio.task;

public class Task_rescan extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().rescan(true);		
	}
}
