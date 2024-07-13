package software.coley.recaf.launcher.commands;

import org.slf4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launcher.task.ExecutionTasks;
import software.coley.recaf.launcher.util.Loggers;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command for checking the currently installed version of Recaf.
 */
@Command(name = "run", description = "Runs the installed version of Recaf")
public class Run implements Callable<ExecutionTasks.RunResult> {
	private static final Logger logger = Loggers.newLogger();

	@Option(names = {"-j", "--java"}, description = {
			"Path of Java executable to use when running Recaf.",
			"Not specifying a value will use the same Java executable used by the launcher."
	})
	private File javaExecutable;

	@Override
	public ExecutionTasks.RunResult call() throws Exception {
		try {
			String javaExecutablePath = javaExecutable == null ? null : javaExecutable.getAbsolutePath();
			return ExecutionTasks.run(true, javaExecutablePath);
		} catch (IOException ex) {
			logger.error("Encountered error running Recaf", ex);
			throw ex;
		}
	}
}
