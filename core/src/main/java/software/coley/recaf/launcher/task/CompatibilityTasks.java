package software.coley.recaf.launcher.task;

import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.info.JavaVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * Tasks for computing compatibility with Recaf.
 */
public class CompatibilityTasks {
	/**
	 * @param javaVersion
	 * 		Java version to run with.
	 * @param fxVersion
	 * 		JavaFX version to use in the classpath.
	 *
	 * @return Set of potential compatibility problems for the given environment.
	 */
	@Nonnull
	public static EnumSet<CompatibilityProblem> getCompatibilityProblems(int javaVersion,
	                                                                     @Nullable JavaFxVersion fxVersion) {
		EnumSet<CompatibilityProblem> set = EnumSet.noneOf(CompatibilityProblem.class);

		// Warn about Java compatibility with different versions
		if (javaVersion == JavaVersion.UNKNOWN_VERSION) {
			// Shouldn't happen, Java version should always be resolvable
			set.add(CompatibilityProblem.UNKNOWN_JAVA_VERSION);
		} else if (javaVersion < JavaVersion.MIN_COMPATIBLE) {
			// Recaf 2: 8+
			// Recaf 3: 11+
			// Recaf 4: 22+
			set.add(CompatibilityProblem.OUTDATED_JAVA_VERSION);
		}

		// Warn about JavaFX compatibility with different versions
		if (fxVersion == null) {
			set.add(CompatibilityProblem.NO_CACHED_JAVA_FX);
		} else {
			try {
				int fx = fxVersion.getMajorVersion();
				if (fx < JavaFxVersion.MIN_SUGGESTED_JFX_VERSION)
					set.add(CompatibilityProblem.OUTDATED_JAVA_FX_ARTIFACTS);
				else if (!fxVersion.isCompatibleWith(javaVersion))
					set.add(CompatibilityProblem.TOO_NEW_JAVA_FX_ARTIFACTS);
			} catch (Throwable t) {
				// Can be thrown if the version string cannot be parsed to a major release integer
				set.add(CompatibilityProblem.OUTDATED_JAVA_FX_ARTIFACTS);
			}
		}

		return set;
	}

	/**
	 * @return Set of potential compatibility problems if
	 * Recaf were to be launched with the current runtime contents.
	 */
	@Nonnull
	public static EnumSet<CompatibilityProblem> getRuntimeCompatibilityProblems() {
		EnumSet<CompatibilityProblem> set = EnumSet.noneOf(CompatibilityProblem.class);

		// Warn about Java compatibility with different versions
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

		return set;
	}

	/**
	 * Type of compatibility problem.
	 */
	public enum CompatibilityProblem {
		UNKNOWN_JAVA_VERSION(() -> "Unknown Java version"),
		OUTDATED_JAVA_VERSION(() -> "Outdated Java version (" + JavaVersion.get() + "), requires 22+"),
		OUTDATED_JAVA_FX_ARTIFACTS(() -> "Outdated JavaFX artifacts"),
		TOO_NEW_JAVA_FX_ARTIFACTS(() -> "JavaFX artifacts depend on higher version of Java"),
		NO_CACHED_JAVA_FX(() -> "Missing JavaFX artifacts");

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
