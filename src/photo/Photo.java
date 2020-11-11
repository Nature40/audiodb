package photo;

import java.nio.file.Path;

public class Photo {
	public final String locationID;
	public final String id;
	public final Path path;
	
	public Photo(String locationID, String id, Path path) {
		this.locationID = locationID;
		this.id = id;
		this.path = path;
	}
}
