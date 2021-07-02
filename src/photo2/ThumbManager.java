package photo2;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.IO;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import util.SpiUtil;

public class ThumbManager {
	static final Logger log = LogManager.getLogger();

	private Connection conn;

	ThreadLocal<ThumbSqlConnector> tlsqlconnector = new ThreadLocal<ThumbSqlConnector>() {
		@Override
		public ThumbSqlConnector initialValue() {
			return new ThumbSqlConnector(conn);
		}		
	};	

	private ConcurrentHashMap<String, ForkJoinTask<?>> taskMap = new ConcurrentHashMap<String, ForkJoinTask<?>>();

	public class ThumbTask implements Runnable {
		private final Photo2 photo;
		private final String cacheFilename;
		private final long reqWidth;
		private final long reqHeight;

		public ThumbTask(Photo2 photo, String cacheFilename, long reqWidth2, long reqHeight2) {
			this.photo = photo;
			this.cacheFilename = cacheFilename;
			this.reqWidth = reqWidth2;
			this.reqHeight = reqHeight2;
		}

		@Override
		public void run() {
			try {
				log.info(cacheFilename);
				ThumbSqlConnector sqlconnector = tlsqlconnector.get();				
				try {
					sqlconnector.stmt_query_file.setString(1, cacheFilename);
					ResultSet res1 = sqlconnector.stmt_query_file.executeQuery();
					if(res1.next()) {
						log.info("image already scaled. return.");
						return;
					}
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}	

				BufferedImage bufferedImage = ImageIO.read(photo.imagePath.toFile());
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
				BufferedImage dstImage = ThumbManager.scale(bufferedImage, (int) calcWidth, (int) calcHeight);			
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				ThumbManager.writeJPG(dstImage, 0.95f, outStream);	
				ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
				outStream.close();				
				sqlconnector.stmt_insert_file.setString(1, cacheFilename);
				sqlconnector.stmt_insert_file.setBlob(2, inStream);
				sqlconnector.stmt_insert_file.execute();
				inStream.close();
			} catch(Exception e) {
				if(photo.imagePath.toFile().exists()) {
					log.warn(e + "  " + photo.imagePath);
					throw new RuntimeException(e);	
				} else {
					log.warn("missing image file: " + photo.imagePath);
					throw new RuntimeException("missing image file: " + photo.imagePath);	
				}				
			} finally {
				taskMap.remove(cacheFilename);
			}
		}
	}

