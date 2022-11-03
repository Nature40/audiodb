package audio.task;

import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Clear sample database and traverse root_path with reading all YAML files and filling sample database.")
@Role("admin")
public class Task_audio_rebuild extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().refresh(true);		
	}
}
