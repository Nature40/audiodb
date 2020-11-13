package photo.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;

import audio.Broker;
import photo.Photo;
import photo.PhotoDB;

public class PhotoHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB photoDB;

	public PhotoHandler(Broker broker) {
		this.broker = broker;
		this.photoDB = broker.photoDB();
	}

	public void handle(String name, String next, Request request, HttpServletResponse response) throws IOException {
		Photo photo = photoDB.getPhoto(name);
		handleRoot(photo, request, response);
	}

	private void handleRoot(Photo photo, Request request, HttpServletResponse response) throws IOException {
		File file = photo.path.toFile();
		long fileLen = file.length();

		response.setContentType("image/jpeg");
		response.setContentLengthLong(fileLen);
		try(FileInputStream in = new FileInputStream(file)) {
			IO.copy(in, response.getOutputStream());
		}
	}
}
