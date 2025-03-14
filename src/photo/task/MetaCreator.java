package photo.task;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.tinylog.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import photo.PhotoDB2;
import photo.PhotoProjectConfig;
import util.collections.vec.Vec;
import util.yaml.YamlUtil;

public class MetaCreator {

	private static final Pattern FILE_TIME_PATTERN = Pattern.compile("_(\\d{8}_\\d{6})\\.jpg$");
	private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	
	private static final String[] RESERVED_KEYS = new String[] {"", " ", "*", "_", "PhotoSens", "original_path", "file", "file_size", "log"};
	
	private static final HashSet<String> RESEVED_KEY_SET = new HashSet<String>();
	
	static {
		RESEVED_KEY_SET.addAll(Arrays.asList(RESERVED_KEYS));
	}

	/**
	 * 
	 * @param relativeFolderPath  nullable
	 * @return not null
	 */
	private static String[] pathToArray(Path relativeFolderPath) {
		if(relativeFolderPath == null) {
			return new String[] {};
		}
		return StreamSupport.stream(relativeFolderPath.spliterator(), false).map(Path::toString).toArray(String[]::new);		
	}

	/**
	 * 
	 * @param file
	 * @param yamlPath
	 * @param missingLocation
	 * @param relativeFolderPath nullable
	 * @param photoProjectConfig 
	 * @param photoConfig 
	 * @return
	 */
	public static boolean createYaml(File file, Path yamlPath, String missingLocation, Path relativeFolderPath, PhotoProjectConfig photoProjectConfig) {
		try {
			String filename = file.getName();
			//Logger.info(filename);			
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			//Riff riff = new Riff(file);
			m.put("PhotoSens", "v1.0");
			String[] originalPath = pathToArray(relativeFolderPath);
			m.put("original_path", originalPath);
			m.put("file", filename);
			try {
				m.put("file_size", file.length());
			} catch(Exception e) {
				Logger.warn(e);
			}
			if(missingLocation != null && !missingLocation.isBlank() && !PhotoDB2.NO_LOCATION.equals(missingLocation)) {
				m.put("location", missingLocation);
			}

			LocalDateTime datetime = getDateByFilename(filename);
			if(datetime == null || PhotoDB2.NO_DATE.equals(datetime)) {
				datetime = getDateByExif(file);
			}
			if(datetime != null && !PhotoDB2.NO_DATE.equals(datetime)) {
				m.put("date", datetime.format(ISO_FORMATTER));
			}

			//addRiffMeta(riff, m);
			Vec<Object> logList = new Vec<Object>();
			LinkedHashMap<String, Object> logO = new LinkedHashMap<String, Object>();
			logO.put("action", "create_yaml");
			logO.put("date", LocalDateTime.now().format(ISO_FORMATTER));
			logList.add(logO);
			m.put("log", logList);
			
			if(photoProjectConfig.original_path_keys != null && photoProjectConfig.original_path_keys.length > 0 && originalPath != null && originalPath.length > 0) {
				int len = Math.min(photoProjectConfig.original_path_keys.length, originalPath.length);
				for(int i=0; i<len; i++) {
					String key = photoProjectConfig.original_path_keys[i];
					String value = originalPath[i];
					if(key != null && !key.isBlank() && key != "_" && value != null && !value.isBlank() && value != "_") {
						if(!RESEVED_KEY_SET.contains(key)) {
							m.put(key, value);
						}
					}
				}
			}
			
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
			if(directory != null) {
				Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				if(date != null) {
					LocalDateTime datetime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
					return datetime;
				} else {
					return null;
				}			
			} else {
				return null;
			}
		} catch(Exception e) {
			Logger.warn(e);
			return null;
		}
	}
}
