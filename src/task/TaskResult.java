package task;

import java.nio.file.Path;

public abstract class TaskResult {
	
	public static enum Type {
		TEXT, 
		FILE,
	}
	
	public interface Visitor {
		void text(Text text);
		void file(File file);
		
		default void visit(TaskResult ...results) {
			if(results != null) {
				for(TaskResult result:results) {
					visit(result);
				}
			}
		}
		default void visit(TaskResult result) {
			if(result != null) {
				switch(result.type) {
				case TEXT: {
					text((Text) result);
					break;
				}
				case FILE: {
					file((File) result);
					break;
				}
				default:
					throw new RuntimeException("unknown type: " + result.type);
				}
			}
		}
	}
	
	public static class Text extends TaskResult {
		public final String text;

		public Text(String text) {
			super(Type.TEXT);
			this.text = text;
		}
	}
	
	public static class File extends TaskResult {
		public final String filename;
		public final Path path;

		public File(String filename, Path path) {
			super(Type.FILE);
			this.filename = filename;
			this.path = path;
		}
	}
	
	public static Text ofText(String text) {
		return new Text(text);
	}
	
	public static File ofFile(Path path) {
		return new File(path.getFileName().toString(), path);
	}
	
	public final Type type;
	
	public TaskResult(Type type) {
		this.type = type;
	}
}
