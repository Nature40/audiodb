package audio.task;

@Description("Traverse root_path and check for changed or added or removed YAML files to update sample database.")
public class Task_refresh extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().refresh(false);		
	}
}
