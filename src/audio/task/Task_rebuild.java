package audio.task;

@Description("Clear sample database and traverse root_path with reading all YAML files and filling sample database.")
public class Task_rebuild extends Task {

	@Override
	public void run() {
		ctx.broker.sampleManager().refresh(true);		
	}
}
