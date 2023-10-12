package software.coley.recaf.launch;

import picocli.CommandLine;
import software.coley.recaf.launch.commands.Root;

import javax.swing.JOptionPane;


/**
 * Entry point class.
 */
public class Launcher {
	public static void main(String[] args) {
		// Check if user tried to run by double-clicking the jar instead of running from a console.
		if (System.console() == null && (args == null || args.length == 0)) {
			JOptionPane.showMessageDialog(null, "The launcher is a command line application, run it from the command line");
		}

		// Run the console commands.
		CommandLine cmd = new CommandLine(new Root());
		cmd.execute(args);
	}
}
