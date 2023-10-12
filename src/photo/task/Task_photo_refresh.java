package photo.task;

import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("photo")
@Description("Traverse root_path and check for changed or added or removed YAML files to update photo database.")
@Role("admin")
public class Task_photo_refresh extends Task {

	@Override
	public void run() {
		ctx.broker.photodb2().refresh();
	}
}
