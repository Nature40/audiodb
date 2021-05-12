package photo2.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletResponse;

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
		File file = photo.imagePath.toFile();
		long reqWidth = Web.getInt(request, "width", 0);
		long reqHeight = Web.getInt(request, "height", 0);
		boolean cached = Web.getFlagBoolean(request, "cached");

		if(reqWidth <= 0 && reqHeight <= 0) {
			long fileLen = file.length();
			response.setContentType("image/jpeg");
			response.setContentLengthLong(fileLen);
			try(FileInputStream in = new FileInputStream(file)) {
				IO.copy(in, response.getOutputStream());
			}
		} else {
			try {

				String cacheFilename = photo.id + ".image.width" + reqWidth + ".height" + reqHeight + ".jpg";
				log.info(cacheFilename);

				File cacheFile = Paths.get(CACHE_PATH.toString(), cacheFilename).toFile();

				if(!cached || !cacheFile.exists()) {

					BufferedImage bufferedImage = ImageIO.read(file);
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
					BufferedImage dstImage = scale(bufferedImage, (int) calcWidth, (int) calcHeight);
					if(cached) {
						writeJPG(dstImage, 0.95f, cacheFile);
					} else {
						writeJPG(dstImage, 0.95f, response);						
					}
				}
				if(cached) {
					long cacheFileLen = cacheFile.length();
					response.setContentType("image/jpeg");
					response.setContentLengthLong(cacheFileLen);
					try(FileInputStream in = new FileInputStream(cacheFile)) {
						IO.copy(in, response.getOutputStream());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.error(e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: " + e.getMessage());
			}
		}
	}
	
	private void writeJPG(BufferedImage dstImage, float quality, HttpServletResponse response) throws IOException {
		ImageWriter jpgWriter = SpiUtil.JPEG_IMAGE_WRITER_SPI.createWriterInstance();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(quality);
		ImageOutputStream imageOut = SpiUtil.OUTPUT_STREAM_IMAGE_OUTPUT_STREAM.createOutputStreamInstance(response.getOutputStream());
		jpgWriter.setOutput(imageOut);
		jpgWriter.write(null, new IIOImage(dstImage, null, null), jpgWriteParam);		
	}

	private void writeJPG(BufferedImage dstImage, float quality, File file) throws IOException {
		ImageWriter jpgWriter = SpiUtil.JPEG_IMAGE_WRITER_SPI.createWriterInstance();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(quality);
		try(ImageOutputStream imageOut = ImageIO.createImageOutputStream(file)){
			jpgWriter.setOutput(imageOut);
			jpgWriter.write(null, new IIOImage(dstImage, null, null), jpgWriteParam);	
		}
	}

	public BufferedImage scale(BufferedImage src, int width, int height) {
		/*BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		//g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);	
		if(!g.drawImage(src, 0, 0, width, height, null)) {
			log.warn("image not drawn fully");			
		}
		g.dispose();
		dst.flush();*/
		//BufferedImage dst = Scalr.resize(src, Method.SPEED, Mode.FIT_EXACT, width, height);
		BufferedImage dst = Scalr.resize(src, Method.ULTRA_QUALITY, Mode.FIT_EXACT, width, height);
		return dst;
	}

	private void handleRoot(Photo2 photo, Request request, HttpServletResponse response) throws IOException, ImageProcessingException {
		response.setContentType("application/json");
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("photo");
		json.object();
		JsonUtil.write(json, "id", photo.id);
		json.endObject();
		JsonUtil.write(json, "location", photo.location);
		json.endObject();
	}
}
