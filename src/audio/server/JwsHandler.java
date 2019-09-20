package audio.server;

import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.samskivert.mustache.MustacheException;

import audio.Account;
import audio.Broker;
import audio.JwsConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import util.TemplateUtil;

public class JwsHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final static int clock_skew = 60;

	// algorithm: ES512 (ECDSA using P-521 and SHA-512) (a standard JWS algorithm)
	static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.ES512;
	static KeyFactory KEY_FACTORY = getKeyFactory("EC");

	// private key format: PKCS#8 (Base64 encoded)

	// public key format: X.509 (Base64 encoded)

	public static KeyFactory getKeyFactory(String name) {
		try {
			return KeyFactory.getInstance(name);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private final Broker broker;

	public JwsHandler(Broker broker) throws IOException {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String jwsParam = request.getParameter("jws");
		if(jwsParam != null) {
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			handleJwsParameterRedirect(jwsParam, baseRequest, response);
			return;
		}		
	}

	private void handleJwsParameterRedirect(String jwsParam, Request request, HttpServletResponse response) throws MustacheException, IOException {
		String redirect_target = request.getRequestURL().toString();
		String qs = request.getQueryString();
		int jwsIndex = qs.indexOf("jws=");
		if(jwsIndex < 0) {
			throw new RuntimeException("url JWS error");
		}
		if(jwsIndex > 0) {
			redirect_target += "?" + qs.substring(0, jwsIndex - 1);
		}
		//String redirect_target = "/";
		try {
			Jws<Claims> jws = Jwts.parser().setSigningKeyResolver(signingKeyResolver).setAllowedClockSkewSeconds(clock_skew).parseClaimsJws(jwsParam);
			Claims jwsClaims = jws.getBody();
			String username = jwsClaims.getSubject();

			JwsConfig jwsConfig = broker.config().jwsConfigs.first();

			String[] roles = jwsConfig.roles;
			Account account = new Account(username, roles);

			HttpSession session = request.getSession(true);
			AccessHandler.injectSameSite(response);
			session.setAttribute("authentication", "jws");
			session.setAttribute("account", account);
			session.setAttribute("roles", broker.roleManager().getRoleBits(account.roles));

			request.setHandled(true);
			response.setHeader(HttpHeader.LOCATION.asString(), redirect_target);
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setContentLength(0);
			return;
		} catch (Exception e) {
			log.warn(e);
			request.setHandled(true);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			HashMap<String, Object> ctx = new HashMap<>();
			ctx.put("error", e.getMessage());
			ctx.put("redirect_target", redirect_target);
			TemplateUtil.getTemplate("login_jws_error.mustache", true).execute(ctx, response.getWriter());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private SigningKeyResolver signingKeyResolver = new SigningKeyResolver() {

		@Override
		public Key resolveSigningKey(@SuppressWarnings("rawtypes") JwsHeader header, String plaintext) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public Key resolveSigningKey(@SuppressWarnings("rawtypes") JwsHeader header, Claims claims) {
			return stringToPublicKey(getKey(header.getKeyId()));
		}

		private String getKey(String keyID) {
			if(keyID == null) { // set first key from config
				return broker.config().jwsConfigs.first().provider_public_key;
			}
			for(JwsConfig jwsConfig : broker.config().jwsConfigs) {
				if(keyID.equals(jwsConfig.provider_key_id)) {
					return jwsConfig.provider_public_key;
				}
			}
			throw new RuntimeException("keyID not found");
		}
	};

	private static PublicKey stringToPublicKey(String s) {
		byte[] bytes = Base64.getDecoder().decode(s);	
		X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
		try {
			return KEY_FACTORY.generatePublic(spec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static PrivateKey stringToPrivateKey(String s) {
		byte[] bytes = Base64.getDecoder().decode(s);		
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
		try {
			return KEY_FACTORY.generatePrivate(spec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}
}
