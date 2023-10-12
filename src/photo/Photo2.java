package photo;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Consumer;

import org.tinylog.Logger;

import photo.api.PhotoMeta;
import util.HashUtil;
import util.yaml.YamlList;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class Photo2 {	

	public final String id;
	public final PhotoProjectConfig projectConfig;
	public final Path metaPath;
	public final Path imagePath;
	public final String location;
	public final LocalDateTime date;
	public final long last_modified;
	public final boolean locked;

	public Photo2(String id, PhotoProjectConfig projectConfig, Path metaPath, Path imagePath, String location, LocalDateTime date, long last_modified, boolean locked) {
		this.id = id;
		this.projectConfig = projectConfig;
		this.metaPath = metaPath;
		this.imagePath = imagePath;
		this.location = location;
		this.date = date;
		this.last_modified = last_modified;
		this.locked = locked;
		//Logger.info(this);
	}

	/*public void foreachClassification(Consumer<YamlMap> consumer) {
		YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
		YamlList list = yamlMap.optList("classifications");
		list.asMaps().forEach(consumer);
	}*/

	public void foreachDetection(Consumer<YamlMap> consumer) {
		YamlMap yamlMap = getMeta();
		YamlList list = yamlMap.optList("detections");
		list.asMaps().forEach(consumer);
	}

	public YamlMap getMeta() {
		YamlMap yamlMap = YamlUtil.readYamlMap(metaPath);
		return yamlMap;
	}

	private void writeMeta(Map<String, Object> map) {
		YamlUtil.writeSafeYamlMap(metaPath, map);
	}

	public boolean setClassification(float[] bbox, String classification, String classificator, String identity, String date, float conf) {		
		PhotoMeta photoMeta = new PhotoMeta(getMeta());
		boolean ret = photoMeta.setClassification(bbox, classification, classificator, identity, date, conf);
		writeMeta(photoMeta.metaMap.getInternalMap());
		return ret;
	}

	@Override
	public String toString() {
		return "Photo2 [id=" + id + ", metaPath=" + metaPath + ", imagePath=" + imagePath + ", location=" + location
				+ ", date=" + date + ", last_modified=" + last_modified + ", locked=" + locked + "]";
	}

	public String getFileHash(boolean createIfMissing) {
		YamlMap meta = getMeta();
		Map<String, Object> map = meta.getInternalMap();
		String xxh64 = meta.optString("XXH64");
		if(xxh64 == null && createIfMissing) {
			File file = imagePath.toFile();
			if(meta.contains("file_size")) {
				long metaFileSize = meta.getLong("file_size");
				long fileSize = file.length();
				if(metaFileSize != fileSize) {
					throw new RuntimeException("unexpected file size " + fileSize + "  in  " + imagePath.toString());
				}
			} else {
				try {
					long fileSize = file.length();
					map.put("file_size", fileSize); // write later
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			Logger.info(imagePath);		
			try {
				xxh64 = HashUtil.getFileHashString(file);
				map.put("XXH64", xxh64);
				YamlUtil.writeSafeYamlMap(metaPath, map);
			} catch (Exception e) {
				Logger.warn(e);
			}
		}
		return xxh64;
	}
}
