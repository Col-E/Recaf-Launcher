package software.coley.recaf.launch.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.info.JavaVersion;

import java.util.EnumSet;
import java.util.concurrent.Callable;

/**
 * Command for checking compatibility.
 */
@Command(name = "compatibility", description = "Checks the local system for compatibility with Recaf 4.X")
public class Compatibility implements Callable<Boolean> {
	@Option(names = "ss", description = "Skip solutions to detected problems")
	private boolean skipSuggestions;
	@Option(names = "ifx", description = "Ignore problems with the local system's bundled JavaFX version")
	private boolean ignoreBundledFx;

	@Override
	public Boolean call() {
		return isCompatible(ignoreBundledFx, skipSuggestions, true);
	}

	/**
	 * @param ignoreBundledFx
	 * 		Ignore problems with the local system's bundled JavaFX version.
	 * @param skipSuggestions
	 * 		Skip logging solutions to detected problems.
	 * @param log
	 *        {@code true} to incompatibilities.
	 *
	 * @return {@code true} when compatible.
	 */
	public static boolean isCompatible(boolean ignoreBundledFx, boolean skipSuggestions, boolean log) {
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
			if (log) {
				System.err.println(problemCount == 1 ? "A problem was found that may lead to incompatibilities with Recaf" :
						"Multiple problems were found that may lead to incompatibilities with Recaf");
				for (CompatibilityProblem problem : problems) {
					System.err.println(" - " + problem.message);
				}
				if (!skipSuggestions) {
					System.err.println();
					System.err.println("Suggestions:");
					System.err.println(" - Install OpenJDK 17 or higher from https://adoptium.net/temurin/releases/");
				}
			}
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
		} else if (javaVersion < 17) {
			// Recaf 2: 8+
			// Recaf 3: 11+
			// Recaf 4: 17+
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
		UNKNOWN_JAVA_VERSION("Unknown Java version"),
		OUTDATED_JAVA_VERSION("Outdated Java version"),
		OUTDATED_BUNDLED_JAVA_FX("Outdated JavaFX bundled in Java Runtime"),
		BUNDLED_JAVA_FX("JavaFX bundled in Java Runtime");

		private final String message;

		CompatibilityProblem(String message) {
			this.message = message;
		}
	}
}
