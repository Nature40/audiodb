package audio;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public class Sample {

	public final String id;
	public final Path path;

	public Sample(String id, Path path) {
		this.id = id;		
		this.path = path;
	}

	public File file() {
		return path.toFile();
	}

}
