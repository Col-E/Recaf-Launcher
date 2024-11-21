package software.coley.recaf.launcher.commands;

import org.slf4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launcher.info.PlatformType;
import software.coley.recaf.launcher.task.CompatibilityTasks;
import software.coley.recaf.launcher.util.Loggers;

import java.util.EnumSet;
import java.util.concurrent.Callable;

/**
 * Command for checking compatibility.
 */
@Command(name = "compatibility>", description = "Checks the current runtime for compatibility with Recaf 4.X")
public class Compatibility implements Callable<Boolean> {
	private static final Logger logger = Loggers.newLogger();

	@Option(names = {"-ss", "--skipSuggestions"}, description = "Skip solutions to detected problems")
	private boolean skipSuggestions;

	@Override
	public Boolean call() {
		return isCompatible(skipSuggestions);
	}

	/**
	 * @param skipSuggestions
	 * 		Skip logging solutions to detected problems.
	 *
	 * @return {@code true} when compatible.
	 */
	public static boolean isCompatible(boolean skipSuggestions) {
		EnumSet<CompatibilityTasks.CompatibilityProblem> problems = CompatibilityTasks.getRuntimeCompatibilityProblems();

		// Check and log problems
		int problemCount = problems.size();
		if (problemCount > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(problemCount == 1 ? "A problem was found that may lead to incompatibilities with Recaf" :
					"Multiple problems were found that may lead to incompatibilities with Recaf");
			for (CompatibilityTasks.CompatibilityProblem problem : problems)
				sb.append(" - ").append(problem.getMessage());
			if (!skipSuggestions) {
				String suffix = PlatformType.isLinux() ? " or your package manager" : "";
				sb.append("\nSuggestions:\n - Install OpenJDK 22 or higher from https://adoptium.net/temurin/releases/").append(suffix);
			}
			logger.warn(sb.toString());
			return false;
		}
		return true;
	}
}
