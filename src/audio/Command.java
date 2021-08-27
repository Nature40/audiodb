package audio;

@FunctionalInterface
interface Command {
	void execute(String command, String[] params) throws Exception;
}
