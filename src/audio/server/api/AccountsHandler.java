package audio.server.api;

import java.io.IOException;
import java.util.BitSet;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.tinylog.Logger;

import audio.Account;
import audio.Broker;
import audio.role.Role;
import audio.role.RoleManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.Web;
import util.collections.vec.Vec;

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
			case "GET":
				handleGET(target, baseRequest, request, response);
				break;				
			default: {
				String errorText = "unknown method in " + "accounts: " + baseRequest.getMethod();
				Logger.error(errorText );
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType(Web.MIME_TEXT);
				response.getWriter().print(errorText);		
			}
			}
		}
		catch(Exception e) {
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("error");
			json.value(e.getMessage());
			json.endObject();
		}
	}

	public void handlePOST(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession(false);
		Account sessionAccount = (Account) session.getAttribute("account");
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		RoleManager roleManager = broker.roleManager();
		roleManager.role_readOnly.checkHasNot(roleBits);

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
				if(user.isBlank()) {
					throw new RuntimeException("invalid user name");
				}
				String hash = jsonAction.getString("hash");
				Vec<String> vec = new Vec<String>();
				if(jsonAction.has("roles")) {
					JSONArray jsonRoles = jsonAction.getJSONArray("roles");					
					for(int r = 0; r < jsonRoles.length(); r++) {
						String roleName = jsonRoles.getString(r);
						Role role = roleManager.getRole(roleName);
						if(role != null && role.has(roleBits)) {
							vec.add(role.name);
						}
					}
				}
				Account account = Account.ofHash(user, hash.getBytes(), vec.toArray(String[]::new));
				broker.accountManager().addAccount(account, true);
				break;
			}
			case "delete_account": {
				broker.roleManager().role_manage_account.checkHas(roleBits);
				Logger.info("delete_account action");
				String user = jsonAction.getString("user");
				if(user.isBlank()) {
					throw new RuntimeException("invalid user name");
				}
				Account account = broker.accountManager().getAccount(user);
				if(account == null) {
					throw new RuntimeException("Account not found.");
				}
				broker.accountManager().removeAccount(account, true);				
				break;
			}
			case "edit_account": {
				broker.roleManager().role_manage_account.checkHas(roleBits);
				Logger.info("edit_account action");
				String user = jsonAction.getString("user");
				if(user.isBlank()) {
					throw new RuntimeException("invalid user name");
				}
				Account account = broker.accountManager().getAccount(user);
				if(account == null) {
					throw new RuntimeException("Account not found.");
				}
				
				byte[] hash_bytes = jsonAction.has("hash") ? jsonAction.getString("hash").getBytes() : account.hash_bytes;
				Vec<String> vec = new Vec<String>();
				if(jsonAction.has("roles")) {
					JSONArray jsonRoles = jsonAction.getJSONArray("roles");					
					for(int r = 0; r < jsonRoles.length(); r++) {
						String roleName = jsonRoles.getString(r);
						Role role = roleManager.getRole(roleName);
						if(role != null && (role.has(roleBits) || roleManager.isLoweringRole(role))) {
							vec.add(role.name);
						}
					}
				}
				Account accountEdited = Account.ofHash(user, hash_bytes, vec.toArray(String[]::new));
				if(account.webAuthnAccount() != null) {
					accountEdited = Account.withWebAuthn(accountEdited, account.webAuthnAccount());
				}
				broker.accountManager().setAccount(accountEdited, true);			
				break;
			}
			case "change_password": {
				Logger.info("change_password action");
				//sessionAccount
				String current_hash = jsonAction.getString("current_hash");
				String new_hash = jsonAction.getString("new_hash");
				sessionAccount.checkHash(current_hash.getBytes());
				Account accountEdited = Account.withHash(sessionAccount, new_hash.getBytes());
				broker.accountManager().setAccount(accountEdited, true);			
				break;
			}
			default:
				throw new RuntimeException("unknown action: " + actionName);
			}
		}

		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("massage");
		json.value("ok");
		json.endObject();
	}

	public void handleGET(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession(false);
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().roleMask_list_account.checkIntersects(roleBits);

		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("accounts");
		json.array();
		broker.accountManager().forEach(account -> {
			json.object();
			json.key("name");
			json.value(account.username);
			json.key("roles");
			json.value(broker.roleManager().getRoleNames(broker.roleManager().getRoleBits(account.roles)));
			json.endObject();
		});
		json.endArray();
		json.key("salt");
		json.value(broker.accountManager().salt());
		json.endObject();
	}	

}
