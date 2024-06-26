package software.coley.recaf.launch.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.info.JavaVersion;
import software.coley.recaf.launch.info.PlatformType;

import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Command for checking compatibility.
 */
@Command(name = "compatibility", description = "Checks the local system for compatibility with Recaf 4.X")
public class Compatibility implements Callable<Boolean> {
	private static final Logger logger = LoggerFactory.getLogger(Compatibility.class);

	@Option(names = {"-ss", "--skipSuggestions"}, description = "Skip solutions to detected problems")
	private boolean skipSuggestions;
	@Option(names = {"-ifx", "--ignoreBundledFx"}, description = "Ignore problems with the local system's bundled JavaFX version")
	private boolean ignoreBundledFx;

	@Override
	public Boolean call() {
		return isCompatible(ignoreBundledFx, skipSuggestions);
	}

	/**
	 * @param ignoreBundledFx
	 * 		Ignore problems with the local system's bundled JavaFX version.
	 * @param skipSuggestions
	 * 		Skip logging solutions to detected problems.
	 *
	 * @return {@code true} when compatible.
	 */
	public static boolean isCompatible(boolean ignoreBundledFx, boolean skipSuggestions) {
		EnumSet<CompatibilityProblem> problems = getCompatibilityProblem();
		if (ignoreBundledFx) {
			// Allow people to shoot themselves in the foot.
			// This is ideally used in situations where people have 'JavaFX >= 21' and
			// there are no API incompatibilities with Recaf.
			problems.remove(CompatibilityProblem.OUTDATED_BUNDLED_JAVA_FX);
			problems.remove(CompatibilityProblem.BUNDLED_JAVA_FX);
		}

		// Check and log problems
		int problemCount = problems.size();
		if (problemCount > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(problemCount == 1 ? "A problem was found that may lead to incompatibilities with Recaf" :
					"Multiple problems were found that may lead to incompatibilities with Recaf");
			for (CompatibilityProblem problem : problems)
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

	/**
	 * @return Set of potential compatibility problems.
	 */
	public static EnumSet<CompatibilityProblem> getCompatibilityProblem() {
		EnumSet<CompatibilityProblem> set = EnumSet.noneOf(CompatibilityProblem.class);

		// Warn about java compatibility with different versions
		int javaVersion = JavaVersion.get();
		if (javaVersion == JavaVersion.UNKNOWN_VERSION) {
			// Shouldn't happen, Java version should always be resolvable
			set.add(CompatibilityProblem.UNKNOWN_JAVA_VERSION);
		} else if (javaVersion < 22) {
			// Recaf 2: 8+
			// Recaf 3: 11+
			// Recaf 4: 22+
			set.add(CompatibilityProblem.OUTDATED_JAVA_VERSION);
		}

		// Warn if current VM includes incompatible JavaFX versions
		int fxVersion = JavaFxVersion.getRuntimeVersion();
		switch (fxVersion) {
			case JavaFxVersion.ERR_NO_FX_FOUND:
				// We good, no JavaFX found locally
				break;
			case JavaFxVersion.ERR_CANNOT_REFLECT:
			case JavaFxVersion.ERR_CANNOT_PARSE:
				// Unknown JavaFX version, assume its bad.
				set.add(CompatibilityProblem.OUTDATED_BUNDLED_JAVA_FX);
				break;
			default:
				// Recaf uses some versions of JavaFX 20, but pulls in newer versions
				// for bug fixes and performance improvements. We'll suggest using 21+
				// and complain about anything less for now.
				if (fxVersion < 21) {
					set.add(CompatibilityProblem.OUTDATED_BUNDLED_JAVA_FX);
				} else {
					set.add(CompatibilityProblem.BUNDLED_JAVA_FX);
				}
		}

		return set;
	}

	/**
	 * Type of compatibility problem.
	 */
	public enum CompatibilityProblem {
		UNKNOWN_JAVA_VERSION(() -> "Unknown Java version"),
		OUTDATED_JAVA_VERSION(() -> "Outdated Java version (" + JavaVersion.get() + "), requires 22+"),
		OUTDATED_BUNDLED_JAVA_FX(() -> "Outdated JavaFX bundled in Java Runtime (" + JavaFxVersion.getRuntimeVersion() + ")"),
		BUNDLED_JAVA_FX(() -> "JavaFX bundled in Java Runtime (" + JavaFxVersion.getRuntimeVersion() + ")");

		private final Supplier<String> message;

		CompatibilityProblem(Supplier<String> message) {
			this.message = message;
		}

		public String getMessage() {
			return message.get();
		}
	}
}
