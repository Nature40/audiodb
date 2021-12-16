package audio.server.api;

import java.io.IOException;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LogoutHandler extends AbstractHandler {

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
