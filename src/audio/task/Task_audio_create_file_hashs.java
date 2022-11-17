package audio.task;

import task.Cancelable;
import task.Description;
import task.Param;
import task.Role;
import task.Tag;
import task.Task;
import task.Descriptor.Param.Type;

@Tag("audio")
@Description("Create hashs of all audio files, if missing.")
@Param(name = "count_only", type = Type.BOOLEAN, preset = "FALSE", description = "Only count files to hash.")
@Cancelable
@Role("admin")
public class Task_audio_create_file_hashs extends Task {

	@Override
	public void run() {

		boolean count_only = this.ctx.getParamBoolean("count_only");

		if(count_only) {
			long[] counter = new long[] {0, 0, 0};
			ctx.broker.sampleManager().forEach(sample -> {
				if(isSoftCanceled()) {
					throw new RuntimeException("canceled");
				}
				String hash = sample.getFileHash(false);
				counter[0]++;
				if(hash == null) {
					counter[1]++;
				} else {
					counter[2]++;
				}				
				if(counter[0]%10 == 0) {
					setMessage(counter[0] + " files traversed  "  + counter[1] + " files to hash  "  + counter[2] + " files with hash");
				}
			});
			setMessage(counter[0] + " files traversed  "  + counter[1] + " files to hash  "  + counter[2] + " files with hash   done.");
		} else {
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
}
