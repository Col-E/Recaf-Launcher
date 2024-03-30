package software.coley.recaf.launch.info;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.Reflection;
import software.coley.recaf.launch.util.Web;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
	public static final NavigableMap<Integer, Integer> JFX_SUPPORTED_JDK_MAP = new TreeMap<>(Map.of(
			0, 17, // Base case, Recaf does not go below JDK 17
			23, 21 // JavaFX 23 requires Java 21 or higher
	));
	private static final String JFX_METADATA = "https://repo1.maven.org/maven2/org/openjfx/javafx-base/maven-metadata.xml";

	private static int runtimeVersion;

	private final String version;

	public JavaFxVersion(String version) {
		this.version = version;
	}

	public JavaFxVersion(int version) {
		this.version = String.valueOf(version);
	}

	/**
	 * @return Major release version of JavaFX.
	 */
	public int getMajorVersion() {
		if (version.matches("\\d+"))
			return Integer.parseInt(version);
		if (version.contains("."))
			return Integer.parseInt(version.substring(0, version.indexOf('.')));
		if (version.contains("-"))
			return Integer.parseInt(version.substring(0, version.indexOf('-')));
		throw new IllegalStateException("Cannot map JFX version to major version: " + version);
	}

	/**
	 * @return Locally cached JavaFX version in the Recaf directory, or {@code null} if not known/installed.
	 */
	public static JavaFxVersion getLocalVersion() {
		Path dependenciesDir = CommonPaths.getDependenciesDir();
		if (!Files.isDirectory(dependenciesDir)) return null;
		try {
			Optional<JavaFxVersion> maxVersion = Files.list(dependenciesDir)
					.map(JavaFxVersion::mapToVersion)
					.filter(Objects::nonNull)
					.max(Comparator.naturalOrder());
			return maxVersion.orElse(null);
		} catch (IOException ex) {
			logger.error("Could not determine latest JavaFX version from local cache", ex);
			return null;
		}
	}

	/**
	 * @return Latest remote JavaFX version.
	 */
	public static JavaFxVersion getLatestVersion() {
		try {
			String metadataXml = Web.getText(JFX_METADATA);
			String metadataJson = XML.toJSONObject(metadataXml).toString();
			JsonObject metadata = Json.parse(metadataJson).asObject();
			JsonObject versioning = metadata.get("metadata").asObject().get("versioning").asObject();
			JsonArray versions = versioning.get("versions").asObject().get("version").asArray();

			// Newer versions are last in the array.
			int currentJavaVersion = JavaVersion.get();
			for (int i = versions.size() - 1; i > 0; i--) {
				JsonValue version = versions.get(i);
				JavaFxVersion latestVersion = new JavaFxVersion(version.asString());

				// Only return this version if its compatible with the current JDK.
				int major = latestVersion.getMajorVersion();
				int requiredJavaVersion  = JavaFxVersion.JFX_SUPPORTED_JDK_MAP.floorEntry(major).getValue();
				if (currentJavaVersion >= requiredJavaVersion)
					return latestVersion;
			}

			logger.error("Failed to find a compatible JavaFX version");
			return null;
		} catch (IOException ex) {
			logger.error("Failed to retrieve latest JavaFX version information", ex);
			return null;
		}
	}

	/**
	 * @param javafxDependency
	 * 		Local file path.
	 *
	 * @return Extracted version based on file name pattern.
	 */
	public static JavaFxVersion mapToVersion(Path javafxDependency) {
		JavaFxPlatform platform = JavaFxPlatform.detect();
		String name = javafxDependency.getFileName().toString();
		String[] prefixes = {
				"javafx-base-",
				"javafx-controls-",
				"javafx-fxml-",
				"javafx-graphics-",
				"javafx-media-",
				"javafx-swing-",
				"javafx-web-"
		};
		for (String prefix : prefixes) {
			if (name.startsWith(prefix)) {
				int prefixLength = prefix.length();
				String version = name.substring(prefixLength, name.indexOf("-" + platform.getClassifier(), prefixLength));
				return new JavaFxVersion(version);
			}
		}
		return null;
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
			Method getVersion = versionClass.getDeclaredMethod("getVersion");
			getVersion.setAccessible(true);
			String version = String.valueOf(getVersion.invoke(null));

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
