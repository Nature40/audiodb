package audio.task;

import java.io.File;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.tinylog.Logger;

import audio.QoaMeta;
import task.Description;
import task.Descriptor.Param.Type;
import task.Param;
import task.Role;
import task.Tag;
import task.Task;

@Tag("audio")
@Description("Check QOA audio files.")
@Param(name = "src_path", type = Type.STRING, preset = "qoa_output", description = "Root path destination for QOA audio files to be checked.")
@Role("admin")
public class Task_audio_qoa_check extends Task {

	private Path src_root;

	@Override
	protected void init() {
		src_root = ctx.broker.config().audioConfig.root_data_path;
		Logger.info(this.ctx);
		String path = this.ctx.getParamString("src_path");
		src_root = Paths.get(path);
	}

	@Override
	protected void run() throws Exception {		
		traverseFolder(src_root);
	}

	private void traverseFolder(Path folder) {
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(folder);
			for(Path path: dirStream) {
				File file = path.toFile();
				if(file.isDirectory()) {
					traverseFolder(path);					
				} else if(file.isFile()) {
					traverseFile(file);
				} else {
					Logger.warn("unknown entry: " + file.toString());
				}
			}
		} catch (DirectoryIteratorException e){ 
			Logger.warn(e.getMessage());
		} catch (Exception e) {
			Logger.warn(e);
		}		
	}

	private void traverseFile(File file) {
		String filename = file.getName();
		if(Task_audio_create_yaml.isQoa(filename)) {
			Logger.info(file.toString());
			try {
				QoaMeta m = new QoaMeta(file);
			} catch (Exception e) {
				Logger.warn(e);
			}			
		}	
	}
}
