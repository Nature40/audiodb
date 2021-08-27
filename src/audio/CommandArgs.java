package audio;

@FunctionalInterface
public interface CommandArgs extends Command {
	default void execute(String command, String[] params) throws Exception {		
		main(params);
	}
	void main(String[] params) throws Exception;
}
