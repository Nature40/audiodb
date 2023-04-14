package audio.server.api;

import java.io.IOException;
import java.util.BitSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.Web;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import audio.Account;
import audio.AudioProjectConfig;
import audio.Broker;

public class IdentityHandler extends AbstractHandler {
	
	private final Broker broker;

	public IdentityHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");
		String authentication = (String) session.getAttribute("authentication");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		String[] roleNames = broker.roleManager().getRoleNames(roleBits);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("authentication");
		json.value(authentication);
		json.key("user");
		json.value(account.username);
		json.key("roles");
		json.value(roleNames);
		json.key("audio_config");
		json.object();
		AudioProjectConfig config = broker.config().audioConfig;
		json.key("player_spectrum_threshold");
		json.value(config.player_spectrum_threshold);
		json.key("player_playbackRate");
		json.value(config.player_playbackRate);
		json.key("player_preservesPitch");
		json.value(config.player_preservesPitch);
		json.key("player_overwriteSamplingRate");
		json.value(config.player_overwriteSamplingRate);
		json.key("player_samplingRate");
		json.value(config.player_samplingRate);
		json.endObject(); // audio_config
		json.key("salt");
		json.value(broker.accountManager().salt());		
		json.endObject(); // full JSON
	}
}
