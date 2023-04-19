package audio.server;

import java.util.HashMap;

import audio.Broker;

public class IndexPage extends MustachePage {

	public IndexPage(Broker broker) {
		super(broker, "index.mustache");
	}

	@Override
	protected void injectContext(HashMap<String, Object> ctx) {
		ctx.put("hasAudioProject", broker.config().audioConfig.root_path != null);	
		ctx.put("hasPhotoProject", !broker.config().photoConfig.projectMap.isEmpty());		
	}
}
