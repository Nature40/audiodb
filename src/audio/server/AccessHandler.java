package audio.server;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import audio.Account;
import audio.Broker;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import util.TemplateUtil;
import util.Web;
import util.collections.vec.Vec;

public class AccessHandler extends AbstractHandler {

	private final Broker broker;

	public AccessHandler(Broker broker) throws IOException {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if(baseRequest.getSession(false) == null) {
			if(broker.config().login) {
				baseRequest.setHandled(true);
				response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
				response.setHeader("Authentication", "required");
				String server_nonce = broker.accountManager().createServerNonce();
				HashMap<String, Object> ctx = new HashMap<>();
				ctx.put("server_nonce", server_nonce);
				ctx.put("salt", broker.accountManager().salt());

				String reqUrl = request.getRequestURL().toString();
				String reqUrlQs = request.getQueryString();
				Logger.info("reqUrlQs " + reqUrlQs);
				String req = reqUrl;
				if(reqUrlQs != null) {
					req += '?' + reqUrlQs;
				}
				Vec<Map<String, Object>> jwsList = new Vec<Map<String, Object>>();
				for(audio.JwsConfig jwsConfig:broker.config().jwsConfigs) {
					String clientJws = Jwts.builder()
							.setPayload(req)
							.setHeaderParam(JwsHeader.KEY_ID, jwsConfig.client_key_id)
							.signWith(JwsHandler.stringToPrivateKey(jwsConfig.client_private_key))
							.compact();					
					String redirect_target = jwsConfig.provider_url + "?jws="+clientJws;

					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("redirect_target", redirect_target);
					map.put("link_text", jwsConfig.link_text);
					map.put("link_description", jwsConfig.link_description);
					jwsList.add(map);
				}
				ctx.put("jws", jwsList);
				ctx.put("jws_section", !jwsList.isEmpty());
				response.setContentType(Web.MIME_HTML);
				TemplateUtil.getTemplate("login.mustache", true).execute(ctx, response.getWriter());
			} else {
				HttpSession session = request.getSession(true);
				injectSameSite(response);				
				Account account = broker.config().default_account;
				session.setAttribute("authentication", "no");
				session.setAttribute("account", account);
				session.setAttribute("roles", broker.roleManager().getRoleBits(account.roles));
			}
		}
	}

	public static void injectSameSite(HttpServletResponse response) {
		Collection<String> headernames = response.getHeaders("Set-Cookie");
		if(headernames.isEmpty()) {
			return;
		}
		//String[] headers = headernames.stream().map(s -> s + ";SameSite=Strict").toArray(String[]::new); // cookies are not sent on same site redirects if SameSite=Strict 
		String[] headers = headernames.stream().map(s -> s + ";SameSite=Lax").toArray(String[]::new);
		boolean isFirst = true;
		for(String header: headers) {
			if(isFirst) {
				response.setHeader("Set-Cookie", header);
				isFirst = false;
			} else {
				response.addHeader("Set-Cookie", header);
			}
		}
	}
}
