package audio.task;

import task.Cancelable;
import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Create hashs of all audio files, if missing.")
@Cancelable
@Role("admin")
public class Task_audio_create_file_hashs extends Task {

	@Override
	public void run() {
		long[] counter = new long[] {0};
		ctx.broker.sampleManager().forEach(sample -> {
			if(isSoftCanceled()) {
				throw new RuntimeException("canceled");
			}
			String hash = sample.getFileHash(true);
			//Logger.info(hash);
			counter[0]++;
			if(counter[0]%10 == 0) {
				setMessage(counter[0] + " files hashed   current  "  + sample.samplePath);
			}
		});
		setMessage(counter[0] + " files hashed   done.");
	}
}
