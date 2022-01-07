package audio.task;

public class Task_refresh_label extends Task {

	@Override
	public void run() {
		ctx.broker.labelStore().rebuild();		
	}
}
