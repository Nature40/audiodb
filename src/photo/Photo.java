package photo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import util.collections.ReadonlyList;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Photo {
	private static final Logger log = LogManager.getLogger();
	public static Comparator<Photo> comparator = new Comparator<Photo>() {
		@Override
		public int compare(Photo a, Photo b) {
			if(a.location != b.location) {
				int d = a.location.compareTo(b.location);
				if(d != 0) {
					return d;
				}
			}
			int c = Long.compare(a.timestamp, b.timestamp);
			if(c != 0) {
				return c;
			}
			return a.id.compareTo(b.id);
		}
	};
	
	public static final String LOCATION_MISSING = "";
	public static final long TIMESTAMP_MISSING = 0;

	private String location = LOCATION_MISSING;
	private long timestamp = TIMESTAMP_MISSING;
	public final String id;
	public final Path photoPath;
	public final Path metaPath;
	
	private ReadonlyList<Tag> tags = ReadonlyList.EMPTY;

	public Photo(String id, Path photoPath, String location) {
		this.location = location;
		this.id = id;
		this.photoPath = photoPath;
		this.metaPath = Paths.get(photoPath.toString() + ".photo_meta");
		readOrCreate();
	}

	public void readOrCreate() {
		if(metaPath.toFile().exists()) {
			readMeta();
		} else {
			create();
		}
	}

	public synchronized boolean readMeta() {
		if(!Files.exists(metaPath)) {
			log.info("no metaPath file: " + metaPath);
			return false;			
		}
		YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
		location = yamlMap.optString("location", LOCATION_MISSING);
		timestamp = yamlMap.optLong("timestamp", TIMESTAMP_MISSING);		
		tags = yamlMap.optReadonlyList("tags", Tag::ofYAML);		
		return true;
	}

	public void writeMeta() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		YamlUtil.optPut(yamlMap, "location", location, LOCATION_MISSING);
		YamlUtil.optPut(yamlMap, "timestamp", timestamp, TIMESTAMP_MISSING);
		YamlUtil.optPut(yamlMap, "tags", tags, Tag::toMap);
		YamlUtil.writeSafeYamlMap(metaPath, yamlMap);
	}

	public void create() {
		try {
			log.info("create " + metaPath);
			Metadata metadata = ImageMetadataReader.readMetadata(photoPath.toFile());
			ExifIFD0Directory directory1 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			Date date = directory1.getDate(ExifIFD0Directory.TAG_DATETIME);
			log.info(date);
			LocalDateTime datetime = convertToLocalDateTimeViaInstant(date);
			log.info(datetime);
			this.timestamp = datetime.toInstant(ZoneOffset.UTC).toEpochMilli();
			writeMeta();
		} catch (Exception e) {
			log.warn("Error " + e);
		}
	}

	public static LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
		return dateToConvert.toInstant()
				.atZone(ZoneId.of("UTC"))
				.toLocalDateTime();
	}

	public long timestamp() {
		return timestamp;
	}

	public boolean hasTimestamp() {
		return timestamp != TIMESTAMP_MISSING;
	}

	public ReadonlyList<Tag> tags() {
		return tags;
	}
}
