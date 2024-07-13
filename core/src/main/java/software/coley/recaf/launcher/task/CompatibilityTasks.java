package software.coley.recaf.launcher.task;

import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.info.PlatformType;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * Tasks for computing compatibility with Recaf.
 */
public class CompatibilityTasks {
	/**
	 * @return Set of potential compatibility problems.
	 */
	@Nonnull
	public static EnumSet<CompatibilityProblem> getCompatibilityProblem() {
		EnumSet<CompatibilityProblem> set = EnumSet.noneOf(CompatibilityProblem.class);

		// Warn about java compatibility with different versions
		int javaVersion = JavaVersion.get();
		if (javaVersion == JavaVersion.UNKNOWN_VERSION) {
			// Shouldn't happen, Java version should always be resolvable
			set.add(CompatibilityProblem.UNKNOWN_JAVA_VERSION);
		} else if (javaVersion < JavaVersion.MIN_COMPATIBLE) {
			// Recaf 2: 8+
			// Recaf 3: 11+
			// Recaf 4: 22+
			set.add(CompatibilityProblem.OUTDATED_JAVA_VERSION);
		}

		// Warn if current VM includes incompatible JavaFX versions
		int fxVersion = JavaFxTasks.detectClasspathVersion();
		switch (fxVersion) {
			case JavaFxTasks.ERR_NO_FX_FOUND:
				// We good, no JavaFX found locally
				break;
			case JavaFxTasks.ERR_CANNOT_REFLECT:
			case JavaFxTasks.ERR_CANNOT_PARSE:
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
		OUTDATED_BUNDLED_JAVA_FX(() -> "Outdated JavaFX bundled in Java Runtime (" + JavaFxTasks.detectClasspathVersion() + ")"),
		BUNDLED_JAVA_FX(() -> "JavaFX bundled in Java Runtime (" + JavaFxTasks.detectClasspathVersion() + ")");

		private final Supplier<String> message;

		CompatibilityProblem(@Nonnull Supplier<String> message) {
			this.message = message;
		}

		@Nonnull
		public String getMessage() {
			return message.get();
		}
	}
}
