package audio.task;

import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("compact sample database. Database is closed. AudioDB needs to be restared afterwards to access the database again.")
@Role("admin")
public class Task_audio_compact_sample_storage_db extends Task {
	@Override
	public void run() {
		ctx.broker.sampleStorage().compact();
	}
}
