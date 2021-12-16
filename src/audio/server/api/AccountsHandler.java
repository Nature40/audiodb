package audio.server.api;

import java.io.IOException;
import java.util.BitSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import audio.Account;
import audio.Broker;
import audio.Role;

public class AccountsHandler extends AbstractHandler {

	private final Broker broker;

	public AccountsHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			baseRequest.setHandled(true);
			switch(baseRequest.getMethod()) {
			case "POST":
				handlePOST(target, baseRequest, request, response);
				break;
			default: {
				String errorText = "unknown method in " + "accounts: " + baseRequest.getMethod();
				Logger.error(errorText );
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain");
				response.getWriter().print(errorText);		
			}
			}
		}
		catch(Exception e) {
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("application/json");
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("error");
			json.value(e.getMessage());
			json.endObject();
		}
	}

	public void handlePOST(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession(false);
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readOnly.checkHasNot(roleBits);
		
		Logger.info("accounts");		

		JSONObject jsonReq = new JSONObject(new JSONTokener(baseRequest.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		for (int i = 0; i < jsonActionsLen; i++) {
			JSONObject jsonAction = jsonActions.getJSONObject(i);
			String actionName = jsonAction.getString("action");
			switch(actionName) {
			case "create_account": {
				broker.roleManager().role_create_account.checkHas(roleBits);
				Logger.info("create_account action");
				String user = jsonAction.getString("user");
				String hash = jsonAction.getString("hash");
				Account account = Account.ofHash(user, hash.getBytes(), new String[] {});
				broker.accountManager().addAccount(account, true);
				break;
			}				
			default:
				throw new RuntimeException("unknown action: " + actionName);
			}
		}

		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("massage");
		json.value("ok");
		json.endObject();
	}

}