	public ThumbManager() {
		try {
			this.conn = DriverManager.getConnection("jdbc:h2:./thumb_cache");

			Statement stmt = conn.createStatement();
			ResultSet res = conn.getMetaData().getTables(null, null, "THUMB", null);
			if(res.next()) {
				/*log.info("DROP TABLE THUMB");
				stmt.executeUpdate("DROP TABLE THUMB");
				stmt.executeUpdate("CREATE TABLE THUMB (ID VARCHAR(255) PRIMARY KEY, FILE BLOB)");*/
			} else {
				stmt.executeUpdate("CREATE TABLE THUMB (ID VARCHAR(255) PRIMARY KEY, FILE BLOB)");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}


	}

	public String getCacheFilename(Photo2 photo, long reqWidth, long reqHeight) {
		String cacheFilename = photo.id + ".image.width" + reqWidth + ".height" + reqHeight + ".jpg";
		return cacheFilename;
	}

	public void getScaled(Photo2 photo, long reqWidth, long reqHeight, HttpServletResponse response) {
		String cacheFilename = getCacheFilename(photo, reqWidth, reqHeight);
		getScaled(cacheFilename, photo, reqWidth, reqHeight, response);
	}

	public void getScaled(String cacheFilename, Photo2 photo, long reqWidth, long reqHeight, HttpServletResponse response) {
		log.info(cacheFilename);
		try {
			ThumbSqlConnector sqlconnector = tlsqlconnector.get();
			sqlconnector.stmt_query_file.setString(1, cacheFilename);
			ResultSet res = sqlconnector.stmt_query_file.executeQuery();
			if(!res.next()) {
				ForkJoinTask<?> task = submitScaled(cacheFilename, photo, reqWidth, reqHeight);
				if(task != null) {
					try {
						task.get();
					} catch (InterruptedException | ExecutionException e) {
						log.warn(e);
						throw new RuntimeException(e);
					}
				}
			}
			if(response != null) {
				sqlconnector.stmt_query_file.setString(1, cacheFilename);
				res = sqlconnector.stmt_query_file.executeQuery();
				if(!res.next()) {
					throw new RuntimeException("internal error");
				}
				InputStream inStream = res.getBinaryStream(1);
				IO.copy(inStream, response.getOutputStream());
			}
		} catch (SQLException | IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	public static void writeJPG(BufferedImage dstImage, float quality, OutputStream out) throws IOException {
		ImageWriter jpgWriter = SpiUtil.JPEG_IMAGE_WRITER_SPI.createWriterInstance();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(quality);
		ImageOutputStream imageOut = SpiUtil.OUTPUT_STREAM_IMAGE_OUTPUT_STREAM.createOutputStreamInstance(out);
		jpgWriter.setOutput(imageOut);
		jpgWriter.write(null, new IIOImage(dstImage, null, null), jpgWriteParam);		
	}

	public static void writeJPG(BufferedImage dstImage, float quality, HttpServletResponse response) throws IOException {
		ImageWriter jpgWriter = SpiUtil.JPEG_IMAGE_WRITER_SPI.createWriterInstance();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(quality);
		ImageOutputStream imageOut = SpiUtil.OUTPUT_STREAM_IMAGE_OUTPUT_STREAM.createOutputStreamInstance(response.getOutputStream());
		jpgWriter.setOutput(imageOut);
		jpgWriter.write(null, new IIOImage(dstImage, null, null), jpgWriteParam);		
	}

	public static void writeJPG(BufferedImage dstImage, float quality, File file) throws IOException {
		ImageWriter jpgWriter = SpiUtil.JPEG_IMAGE_WRITER_SPI.createWriterInstance();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(quality);
		try(ImageOutputStream imageOut = ImageIO.createImageOutputStream(file)){
			jpgWriter.setOutput(imageOut);
			jpgWriter.write(null, new IIOImage(dstImage, null, null), jpgWriteParam);	
		}
	}

	public static BufferedImage scale(BufferedImage src, int width, int height) {
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
	
	public static BufferedImage scaleFast(BufferedImage src, int width, int height) {
		/*BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		//g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);	
		if(!g.drawImage(src, 0, 0, width, height, null)) {
			log.warn("image not drawn fully");			
		}
		g.dispose();
		dst.flush();*/
		BufferedImage dst = Scalr.resize(src, Method.SPEED, Mode.FIT_EXACT, width, height);
		//BufferedImage dst = Scalr.resize(src, Method.ULTRA_QUALITY, Mode.FIT_EXACT, width, height);
		return dst;
	}

	public ThumbSqlConnector getSqlConnector() {
		return tlsqlconnector.get();
	}

	public ForkJoinTask<?> submitScaled(String cacheFilename, Photo2 photo, long reqWidth, long reqHeight) {
		ForkJoinTask<?> task = taskMap.computeIfAbsent(cacheFilename, cf -> {
			try {
				ThumbSqlConnector sqlconnector = tlsqlconnector.get();
				sqlconnector.stmt_query_file.setString(1, cacheFilename);
				ResultSet res1 = sqlconnector.stmt_query_file.executeQuery();
				if(res1.next()) {
					log.info("image already scaled. return.");
					return null;
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			ThumbTask thumbTask = new ThumbTask(photo, cacheFilename, reqWidth, reqHeight);
			ForkJoinTask<?> fjt = ForkJoinPool.commonPool().submit(thumbTask);
			//log.info(fjt);
			return fjt;
		});
		return task;
	}

}
