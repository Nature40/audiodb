package audio.task;

import task.Description;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Regenerate worklists.")
public class Task_audio_refresh_worklists extends Task {

	@Override
	public void run() {
		ctx.broker.worklistStore().refresh();	
	}
}
