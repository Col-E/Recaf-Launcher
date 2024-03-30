package software.coley.recaf.launch.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import software.coley.recaf.launch.util.Config;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Command for specifying a default action to run, which will be used when invoking the launcher with no arguments
 * in any following execution.
 */
@Command(name = "set-default-action", description = "Allows specifying a default action to run when no arguments are specified. " +
		"For commands with spaces in them, surround the whole command with quotes.")
public class SetDefaultAction implements Callable<Void> {
	private static final Logger logger = LoggerFactory.getLogger(SetDefaultAction.class);

	@Parameters(index = "0", description = "The action to run. Should match one of the launcher commands.")
	private String action;

	@Override
	public Void call() {
		if ("set-default-action".equals(action)) {
			logger.error("You cannot set the default action to be a recursive setting of the default action");
			return null;
		}

		// Collect commands.
		Set<String> commandNames = new TreeSet<>();
		Command rootCommand = Root.class.getDeclaredAnnotation(Command.class);
		Class<?>[] subcommands = rootCommand.subcommands();
		for (Class<?> subcommand : subcommands) {
			commandNames.add(subcommand.getDeclaredAnnotation(Command.class).name());
		}

		// Set it if it matches.
		if (commandNames.contains(action)) {
			Config.getInstance().setDefaultAction(action);
		} else {
			logger.error("The value '" + action + "' did not match any existing command name.\n" +
					"The available commands are:\n" +
					" - " + String.join("\n - ", commandNames));
		}

		return null;
	}
}
