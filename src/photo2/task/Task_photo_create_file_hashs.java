package photo2.task;

import org.tinylog.Logger;

import photo2.Photo2;
import photo2.PhotoDB2;
import task.Cancelable;
import task.Description;
import task.Tag;
import task.Task;

@Tag("photo")
@Description("Create hashs of all image files, if missing.")
@Cancelable
public class Task_photo_create_file_hashs extends Task {

	@Override
	public void run() {
		long[] counter = new long[] {0};
		PhotoDB2 photodb = ctx.broker.photodb2();
		photodb.foreachProject(config -> {			
			if(isSoftCanceled()) {
				throw new RuntimeException("canceled");
			}
			Logger.info("Hash project: " + config.project);
			photodb.foreachId(config.project, null, id -> {
				Logger.info("Hash ID: " + id);
				if(isSoftCanceled()) {
					throw new RuntimeException("canceled");
				}
				Photo2 photo = photodb.getPhoto2NotLocked(id);
				String hash = photo.getFileHash(true);
				counter[0]++;
				if(counter[0]%10 == 0) {
					setMessage(counter[0] + " files hashed   current  "  + photo.imagePath);
				}
			});
		});
		setMessage(counter[0] + " files hashed   done.");
	}
}
