package audio;

import org.tinylog.Logger;

public final class CommandlineConfig {

	public static final CommandlineConfig DEFAULT = new CommandlineConfig(new Builder());

	public final boolean no_yaml_scan;

	public static class Builder {

		public boolean no_yaml_scan = false;

		public Builder() {}

		public Builder(String[] args) {
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				switch(arg) {
				case "--no-yaml-scan":
					break;
				default:
					throw new RuntimeException("unknown command line arg: |" + arg + "|   available args: --no-yaml-scan");
				}
			}
		}
	}

	public CommandlineConfig(Builder builder) {
		no_yaml_scan = builder.no_yaml_scan;

		Logger.info(this);
	}

	@Override
	public String toString() {
		return "CommandlineConfig [no_yaml_scan=" + no_yaml_scan + "]";
	}
}
