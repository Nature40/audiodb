package photo2.task;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tinylog.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import util.collections.vec.Vec;
import util.yaml.YamlUtil;

public class MetaCreator {

	private static final Pattern FILE_TIME_PATTERN = Pattern.compile("_(\\d{8}_\\d{6})\\.jpg$");
	private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	public static boolean createYaml(File file, Path yamlPath, String missingLocation) {
		try {
			String filename = file.getName();
			//Logger.info(filename);			
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			//Riff riff = new Riff(file);
			m.put("PhotoSens", "v1.0");
			m.put("file", filename);
			try {
				m.put("file_size", file.length());
			} catch(Exception e) {
				Logger.warn(e);
			}
			m.put("location", missingLocation);
			
			LocalDateTime datetime = getDateByFilename(filename);
			if(datetime == null) {
				datetime = getDateByExif(file);
			}
			if(datetime != null) {
				m.put("date", datetime.format(ISO_FORMATTER));
			}

			//addRiffMeta(riff, m);
			Vec<Object> logList = new Vec<Object>();
			LinkedHashMap<String, Object> logO = new LinkedHashMap<String, Object>();
			logO.put("action", "create_yaml");
			logO.put("date", LocalDateTime.now().format(ISO_FORMATTER));
			logList.add(logO);
			m.put("log", logList);
			YamlUtil.writeSafeYamlMap(yamlPath, m);
			return true;
		} catch (Exception e) {
			Logger.warn(e);
			return false;
		}
	}
	
	public static LocalDateTime getDateByFilename(String filename) {
		try {
			Matcher matcher = FILE_TIME_PATTERN.matcher(filename);
			if(matcher.find()) {
				String fileTime = matcher.group(1);
				//Logger.info(fileTime);
				LocalDateTime datetime = LocalDateTime.parse(fileTime, FILE_TIME_FORMATTER);
				return datetime;
			}
			return null;
		} catch(Exception e) {
			Logger.warn(e);
			return null;
		}		
	}
	
	public static LocalDateTime getDateByExif(File file) {	
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			LocalDateTime datetime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			return datetime;
		} catch(Exception e) {
			Logger.warn(e);
			return null;
		}
	}
}
