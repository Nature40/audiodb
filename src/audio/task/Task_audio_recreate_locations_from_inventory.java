package audio.task;

import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Remove locations and insert locations from inventory.")
@Role("admin")
public class Task_audio_recreate_locations_from_inventory extends Task {

	@Override
	public void run() {
		ctx.broker.sampleStorage().recreateLocationsFromInventory();
	}
}
