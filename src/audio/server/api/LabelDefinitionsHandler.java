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

import audio.Broker;
import audio.LabelDefinition;
import util.Web;
import util.collections.vec.Vec;

public class LabelDefinitionsHandler extends AbstractHandler {

	private final Broker broker;

	public LabelDefinitionsHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			baseRequest.setHandled(true);
			switch(baseRequest.getMethod()) {
			case "GET":
				handleGET(target, baseRequest, request, response);
				break;
			case "POST":
				handlePOST(target, baseRequest, request, response);
				break;
			default: {
				String errorText = "unknown method in " + "label_definitions: " + baseRequest.getMethod();
				Logger.error(errorText);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain");
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

	public void handleGET(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("label_definitions");
		json.array();		
		broker.labelDefinitions().forEach(ld->{
			ld.toJSON(json);
		});
		json.endArray();
		json.endObject();
	}
	
	public void handlePOST(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession(false);
		BitSet roleBits = (BitSet) session.getAttribute("roles");
		broker.roleManager().role_readOnly.checkHasNot(roleBits);
		
		JSONObject jsonReq = new JSONObject(new JSONTokener(baseRequest.getReader()));
		JSONArray jsonLabelDefinitions = jsonReq.getJSONArray("label_definitions");
		int jsonLabelDefinitionsLen = jsonLabelDefinitions.length();
		Vec<LabelDefinition> vec = new Vec<LabelDefinition>();
		for (int i = 0; i < jsonLabelDefinitionsLen; i++) {
			JSONObject jsonLabelDefinition = jsonLabelDefinitions.getJSONObject(i);
			LabelDefinition labelDefinition = LabelDefinition.ofJSON(jsonLabelDefinition);
			vec.add(labelDefinition);
			broker.labelDefinitions().replace(vec);
		}

		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("massage");
		json.value("ok");
		json.endObject();
	}

}
