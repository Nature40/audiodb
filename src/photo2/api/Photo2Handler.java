package photo2.api;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
import org.json.JSONWriter;

import com.drew.imaging.ImageProcessingException;

import audio.Broker;
import photo2.Photo2;
import photo2.PhotoDB2;
import photo2.ThumbManager;
import util.JsonUtil;
import util.SpiUtil;
import util.Web;

public class Photo2Handler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB2 photodb2;

	private static final Path CACHE_PATH = Paths.get("photo2_cache");
	static {
		CACHE_PATH.toFile().mkdirs();
	}

	public Photo2Handler(Broker broker) {
		this.broker = broker;
		this.photodb2 = broker.photodb2();
	}

	public void handle(String id, String target, Request request, HttpServletResponse response) throws IOException {
		request.setHandled(true);		
		try {
			Photo2 photo = photodb2.getPhoto2(id);
			if(photo == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: photo not found");
				return;
			}			
			if(target.equals("/")) {
				handleRoot(photo, request, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String curr = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				switch(curr) {
				case "image.jpg":
					handleImage(photo, curr, next, request, response);
					break;
				case "meta.yaml":
					handleMeta(photo, curr, next, request, response);
					break;					
				default:
					throw new RuntimeException("unknown path");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: " + e.getMessage());
		}
	}

	private void handleImage(Photo2 photo, String target, String next, Request request, HttpServletResponse response) throws FileNotFoundException, IOException {
		long reqWidth = Web.getInt(request, "width", 0);
		long reqHeight = Web.getInt(request, "height", 0);
		double reqGamma = Web.getDouble(request, "gamma", Double.NaN);

		if(reqWidth == 320 && reqHeight == 320) {
			photodb2.thumbManager.getScaled(photo, reqWidth, reqHeight, response);
		} else if(reqWidth <= 0 && reqHeight <= 0 && (reqGamma == 1 || !Double.isFinite(reqGamma))) {
			File file = photo.imagePath.toFile();
			long fileLen = file.length();
			response.setContentType("image/jpeg");
			response.setContentLengthLong(fileLen);
			try(FileInputStream in = new FileInputStream(file)) {
				IO.copy(in, response.getOutputStream());
			}
		} else {
			BufferedImage bufferedImage = ImageIO.read(photo.imagePath.toFile());
			if(Double.isFinite(reqGamma)) {
				WritableRaster raster = bufferedImage.getRaster();
				DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
				byte[] imageBuffer = dataBuffer.getData();
				double gammaCorrection = 1d / reqGamma;
				byte[] lut = new byte[256];
				for (int i = 0; i <= 255; i++) {
					long v = Math.round(255d * Math.pow(i / 255d, gammaCorrection));
					lut[i] =  (byte) v;
					//log.info(i + " -> " + lut[i] + "   " + v + "   " + Byte.toUnsignedInt(lut[i]));
				}
				for (int i = 0; i < imageBuffer.length; i++) {
					byte v = imageBuffer[i];
					imageBuffer[i] = lut[((int) v) & 0xff];
				}

			}
			if(reqWidth > 0 || reqHeight > 0 ) {
				long imgWidth = bufferedImage.getWidth();
				long imgHeight = bufferedImage.getHeight();
				long calcWidth = 0;
				long calcHeight = 0;
				if(reqWidth > 0 && reqHeight > 0) {
					calcWidth = (imgWidth * reqHeight) / imgHeight;
					calcHeight = (reqWidth * imgHeight) / imgWidth;

					if(calcWidth > reqWidth) {
						calcWidth = (imgWidth * calcHeight) / imgHeight;
					}
					if(calcHeight > reqHeight) {
						calcHeight = (calcWidth * imgHeight) / imgWidth;
					}					
				} else if(reqWidth > 0) {
					calcWidth = reqWidth;
					calcHeight = (reqWidth * imgHeight) / imgWidth;
				} else if(reqHeight > 0) {
					calcWidth = (imgWidth * reqHeight) / imgHeight;
					calcHeight = reqHeight;
				}
				if(calcWidth <= 0) {
					calcWidth = 1;
				}
				if(calcHeight <= 0) {
					calcHeight = 1;
				}
				if(calcWidth > 4096 || calcHeight > 4096) {
					throw new RuntimeException("image too large");
				}
				bufferedImage = ThumbManager.scale(bufferedImage, (int) calcWidth, (int) calcHeight);						
			}
			response.setContentType("image/jpeg");
			ThumbManager.writeJPG(bufferedImage, 0.95f, response);
		}
	}

	private void handleMeta(Photo2 photo, String target, String next, Request request, HttpServletResponse response) throws FileNotFoundException, IOException {
		File file = photo.metaPath.toFile();
		long fileLen = file.length();
		response.setContentType("application/x-yaml");
		response.setContentLengthLong(fileLen);
		try(FileInputStream in = new FileInputStream(file)) {
			IO.copy(in, response.getOutputStream());
		}
	}	

	private void handleRoot(Photo2 photo, Request request, HttpServletResponse response) throws IOException, ImageProcessingException {
		boolean writeClassifications = Web.getFlagBoolean(request, "classifications");
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("photo");
		json.object();
		JsonUtil.write(json, "id", photo.id);
		JsonUtil.write(json, "location", photo.location);
		JsonUtil.write(json, "date", photo.date);
		if(writeClassifications) {
			json.key("classifications");
			json.array();
			photo.foreachClassification(map -> {
				json.object();
				if(map.contains("classification")) {
					json.key("classification");
					json.value(map.getString("classification"));
				}
				if(map.contains("classificator")) {
					json.key("classificator");
					json.value(map.getString("classificator"));
				}
				try {
					if(map.contains("date")) {
						json.key("date");
						LocalDateTime localDateTime = map.getLocalDateTime("date");						
						json.value(localDateTime);
					}
				} catch(Exception e) {
					log.warn(e);
				}
				if(map.contains("bbox")) {
					json.key("bbox");					
					json.value(map.getList("bbox").asFloatArray());
				}
				if(map.contains("conf")) {
					json.key("conf");
					json.value(map.getString("conf"));
				}
				if(map.contains("uncertainty")) {
					json.key("uncertainty");
					json.value(map.getString("uncertainty"));
				}
				if(map.contains("expert_name")) {
					json.key("expert_name");
					json.value(map.getString("expert_name"));
				}
				json.endObject();
			});
			json.endArray(); // classifications
		}
		json.endObject(); // photo
		json.endObject(); // json
	}
}
