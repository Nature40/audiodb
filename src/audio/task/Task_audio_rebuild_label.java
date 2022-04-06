package audio.task;

import task.Description;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Clear label database and traverse all entries in sample database to fill label database and write a set of CSV files to output folder containing labeling entries and statistics.")
public class Task_audio_rebuild_label extends Task {

	@Override
	public void run() {
		ctx.broker.labelStore().rebuild();		
	}
}
