package photo2;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Photo2 {
	static final Logger log = LogManager.getLogger();

	public final String id;
	public final Path metaPath;
	public final Path imagePath;
	public final String location;

	public Photo2(String id, Path metaPath, Path imagePath, String location) {
		this.id = id;
		this.metaPath = metaPath;
		this.imagePath = imagePath;
		this.location = location;
		log.info(this);
	}

	@Override
	public String toString() {
		return "Photo2 [id=" + id + ", metaPath=" + metaPath + ", imagePath=" + imagePath + ", location=" + location
				+ "]";
	}
}
