package audio.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;

import org.tinylog.Logger;

import audio.AudioProjectConfig;
import audio.MetaCreator;

@Description("Traverse root_data_path and for all WAV files without YAML file in root_path create a new YAML file.")
public class Task_create_yaml extends Task {

	private Path root_path;
	private Path root_data_path;
	private boolean root_path_same_root_data_path;

	private Path getYamlPath(Path path) {
		if(!root_path_same_root_data_path) {
			path = root_path.resolve(root_data_path.relativize(path));
		} 
		Path yamlPath = Paths.get(path.toString() + ".yaml");
		return yamlPath;		
	}

	@Override
	protected void init() throws Exception {
		AudioProjectConfig config = ctx.broker.config().audioConfig;
		root_path = config.root_path;
		root_data_path = config.root_data_path;
		root_path_same_root_data_path = root_path == root_data_path;
	}

	@Override
	public void run() {
		traverseFolder(root_data_path);
	}

	private long traverseFolder(Path folder) {
		//Logger.info("folder " + folder);
		LocalDateTime start = LocalDateTime.now();
		long counter = 0;
		{
			String m = "traverse " + folder;
			setMessage(m);
			Logger.info(m);
		}
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(folder);
			for(Path path: dirStream) {
				File file = path.toFile();
				if(file.isDirectory()) {
					counter += traverseFolder(path);					
				} else if(file.isFile()) {
					boolean ret = traverseFile(path, file);
					if(ret) {
						counter++;
					}
				} else {
					Logger.warn("unknown entry: " + file.toString());
				}
			}
		} catch (DirectoryIteratorException e){ 
			Logger.warn(e.getMessage());
		} catch (Exception e) {
			Logger.warn(e);
		}
		LocalDateTime end = LocalDateTime.now();
		Duration duration = Duration.between(start, end);
		if(counter > 0) {			
			Duration durationPerFile = duration.dividedBy(counter);
			String m = "traversed " + folder + "  " + counter + " files  in " + duration + "     " + durationPerFile + " per file";
			setMessage(m);
			Logger.info(m);
		} else {
			String m = "traversed " + folder + " with no files  in " + duration;
			setMessage(m);
			Logger.info(m);
		}

		return counter;
	}

	protected boolean traverseFile(Path path, File file) {
		//Logger.info("file " + path);
		String filename = file.getName();
		if(filename.endsWith(".wav") || filename.endsWith(".WAV")) {
			try {
				long fileSize = Files.size(path);
				if(fileSize > 0) {
					Path yamlPath = getYamlPath(path);
					//Logger.info("yamlPath " + yamlPath);
					File yamlFile = yamlPath.toFile();
					if(!yamlFile.exists()) {
						yamlFile.getParentFile().mkdirs();
						return MetaCreator.createYaml(file, yamlPath);
					} else {
						//Logger.info("already exists " + path);
					}	
				}
			} catch (IOException e) {
				Logger.warn(e);
			}			
		}
		return false;		
	}
}
