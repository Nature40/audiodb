package audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Command_audio_reread implements Command {
	static final Logger log = LogManager.getLogger();

	@Override
	public void execute(String command, String[] params) {
		if(params.length != 0) {
			throw new RuntimeException("no paramer allowed");
		}
		Broker broker = new Broker();
		broker.sampleManager().rescan(true);
	}
}