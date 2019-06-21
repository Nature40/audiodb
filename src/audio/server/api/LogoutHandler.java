package audio.server.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class LogoutHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		HttpSession session = baseRequest.getSession(false);
		if(session != null) {
			session.invalidate();
		}
		baseRequest.setHandled(true);
		response.setHeader(HttpHeader.LOCATION.asString(), "/");
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.setContentLength(0);		
	}
}
