package audio.task;

import task.Cancelable;
import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Infinite running task. For testing purposes.")
@Cancelable
@Role("admin")
public class Task_infinite extends Task {
	
	private long cnt = 0;

	@Override
	public void run() {
		while(true) {
			setMessage("round " + cnt);
			if(isSoftCanceled()) {
				throw new RuntimeException("canceled");
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			cnt++;
		}
	}
}
