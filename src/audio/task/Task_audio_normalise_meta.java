package audio.task;

import task.Cancelable;
import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Normalise metadata.")
@Cancelable
@Role("admin")
public class Task_audio_normalise_meta extends Task {

	@Override
	public void run() {
		if(isSoftCanceled()) {
			throw new RuntimeException("canceled");
		}

		ctx.broker.sampleManager().forEach(sample -> {
			if(isSoftCanceled()) {
				throw new RuntimeException("canceled");
			}
			sample.normaliseMeta();
		});
	}
}
