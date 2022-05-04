package audio.task;

import org.tinylog.Logger;

import task.Description;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Create hashs of all audio files, if missing.")
public class Task_audio_create_file_hashs extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().forEach(sample -> {
			String hash = sample.getFileHash(true);
			//Logger.info(hash);
		});
	}
}
