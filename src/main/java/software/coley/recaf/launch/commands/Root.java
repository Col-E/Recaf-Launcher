package software.coley.recaf.launch.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * Root command. Registers the other commands.
 */
@Command(name = "<launcher>", subcommands = {
		Auto.class, Compatibility.class, UpdateJavaFX.class, UpdateRecaf.class, UpdateRecafFromCI.class,
		GetVersion.class, Run.class, SetDefaultAction.class
})
public class Root implements Callable<Void> {
	private static final Logger logger = LoggerFactory.getLogger(Root.class);

	@Spec
	private CommandLine.Model.CommandSpec spec;

	@Override
	public Void call() throws Exception {
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
