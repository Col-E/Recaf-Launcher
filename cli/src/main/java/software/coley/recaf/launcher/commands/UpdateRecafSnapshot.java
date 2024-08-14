package software.coley.recaf.launcher.commands;

import org.slf4j.Logger;
import picocli.CommandLine.Command;
import software.coley.recaf.launcher.info.RecafVersion;
import software.coley.recaf.launcher.task.RecafTasks;
import software.coley.recaf.launcher.task.VersionUpdateResult;
import software.coley.recaf.launcher.util.Loggers;

import java.util.concurrent.Callable;

/**
 * Command for updating Recaf.
 */
@Command(name = "update-ci", description = "Updates Recaf")
public class UpdateRecafSnapshot implements Callable<RecafVersion> {
	private static final Logger logger = Loggers.newLogger();

	@Override
	public RecafVersion call() {
		VersionUpdateResult result = RecafTasks.updateFromSnapshot("master");

		if (result.getError() != null) {
			logger.error("Encountered error updating Recaf from latest snapshot", result.getError());
			return null;
		}

		return (RecafVersion) result.getTo();
	}
}