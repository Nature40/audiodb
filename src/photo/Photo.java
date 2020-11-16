package photo;

import java.nio.file.Path;
import java.util.Comparator;

public class Photo {
	public static Comparator<Photo> comparator = new Comparator<Photo>() {
		@Override
		public int compare(Photo a, Photo b) {
			return a.id.compareTo(b.id);
		}
	};
	
	public final String locationID;
	public final String id;
	public final Path path;
	
	public Photo(String locationID, String id, Path path) {
		this.locationID = locationID;
		this.id = id;
		this.path = path;
	}
}
