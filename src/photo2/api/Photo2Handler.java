package photo2.api;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.drew.imaging.ImageProcessingException;

import audio.Account;
import audio.Broker;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import photo2.Photo2;
import photo2.PhotoDB2;
import photo2.ThumbManager;
import util.JsonUtil;
import util.Web;

public class Photo2Handler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final PhotoDB2 photodb2;

	public Photo2Handler(Broker broker) {
		this.broker = broker;
		this.photodb2 = broker.photodb2();
	}

	public void handle(String id, String target, Request request, HttpServletResponse response) throws IOException {
		request.setHandled(true);		
		try {
			Photo2 photo = photodb2.getPhoto2NotLocked(id);
			if(photo == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: photo not found");
				return;
			}
			/*if(photo.locked) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: photo access forbidden");
				return;
			}*/
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

	private void handleRoot(Photo2 photo, Request request, HttpServletResponse response) throws ImageProcessingException, IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRoot_GET(photo, request, response);
			break;
		case "POST":
			handleRoot_POST(photo, request, response);
			break;
		default:
			throw new RuntimeException("no call");
		}
	}


	private static float[] jsonToBbox(JSONObject json, String name) {
		JSONArray jsonArray = json.optJSONArray(name);
		if(jsonArray == null) {
			return null;
		}
		if(jsonArray.length() != 4) {
			throw new RuntimeException("no bbox");
		}
		float[] bbox = new float[4];
		for (int i = 0; i < 4; i++) {
			bbox[i] = Float.parseFloat(jsonArray.get(i).toString());
		}
		return bbox;
	}

	private void handleRoot_POST(Photo2 photo, Request request, HttpServletResponse response) throws IOException, ImageProcessingException {
		HttpSession session = request.getSession(false);
		Account account = (Account) session.getAttribute("account");		
		JSONObject jsonReq = new JSONObject(new JSONTokener(request.getReader()));
		JSONArray jsonActions = jsonReq.getJSONArray("actions");
		int jsonActionsLen = jsonActions.length();
		boolean refreshDBphotoEntry = false;
		try {
			for (int i = 0; i < jsonActionsLen; i++) {
				JSONObject jsonAction = jsonActions.getJSONObject(i);
				String actionName = jsonAction.getString("action");
				switch(actionName) {
				case "set_classification": {
					refreshDBphotoEntry = true;
					float[] bbox = jsonToBbox(jsonAction, "bbox");
					String classification = jsonAction.getString("classification");
					String classificator = "Expert";
					String identity = account.username;
					String date = LocalDateTime.now().toString();
					photo.setClassification(bbox, classification, classificator, identity, date);
					break;
				}
				default:
					throw new RuntimeException("unknown action:" + actionName);
				}
			}
		} finally {
			if(refreshDBphotoEntry) {
				photodb2.refreshPhotoDBentry(photo, null);
			}
		}
	}

	private void handleRoot_GET(Photo2 photo, Request request, HttpServletResponse response) throws IOException, ImageProcessingException {
		//boolean writeClassifications = Web.getFlagBoolean(request, "classifications");
		boolean writeDetections = Web.getFlagBoolean(request, "detections");
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("photo");
		json.object();
		JsonUtil.write(json, "id", photo.id);
		JsonUtil.write(json, "location", photo.location);
		JsonUtil.write(json, "date", photo.date);
		if(writeDetections) {
			json.key("detections");
			json.array(); // detections
			photo.foreachDetection(map -> {
				json.object();  // detection
				if(map.contains("bbox")) {
					json.key("bbox");					
					json.value(map.getList("bbox").asFloatArray());
				}
				json.key("classifications");
				json.array(); // classifications
				map.optList("classifications").asMaps().forEach(cmap -> {
					json.object();  // classification
					if(cmap.contains("classification")) {
						json.key("classification");
						json.value(cmap.getString("classification"));
					}
					if(cmap.contains("classificator")) {
						json.key("classificator");
						json.value(cmap.getString("classificator"));
					}
					if(cmap.contains("identity")) {
						json.key("identity");
						json.value(cmap.getString("identity"));
					}
					try {
						if(cmap.contains("date")) {
							LocalDateTime localDateTime = cmap.getLocalDateTime("date");	
							json.key("date");	
							json.value(localDateTime);
						}
					} catch(Exception e) {
						e.printStackTrace();
						log.warn(e);
					}
					if(cmap.contains("conf")) {
						json.key("conf");
						json.value(cmap.getString("conf"));
					}						
					json.endObject(); // classification
				});
				json.endArray(); // classifications	

				json.endObject();  // detection
			});
			json.endArray(); // detections
		}
		/*if(writeClassifications) {
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
		}*/
		json.endObject(); // photo
		json.endObject(); // json
	}
}
