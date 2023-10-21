package software.coley.recaf.launch.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.recaf.launch.util.Reflection;

import java.io.File;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JavaFX version information.
 */
public class JavaFxVersion implements Version {
	private static final Logger logger = LoggerFactory.getLogger(JavaFxVersion.class);

	/**
	 * Code for no local JavaFX detected.
	 */
	public static final int ERR_NO_FX_FOUND = -1;
	/**
	 * Code for local JavaFX detected, but cannot call method to get version info.
	 */
	public static final int ERR_CANNOT_REFLECT = -2;
	/**
	 * Code for local JavaFX detected, but cannot parse version info.
	 */
	public static final int ERR_CANNOT_PARSE = -3;
	/**
	 * Oldest version we'd suggest using.
	 */
	public static final int MIN_SUGGESTED = 21;

	private static int runtimeVersion;

	private final String version;

	public JavaFxVersion(String version) {
		this.version = version;
	}

	public JavaFxVersion(int version) {
		this.version = String.valueOf(version);
	}

	/**
	 * For failure codes, see: {@link #ERR_NO_FX_FOUND}, {@link #ERR_CANNOT_REFLECT}, {@link #ERR_CANNOT_PARSE}.
	 *
	 * @return JavaFX major version of current runtime, or failure code.
	 */
	public static int getRuntimeVersion() {
		if (runtimeVersion == 0)
			runtimeVersion = computeRuntimeVersion();
		return runtimeVersion;
	}

	private static int computeRuntimeVersion() {
		try {
			// Required for newer JDK's - Allows global reflection.
			logger.debug("Attempting to resolve JFX version...");
			Reflection.setup();

			// Get class if available
			String jfxVersionClass = "com.sun.javafx.runtime.VersionInfo";
			Class<?> versionClass = Class.forName(jfxVersionClass);
			logger.debug("JFX found, checking for version info...");

			// Get release version string
			logger.debug("Getting version info from JFX 'VersionInfo' class...");
			Method setupSystemProperties = versionClass.getDeclaredMethod("getVersion");
			setupSystemProperties.setAccessible(true);
			String version = String.valueOf(setupSystemProperties.invoke(null));

			// Extract major version to int
			// Should be the first int
			logger.debug("JavaFX version reported: '{}'", version);
			Matcher matcher = Pattern.compile("\\d+").matcher(version);
			if (matcher.find())
				return Integer.parseInt(matcher.group());
			logger.error("Could not resolve JavaFX version from given: '{}'", version);
			return ERR_CANNOT_PARSE;
		} catch (ClassNotFoundException ex) {
			logger.debug("No JavaFX version class found in the current classpath");
			return ERR_NO_FX_FOUND;
		} catch (ReflectiveOperationException e) {
			String[] cp = System.getProperty("java.class.path").split(File.pathSeparator);
			String suffix = cp.length == 0 ? "" : ":\n - " + String.join("\n - ", cp);
			logger.debug("Could not call 'VersionInfo.getVersion()' for JavaFX despite existing on the current classpath" + suffix);
			return ERR_CANNOT_REFLECT;
		} catch (Throwable t) {
			logger.error("Could not call 'VersionInfo.getVersion()' for JavaFX despite existing on the current classpath\n" +
					"Some unexpected error occurred that was not caught by Reflection.", t);
			return ERR_CANNOT_REFLECT;
		}
	}

	@Override
	public boolean isSnapshot() {
		return version.contains("-ea+");
	}

	@Override
	public JavaFxVersion withoutSnapshot() {
		String subversion = version.substring(0, version.length() - SNAPSHOT_SUFFIX.length());
		return new JavaFxVersion(subversion);
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JavaFxVersion that = (JavaFxVersion) o;

		return version.equals(that.version);
	}

	@Override
	public int hashCode() {
		return version.hashCode();
	}

	@Override
	public String toString() {
		return version;
	}
}
