package audio.server;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;

import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.json.JSONWriter;

import audio.Broker;
import audio.server.api.AccountHandler;
import audio.server.api.AccountsHandler;
import audio.server.api.LabelDefinitionsHandler;
import audio.server.api.LoginHandler;
import audio.server.api.LogoutHandler;

public class Webserver {
	static final Logger log = LogManager.getLogger();

	static class HelloWorld extends AbstractHandler {
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			response.setContentType("text/html; charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("<h1>Hello World</h1>");
			baseRequest.setHandled(true);
		}
	}

	public static void main(String[] srgs) throws Exception {

		Broker broker = new Broker();

		log.info("starting server");

		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setSendDateHeader(false);
		httpConfiguration.setSendXPoweredBy(false);
		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);		
		Server server = new Server();
		ServerConnector httpServerConnector = new ServerConnector(server, httpConnectionFactory);
		httpServerConnector.setPort(broker.config().http_port);
		server.setConnectors(new Connector[] {httpServerConnector});

		DefaultSessionIdManager sessionIdManager = new DefaultSessionIdManager(server);
		SessionHandler sessionHandler = new SessionHandler();
		sessionHandler.setSessionIdManager(sessionIdManager);
		sessionHandler.setHttpOnly(true);
		sessionHandler.setSessionCookie("session");
		SessionCookieConfig sessionCokkieConfig = sessionHandler.getSessionCookieConfig();
		sessionCokkieConfig.setPath("/");

		HandlerList handlerList = new HandlerList();
		handlerList.addHandler(sessionHandler);
		handlerList.addHandler(new InjectHandler());
		if(broker.config().login) {
			handlerList.addHandler(createContext("/login", true, new LoginHandler(broker)));
		}
		handlerList.addHandler(new JwsHandler(broker));
		handlerList.addHandler(new AccessHandler(broker));
		handlerList.addHandler(createContext("/spectrum", true, new SpectrumHandler()));
		handlerList.addHandler(createContext("/audio", true, audio()));
		handlerList.addHandler(createContext("/samples", true, new SamplesHandler()));
		handlerList.addHandler(createContext("/account", true, new AccountHandler(broker)));
		handlerList.addHandler(createContext("/accounts", true, new AccountsHandler(broker)));
		handlerList.addHandler(createContext("/label_definitions", true, new LabelDefinitionsHandler(broker)));
		handlerList.addHandler(createContext("/web", true, webcontent()));
		handlerList.addHandler(new BaseRedirector("/web/app/"));
		//if(broker.config().login) {
			handlerList.addHandler(createContext("/logout", true, new LogoutHandler()));
		//}
		handlerList.addHandler(new NoContentHandler());		
		server.setHandler(handlerList);
		//SessionHandler sessionHandler = new SessionHandler();
		//sessionHandler.setHandler(createContext("", true, handlerList));
		//server.setHandler(sessionHandler);
		server.start();
		server.join();
		log.info("server stopped");
	}

	private static ContextHandler createContext(String contextPath, boolean allowNullPathInfo, Handler handler) {
		ContextHandler context = new ContextHandler(contextPath);
		context.setAllowNullPathInfo(allowNullPathInfo);
		context.setHandler(handler);
		return context;
	}

	private static ResourceHandler webcontent() {
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setPathInfoOnly(true);
		resourceHandler.setResourceBase("webcontent");
		resourceHandler.setDirectoriesListed(false);
		resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
		resourceHandler.setPrecompressedFormats(new CompressedContentFormat[]{CompressedContentFormat.GZIP});
		return resourceHandler;
	}

	private static ResourceHandler audio() {
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setResourceBase("data");
		resourceHandler.setDirectoriesListed(false);
		resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
		return resourceHandler;
	}

	private static class InjectHandler extends AbstractHandler {
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			response.setHeader("X-Robots-Tag", "noindex, nofollow");
			if("127.0.0.1".equals(baseRequest.getRemoteAddr())) {
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setHeader("Access-Control-Allow-Headers", "content-type");
				if(baseRequest.getMethod().equals("OPTIONS")) {
					baseRequest.setHandled(true);
				}
			}
		}
	}

	private static class SamplesHandler extends AbstractHandler {

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			baseRequest.setHandled(true);
			response.setContentType("application/json");
			JSONWriter json = new JSONWriter(response.getWriter());

			json.object();
			json.key("samples");
			json.array();
			Path root = Paths.get("data");
			for(Path path:getPaths(root)) {
				if(path.toFile().isFile()) {
					json.object();
					json.key("name");
					json.value(root.relativize(path).toString());
					json.endObject();
				}
			}

			json.endArray();
			json.endObject();
		}

	}

	private static class NoContentHandler extends AbstractHandler {

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			baseRequest.setHandled(true);
			log.info(target);
			if(target.trim().equalsIgnoreCase("/robots.txt")) {
				response.setContentType("text/plain; charset=utf-8");
				response.getWriter().print("User-agent: * \nDisallow: /\n");
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentType("text/plain; charset=utf-8");
				response.getWriter().print("invalid url");
			}
		}

	}

	public static Path[] getPaths(Path root) throws IOException {
		DirectoryStream<Path> dirStream = Files.newDirectoryStream(root);
		Path[] paths = StreamSupport.stream(dirStream.spliterator(), false)
				.sorted()
				.toArray(Path[]::new);
		dirStream.close();
		return paths;
	}

	public static class BaseRedirector extends AbstractHandler
	{
		private final String redirect_target;
		public BaseRedirector(String redirect_target) {
			this.redirect_target = redirect_target;
		}
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
		{
			if(target.equals("/")) {
				response.setHeader(HttpHeader.LOCATION.asString(), redirect_target);
				response.setStatus(HttpServletResponse.SC_FOUND);
				response.setContentLength(0);
				baseRequest.setHandled(true);
			}
		}
	}
}
