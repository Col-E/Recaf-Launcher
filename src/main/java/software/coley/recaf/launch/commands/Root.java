package software.coley.recaf.launch.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * Root command. Registers the other commands.
 */
@Command(name = "<launcher>", subcommands = {
		Auto.class, Compatibility.class, UpdateJavaFX.class, UpdateRecaf.class, UpdateRecafFromCI.class, GetVersion.class, Run.class
})
public class Root implements Callable<Void> {
	@Spec
	private CommandLine.Model.CommandSpec spec;

	@Override
	public Void call() throws Exception {
		// Providing no args should run this top-level command.
		// Tell the users what their options are.
		for (CommandLine command : spec.subcommands().values()) {
			// Skip hidden commands
			if (command.getCommandSpec().usageMessage().hidden())
				continue;

			String usageMessage = command.getUsageMessage(CommandLine.Help.Ansi.AUTO);
			System.out.println(usageMessage);
		}
		return null;
	}
}
