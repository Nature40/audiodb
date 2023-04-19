package audio.server;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import audio.Broker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.TemplateUtil;
import util.Web;

public class MustachePage extends AbstractHandler {
	
	protected final Broker broker;	
	private final String mustacheFilename;

	public MustachePage(Broker broker, String mustacheFilename) {
		this.broker = broker;
		this.mustacheFilename = mustacheFilename;
	}	

	@Override
	public final void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		response.setContentType(Web.MIME_HTML);
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		injectContext(ctx);
		TemplateUtil.getTemplate(mustacheFilename, true).execute(ctx, response.getWriter());		
	}
	
	protected void injectContext(HashMap<String, Object> ctx) {		
	}
}
