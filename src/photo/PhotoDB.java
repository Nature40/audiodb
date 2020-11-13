package photo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PhotoDB {
	static final Logger log = LogManager.getLogger();

	Path root = Paths.get("photo_data");

	private ConcurrentHashMap<String, Photo> photoMap = new ConcurrentHashMap<String, Photo>();
	private ConcurrentHashMap<String, LocationPhotoDB> locationPhotoDBMap = new ConcurrentHashMap<String, LocationPhotoDB>();

	public PhotoDB() {
		refresh();
	}

	public LocationPhotoDB getOrCreate(String locationID) {
		return locationPhotoDBMap.computeIfAbsent(locationID, id -> new LocationPhotoDB(id));
	}

	private static ArrayList<Path> getLocationPaths(Path root, ArrayList<Path> collector) throws IOException {
		if(collector == null) {
			collector = new ArrayList<Path>();
		}
		for(Path path:Files.newDirectoryStream(root)) {
			if(path.toFile().isDirectory()) {
				collector.add(path);
			} else if(path.toFile().isFile()) {

			} else {
				log.warn("unknown entity: " + path);
			}
		}
		return collector;
	}

	private static ArrayList<Path> getLocationPhotoPaths(Path locationRoot, ArrayList<Path> collector) throws IOException {
		if(collector == null) {
			collector = new ArrayList<Path>();
		}
		for(Path path:Files.newDirectoryStream(locationRoot)) {
			if(path.toFile().isDirectory()) {
				getLocationPhotoPaths(path, collector);
			} else if(path.toFile().isFile()) {
				collector.add(path);
			} else {
				log.warn("unknown entity: " + path);
			}
		}
		return collector;
	}

	public void refresh() {
		try {
			photoMap.clear();
			locationPhotoDBMap.clear();
			ArrayList<Path> locationRoots = getLocationPaths(root, null);
			for(Path locationRoot : locationRoots) {
				ArrayList<Path> photoPaths = getLocationPhotoPaths(locationRoot, null);
				String locationID = locationRoot.getFileName().toString();
				LocationPhotoDB locationPhotoDB = getOrCreate(locationID);
				for(Path photoPath : photoPaths) {
					String photoID = createID(photoPath);
					log.info(photoPath + " -> " + photoID);
					Photo photo = new Photo(locationID, photoID, photoPath);
					insert(photo);
					locationPhotoDB.insert(photo);
				}
			}
		} catch (IOException e) {
			log.warn(e);
		}		
	}

	public void insert(Photo photo) {
		photoMap.put(photo.id, photo);		
	}

	public String createID(Path path) {
		String id = root.relativize(path).toString();
		id = id.replaceAll("/", "__");
		id = id.replaceAll("\\\\", "__");
		return id;
	}
	
	public void foreachLocation(Consumer<? super LocationPhotoDB> consumer) {
		locationPhotoDBMap.values().forEach(consumer);
	}
	
	public void foreachPhoto(Consumer<? super Photo> consumer) {
		photoMap.values().forEach(consumer);
	}	

	public Photo getPhoto(String id) {
		return photoMap.get(id);		
	}

	public LocationPhotoDB getLocationPhotoDB(String location) {
		return locationPhotoDBMap.get(location);
		
	}

}
