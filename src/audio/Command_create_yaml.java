package audio;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import org.tinylog.Logger;

public class Command_create_yaml implements Command {

	@Override
	public void execute(String command, String[] params) {
		if(params.length < 1) {
			throw new RuntimeException("missing parameter: folder");
		}
		if(params.length != 1) {
			throw new RuntimeException("only one paramer allowed: folder");
		}
		File folder = new File(params[0]);
		if(!folder.exists()) {
			throw new RuntimeException("folder does not exist");
		}
		if(!folder.isDirectory()) {
			throw new RuntimeException("is not a folder");
		}
		traverseFolder(folder.toPath());
	}

	private long traverseFolder(Path folder) {
		LocalDateTime start = LocalDateTime.now();
		long counter = 0;
		Logger.info("traverse " + folder);
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(folder);
			for(Path path: dirStream) {
				File file = path.toFile();
				if(file.isDirectory()) {
					counter += traverseFolder(path);
				} else if(file.isFile()) {
					boolean ret = traverseFile(file);
					if(ret) {
						counter++;
					}
				} else {
					Logger.warn("unknown entry: " + file.toString());
				}
			}
		} catch (Exception e) {
			Logger.warn(e);
		}
		LocalDateTime end = LocalDateTime.now();
		Duration duration = Duration.between(start, end);
		if(counter > 0) {			
			Duration durationPerFile = duration.dividedBy(counter);
			Logger.info("traversed " + folder + "  " + counter + " files  in " + duration + "     " + durationPerFile + " per file");
		} else {
			Logger.info("traversed " + folder + "  " + counter + " files  in " + duration);
		}

		return counter;
	}

	protected boolean traverseFile(File file) {
		String path = file.getPath();
		if(path.endsWith(".wav") || path.endsWith(".WAV")) {
			String yamlPath = path + ".yaml";
			File yamlFile = new File(yamlPath);
			if(!yamlFile.exists()) {
				return MetaCreator.createYaml(file, yamlFile.toPath());
			}
		}
		return false;		
	}
}
