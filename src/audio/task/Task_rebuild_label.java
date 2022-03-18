package audio.task;

@Description("Clear label database and traverse all entries in sample database to fill label database and write a set of CSV files to output folder containing labeling entries and statistics.")
public class Task_rebuild_label extends Task {

	@Override
	public void run() {
		ctx.broker.labelStore().rebuild();		
	}
}
