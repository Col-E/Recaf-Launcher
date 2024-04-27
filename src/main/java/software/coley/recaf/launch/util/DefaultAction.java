package software.coley.recaf.launch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import software.coley.recaf.launch.commands.Root;
import software.coley.recaf.launch.commands.SetDefaultAction;

/**
 * Util to handle running the default action.
 *
 * @see SetDefaultAction
 */
public class DefaultAction {
	private static final Logger logger = LoggerFactory.getLogger(DefaultAction.class);

	/**
	 * Run the default action if specified, otherwise list available commands.
	 */
	public static void handleDefaultAction() {
		String defaultAction = Config.getInstance().getDefaultAction();
		if (defaultAction != null) {
			String[] args = new String[]{defaultAction};
			new CommandLine(new Root()).execute(args);
		} else {
			logger.info("\n" +
					"===============================================================\n" +
					"You did not provide any launch arguments, or specify a default\n" +
					"to run in the launcher config.\n\n" +
					"You can open the GUI by running with 'javaw' instead of 'java'.\n" +
					"If you meant to provide launch arguments, their usage is below.\n" +
					"===============================================================\n\n");
			new CommandLine(new Root()).execute();
		}
	}
}
