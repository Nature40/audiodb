package photo.task;

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

import photo.PhotoDB2;
import photo.PhotoProjectConfig;
import task.Cancelable;
import task.Description;
import task.Role;
import task.Tag;
import task.Task;

@Tag("photo")
@Description("Traverse folder and subfolders of root_data_path and, for all JPG image files without the YAML metadata file in folder of root_path, create a new YAML file. As preliminary location name, the path from root_data_path up to image file is set. If image file is directly at root_data_path, no location is set. The date is parsed from image file name (currently only format: '*_yyyyMMdd_HHmmss.jpg'). If not possible, date is parsed from image internal meta data. If not possible, no date is set.")
@Cancelable
@Role("admin")
public class Task_photo_create_yaml extends Task {

	private Path root_path;
	private Path root_data_path;
	private boolean root_path_same_root_data_path;
	private PhotoProjectConfig photoProjectConfig;

	private Path getYamlPath(Path path) {
		if(!root_path_same_root_data_path) {
			path = root_path.resolve(root_data_path.relativize(path));
		} 
		Path yamlPath = Paths.get(path.toString() + ".yaml");
		return yamlPath;		
	}

	@Override
	protected void init() throws Exception {
		PhotoProjectConfig[] values = ctx.broker.config().photoConfig.projectMap.values().toArray(PhotoProjectConfig[]::new);
		if(values.length < 1) {
			throw new RuntimeException("missing photo project");
		}
		PhotoProjectConfig photoProjectConfig = values[0];
		this.photoProjectConfig = photoProjectConfig;
		root_path = photoProjectConfig.root_path;
		root_data_path = photoProjectConfig.root_data_path;
		root_path_same_root_data_path = root_path == root_data_path;
	}

	@Override
	public void run() {
		traverseFolder(root_data_path, root_data_path);
		setMessage("refresh PhotoDB");
		ctx.broker.photodb2().refresh();
		setMessage("done");
	}

	private long traverseFolder(Path folder, Path rootPath) {
		if(isSoftCanceled()) {
			throw new RuntimeException("canceled");
		}
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
				if(isSoftCanceled()) {
					throw new RuntimeException("canceled");
				}
				File file = path.toFile();
				if(file.isDirectory()) {
					counter += traverseFolder(path, rootPath);					
				} else if(file.isFile()) {					
					boolean ret = traverseFile(path, file, rootPath);
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

	protected boolean traverseFile(Path path, File file, Path rootPath) {
		//Logger.info("file " + path);
		String filename = file.getName();
		if((filename.endsWith(".jpg") || filename.endsWith(".JPG")) && !(filename.contains(".jpg.") || filename.contains(".JPG."))) {
			try {
				long fileSize = Files.size(path);
				if(fileSize > 0) {
					Path yamlPath = getYamlPath(path);
					//Logger.info("yamlPath " + yamlPath);
					File yamlFile = yamlPath.toFile();
					if(!yamlFile.exists()) {
						Path relativePath = rootPath.relativize(path);
						Path parentPath = relativePath.getParent();
						String missingLocation = parentPath != null ? parentPath.toString() : PhotoDB2.NO_LOCATION;
						yamlFile.getParentFile().mkdirs();
						return MetaCreator.createYaml(file, yamlPath, missingLocation, parentPath, photoProjectConfig);
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