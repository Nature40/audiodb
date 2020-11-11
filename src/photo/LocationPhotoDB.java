package photo;

import java.util.concurrent.ConcurrentHashMap;

public class LocationPhotoDB {
	
	public final String id;
	
	private final ConcurrentHashMap<String, Photo> photoMap =  new ConcurrentHashMap<String, Photo>();
	
	public LocationPhotoDB(String id) {
		this.id = id;
	}

	public void insert(Photo photo) {
		photoMap.put(photo.id, photo);	
	}
}
