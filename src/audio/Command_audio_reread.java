package audio;

@Deprecated
public class Command_audio_reread implements Command {

	@Override
	public void execute(String command, String[] params) {
		if(params.length != 0) {
			throw new RuntimeException("no paramer allowed");
		}
		Broker broker = new Broker();
		broker.sampleManager().rescan(true);
	}
}