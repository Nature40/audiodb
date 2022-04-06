package audio.task;

import task.Description;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Traverse root_path and check for changed or added or removed YAML files to update sample database.")
public class Task_audio_refresh extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().refresh(false);		
	}
}
