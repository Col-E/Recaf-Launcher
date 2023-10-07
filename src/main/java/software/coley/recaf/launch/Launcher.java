package software.coley.recaf.launch;

import picocli.CommandLine;
import software.coley.recaf.launch.commands.Root;

/**
 * Entry point class.
 */
public class Launcher {
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new Root());
		cmd.execute(args);
	}
}
