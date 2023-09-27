package audio.task;

import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Refresh ordered sample.")
@Role("admin")
public class Task_audio_sample_storage_refresh_ordered_sample extends Task {
	@Override
	public void run() {
		ctx.broker.sampleStorage().tlSampleStorageConnector.get().refreshOrderedSample();
	}
}
