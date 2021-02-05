package audio.server;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.StreamSupport;

import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.OptionalSslConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import audio.Broker;
import audio.Config;
import audio.server.api.AccountHandler;
import audio.server.api.AccountsHandler;
import audio.server.api.IdentityHandler;
import audio.server.api.LabelDefinitionsHandler;
import audio.server.api.LoginHandler;
import audio.server.api.LoginWebAuthnHandler;
import audio.server.api.LogoutHandler;
import audio.server.api.QueryHandler;
import audio.server.api.ReviewStatisticsDetailedHandler;
import audio.server.api.ReviewStatisticsHandler;
import audio.server.api.Review_listsHandler;
import audio.server.api.SamplesHandler;
import audio.server.api.TimeseriesHandler;
import audio.server.api.WebAuthnHandler;
import photo.api.PhotoDBHandler;

public class Webserver {
	static final Logger log = LogManager.getLogger();

	private static HttpConfiguration createBaseHttpConfiguration() {
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setSendDateHeader(false);
		httpConfiguration.setSendXPoweredBy(false);
		return httpConfiguration;
	}

	private static ServerConnector createHttpConnector(Server server, int http_port) {
		HttpConfiguration httpConfiguration = createBaseHttpConfiguration();
		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);		
		ServerConnector httpServerConnector = new ServerConnector(server, httpConnectionFactory);
		httpServerConnector.setPort(http_port);
		return httpServerConnector;
	}

	private static HttpConfiguration createHttpsConfiguration(int https_port) {
		HttpConfiguration httpsConfiguration = createBaseHttpConfiguration();
		httpsConfiguration.setSecureScheme("https");
		httpsConfiguration.setSecurePort(https_port);
		return httpsConfiguration;
	}




	private static ServerConnector createHttpsConnector(Server server, int https_port, String keystore_path, String keystore_password) {
		HttpConfiguration httpsConfiguration = createHttpsConfiguration(https_port);
		httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

		HTTP2ServerConnectionFactory https2ConnectionFactory = new HTTP2ServerConnectionFactory(httpsConfiguration);
		ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
		alpn.setDefaultProtocol("h2");

		SslContextFactory sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(keystore_path);
		sslContextFactory.setKeyStorePassword(keystore_password);
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

		//SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString());
		SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

		OptionalSslConnectionFactory optionalSslConnectionFactory = new OptionalSslConnectionFactory(sslConnectionFactory, HttpVersion.HTTP_1_1.asString());

		HttpConnectionFactory httpsConnectionFactory = new HttpConnectionFactory(httpsConfiguration);

		ServerConnector httpServerConnector = new ServerConnector(server, optionalSslConnectionFactory, sslConnectionFactory, alpn, https2ConnectionFactory, httpsConnectionFactory);
		//ServerConnector httpServerConnector = new ServerConnector(server, sslContextFactory, httpsConnectionFactory);
		httpServerConnector.setPort(https_port);

		return httpServerConnector;
	}



	public static void main(String[] srgs) throws Exception {

		Broker broker = new Broker();
		Config config = broker.config();

		log.info("starting server");



		Server server = new Server();
		if(config.enableHttps()) {
			ServerConnector httpConnector = createHttpConnector(server, config.http_port);
			ServerConnector httpsConnector = createHttpsConnector(server, config.https_port, config.keystore_path, config.keystore_password);
			server.setConnectors(new Connector[] {httpConnector, httpsConnector});
		} else {
			ServerConnector httpConnector = createHttpConnector(server, config.http_port);
			server.setConnectors(new Connector[] {httpConnector});
		}		

		DefaultSessionIdManager sessionIdManager = new DefaultSessionIdManager(server);
		sessionIdManager.setWorkerName(null);
		SessionHandler sessionHandler = new SessionHandler();
		sessionHandler.setSessionIdManager(sessionIdManager);
		sessionHandler.setHttpOnly(true);
		sessionHandler.setSessionCookie("session");
		SessionCookieConfig sessionCokkieConfig = sessionHandler.getSessionCookieConfig();
		sessionCokkieConfig.setPath("/");

		HandlerList handlerList = new HandlerList();
		handlerList.addHandler(new OptionalSecuredRedirectHandler());
		handlerList.addHandler(sessionHandler);
		handlerList.addHandler(new InjectHandler());
		//if(broker.config().login) {
		handlerList.addHandler(createContext("/login", true, new LoginHandler(broker)));
		handlerList.addHandler(createContext("/loginWebAuthn", true, new LoginWebAuthnHandler(broker)));
		//}
		handlerList.addHandler(new JwsHandler(broker));
		handlerList.addHandler(new AccessHandler(broker));
		handlerList.addHandler(createContext("/audio", true, audio()));
		handlerList.addHandler(createContext("/samples", true, new SamplesHandler(broker)));
		handlerList.addHandler(createContext("/review_lists", true, new Review_listsHandler(broker)));
		handlerList.addHandler(createContext("/review_statistics", true, new ReviewStatisticsHandler(broker)));
		handlerList.addHandler(createContext("/review_statistics_detailed", true, new ReviewStatisticsDetailedHandler(broker)));
		handlerList.addHandler(createContext("/account", true, new AccountHandler(broker)));
		handlerList.addHandler(createContext("/identity", true, new IdentityHandler(broker)));
		handlerList.addHandler(createContext("/accounts", true, new AccountsHandler(broker)));
		handlerList.addHandler(createContext("/label_definitions", true, new LabelDefinitionsHandler(broker)));
		handlerList.addHandler(createContext("/query", true, new QueryHandler(broker)));
		handlerList.addHandler(createContext("/timeseries", true, new TimeseriesHandler(broker)));
		handlerList.addHandler(createContext("/web", true, webcontent()));
		handlerList.addHandler(new BaseRedirector("/web/app/"));
		handlerList.addHandler(createContext("/WebAuthn", true, new WebAuthnHandler(broker)));
		//if(broker.config().login) {
		handlerList.addHandler(createContext("/logout", true, new LogoutHandler()));
		//}
		
		handlerList.addHandler(createContext("/PhotoDB", true, new PhotoDBHandler(broker)));
		
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
		resourceHandler.setPrecompressedFormats(new CompressedContentFormat[]{CompressedContentFormat.BR, CompressedContentFormat.GZIP});
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
			response.setHeader("X-Frame-Options", "deny");
			response.setHeader("Referrer-Policy", "no-referrer");
			//response.setHeader("X-Content-Type-Options", "nosniff");
			//log.info("baseRequest.getRemoteAddr() " + baseRequest.getRemoteAddr());
			if("127.0.0.1".equals(baseRequest.getRemoteAddr())  || "[0:0:0:0:0:0:0:1]".equals(baseRequest.getRemoteAddr())) {
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setHeader("Access-Control-Allow-Headers", "content-type");
				if(baseRequest.getMethod().equals("OPTIONS")) {
					baseRequest.setHandled(true);
				}
			}
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

	public static ArrayList<Path> getAudioPaths(Path root, ArrayList<Path> collector) throws IOException {
		if(collector == null) {
			collector = new ArrayList<Path>();
		}
		for(Path path:Files.newDirectoryStream(root)) {
			if(path.toFile().isDirectory()) {
				getAudioPaths(path, collector);
			} else if(path.toFile().isFile()) {
				if(path.getFileName().toString().endsWith(".yaml")) {
					collector.add(path);
				}
			} else {
				log.warn("unknown entity: " + path);
			}
		}
		return collector;
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
