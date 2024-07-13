package software.coley.recaf.launcher.commands;

import org.slf4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import software.coley.recaf.launcher.util.Loggers;

import java.util.concurrent.Callable;

@Command(name = "<launcher>",
		subcommands = {
				Auto.class,
				Compatibility.class,
				Run.class,
				UpdateJavaFX.class,
				UpdateRecafSnapshot.class,
				UpdateRecafStable.class
		}
)
public class Root implements Callable<Void> {
	private static final Logger logger = Loggers.newLogger();
	@Spec
	private CommandSpec spec;

	@Override
	public Void call() {
		// Providing no args should run this top-level command.
		// Tell the users what their options are.
		StringBuilder sb = new StringBuilder();
		for (CommandLine command : spec.subcommands().values()) {
			// Skip hidden commands
			if (command.getCommandSpec().usageMessage().hidden())
				continue;

			String usageMessage = command.getUsageMessage(CommandLine.Help.Ansi.AUTO);
			sb.append(usageMessage).append('\n');
		}

		logger.info(sb.toString());
		return null;
	}
}
