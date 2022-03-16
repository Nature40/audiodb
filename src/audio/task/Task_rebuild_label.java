package audio.task;

public class Task_rebuild_label extends Task {

	@Override
	public void run() {
		ctx.broker.labelStore().rebuild();		
	}
}
