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
@Command(name = "update", description = "Updates Recaf")
public class UpdateRecafStable implements Callable<RecafVersion> {
	private static final Logger logger = Loggers.newLogger();

	@Override
	public RecafVersion call() {
		VersionUpdateResult result = RecafTasks.updateFromStable();

		if (result.getError() != null) {
			logger.error("Encountered error updating Recaf from latest stable release", result.getError());
			return null;
		}

		return (RecafVersion) result.getTo();
	}
}