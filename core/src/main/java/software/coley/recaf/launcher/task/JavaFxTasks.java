package software.coley.recaf.launcher.task;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.json.XML;
import org.slf4j.Logger;
import software.coley.recaf.launcher.info.ArchitectureType;
import software.coley.recaf.launcher.info.JavaFxPlatform;
import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.info.PlatformType;
import software.coley.recaf.launcher.info.SystemInformation;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Hashing;
import software.coley.recaf.launcher.util.Loggers;
import software.coley.recaf.launcher.util.Reflection;
import software.coley.recaf.launcher.util.Web;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaFxTasks {
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
	public static final NavigableMap<Integer, Integer> JFX_SUPPORTED_JDK_MAP = new TreeMap<>();
	private static final Logger logger = Loggers.newLogger();
	private static final String JFX_METADATA = "https://repo1.maven.org/maven2/org/openjfx/javafx-base/maven-metadata.xml";
	private static int runtimeVersion;

	static {
		JavaFxTasks.JFX_SUPPORTED_JDK_MAP.put(0, 17); // Base case
		JavaFxTasks.JFX_SUPPORTED_JDK_MAP.put(23, 21); // JavaFX 23 requires Java 21 or higher)
	}

	/**
	 * @return Detected supported platform for the current system.
	 */
	@Nonnull
	public static JavaFxPlatform detectSystemPlatform() {
		switch (PlatformType.get()) {
			case WINDOWS:
				return JavaFxPlatform.WINDOWS;
			case MAC:
				if (ArchitectureTasks.get() == ArchitectureType.AARCH64)
					return JavaFxPlatform.MAC_AARCH64;
				return JavaFxPlatform.MAC;
			case LINUX:
				if (ArchitectureTasks.get() == ArchitectureType.AARCH64)
					return JavaFxPlatform.LINUX_AARCH64;
				return JavaFxPlatform.LINUX;
			default:
				return JavaFxPlatform.UNSUPPORTED;
		}
	}

	/**
	 * @return Locally cached JavaFX version in the Recaf directory, or {@code null} if not known/installed.
	 */
	@Nullable
	public static JavaFxVersion detectCachedVersion() {
		Path dependenciesDir = CommonPaths.getDependenciesDir();
		if (!Files.isDirectory(dependenciesDir)) return null;
		try {
			Optional<JavaFxVersion> maxVersion = Files.list(dependenciesDir)
					.map(JavaFxTasks::mapToVersion)
					.filter(Objects::nonNull)
					.max(Comparator.naturalOrder());
			return maxVersion.orElse(null);
		} catch (IOException ex) {
			logger.error("Could not determine latest JavaFX version from local cache", ex);
			return null;
		}
	}

	/**
	 * @param compatibilityWithCurrentVM
	 *        {@code true} to filter out remote results which are not compatible with the current Java version.
	 *        {@code false} to not filter results based on Java version compatibility.
	 *
	 * @return Latest remote JavaFX version.
	 */
	@Nullable
	public static JavaFxVersion detectLatestRemoteVersion(boolean compatibilityWithCurrentVM) {
		try {
			String metadataXml = Web.getText(JFX_METADATA);
			String metadataJson = XML.toJSONObject(metadataXml).toString();
			JsonObject metadata = Json.parse(metadataJson).asObject();
			JsonObject versioning = metadata.get("metadata").asObject().get("versioning").asObject();
			JsonArray versions = versioning.get("versions").asObject().get("version").asArray();

			// Newer versions are last in the array.
			int currentJavaVersion = JavaVersion.get();
			for (int i = versions.size() - 1; i > 0; i--) {
				// The XML scheme handling in this json library is... kinda annoying.
				//   <version>11.0.1</version ---> String
				//   <version>11</version ---> int
				// It doesn't enforce a flat type for the repeated elements.
				String versionString;
				JsonValue version = versions.get(i);
				if (version.isString())
					versionString = version.asString();
				else if (version.isNumber())
					versionString = String.valueOf(version.asInt());
				else
					versionString = String.valueOf(JavaFxVersion.MIN_SUGGESTED); // Fallback.

				JavaFxVersion latestVersion = new JavaFxVersion(versionString);

				// Only return this version if its compatible with the current JDK.
				int major = latestVersion.getMajorVersion();
				int requiredJavaVersion = JFX_SUPPORTED_JDK_MAP.floorEntry(major).getValue();
				if (!compatibilityWithCurrentVM || currentJavaVersion >= requiredJavaVersion)
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
	@Nullable
	private static JavaFxVersion mapToVersion(@Nonnull Path javafxDependency) {
		JavaFxPlatform platform = detectSystemPlatform();
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
	public static int detectClasspathVersion() {
		if (runtimeVersion == 0)
			runtimeVersion = computeClasspathVersion();
		return runtimeVersion;
	}

	private static int computeClasspathVersion() {
		try {
			// Required for newer JDK's - Allows global reflection.
			logger.debug("Attempting to resolve JFX version in current classpath...");
			Reflection.setup();

			// Get class if available
			String jfxVersionClass = "com.sun.javafx.runtime.VersionInfo";
			Class<?> versionClass = Class.forName(jfxVersionClass);
			logger.debug("JFX found in classpath, checking for version info...");

			// Get release version string
			logger.debug("Getting version info from classpath JFX 'VersionInfo' class...");
			Method getVersion = versionClass.getDeclaredMethod("getVersion");
			getVersion.setAccessible(true);
			String version = String.valueOf(getVersion.invoke(null));

			// Extract major version to int
			// Should be the first int
			logger.debug("Classpath's JavaFX version reported: '{}'", version);
			Matcher matcher = Pattern.compile("\\d+").matcher(version);
			if (matcher.find())
				return Integer.parseInt(matcher.group());
			logger.error("Could not resolve classpath's JavaFX version from given: '{}'", version);
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

	/**
	 * @param clear
	 * 		Clear the dependency cache
	 * @param keepLatest
	 *        {@code true} to keep the latest version in the cache, clearing only older items.
	 * @param maxCacheCount
	 * 		Clear the dependency cache when this many files occupy it
	 * @param maxCacheSize
	 * 		Clear the dependency cache when this many bytes occupy it
	 */
	public static void checkClearCache(boolean clear, boolean keepLatest, int maxCacheCount, long maxCacheSize) {
		int cachedFileCount = getCachedFileCount();
		long cachedFileSize = getCachedFileSize();
		logger.debug("JFX cache:\n"
				+ " - Files: {}\n"
				+ " - Size:  {}", cachedFileSize, cachedFileSize);
		if (clear ||
				cachedFileCount > maxCacheCount ||
				cachedFileSize > maxCacheSize)
			clearCache(keepLatest);
	}

	/**
	 * @return Number of bytes in the dependency cache.
	 */
	public static long getCachedFileSize() {
		File dir = CommonPaths.getDependenciesDir().toFile();
		if (!dir.exists()) return 0;
		long size = 0;
		for (File file : Objects.requireNonNull(dir.listFiles()))
			size += file.length();
		return size;
	}

	/**
	 * @return Number of files in the dependency cache.
	 */
	public static int getCachedFileCount() {
		File dir = CommonPaths.getDependenciesDir().toFile();
		if (!dir.exists()) return 0;
		return Objects.requireNonNull(dir.list()).length;
	}

	/**
	 * Clear the local dependency cache.
	 *
	 * @param keepLatest
	 *        {@code true} to keep the latest version in the cache, clearing only older items.
	 */
	public static void clearCache(boolean keepLatest) {
		logger.debug("Clearing dependency cache" + (keepLatest ? ", keeping latest entries" : ""));
		JavaFxVersion latestLocalVersion = detectCachedVersion();
		Path dependenciesDir = CommonPaths.getDependenciesDir();
		try {
			if (!Files.isDirectory(dependenciesDir)) return;
			Files.walkFileTree(dependenciesDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (keepLatest) {
						// Only delete if it's an old version
						JavaFxVersion versionOfFile = mapToVersion(file);
						if (versionOfFile == null || versionOfFile.isOlder(latestLocalVersion)) {
							logger.debug("Deleting dependency {}", file.getFileName());
							Files.delete(file);
						}
					} else {
						logger.debug("Deleting dependency {}", file.getFileName());
						Files.delete(file);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException ex) {
			logger.error("Failed clearing dependency cache", ex);
		}
	}

	/**
	 * Downloads and caches the latest version of JavaFX if we're not already up-to-date.
	 *
	 * @param force
	 *        {@code true} to re-download the version even if a local one exists.
	 *
	 * @return Local version of JavaFX after update process.
	 */
	@Nullable
	public static JavaFxVersion update(boolean force) {
		return update(-1, force);
	}

	/**
	 * Downloads and caches the latest version of JavaFX if we're not already up-to-date.
	 *
	 * @param version
	 * 		Target JavaFX version to use, instead of whatever is the latest.
	 * 		Any value less than 11 <i>(JavaFX's first maven version)</i> to use the latest.
	 * @param force
	 *        {@code true} to re-download the version even if a local one exists.
	 *
	 * @return Local version of JavaFX after update process.
	 * Can be {@code null} if the local version is missing and the remote version cannot be checked.
	 */
	@Nullable
	public static JavaFxVersion update(int version, boolean force) {
		JavaFxVersion latest = version < 11 ? detectLatestRemoteVersion(false) : new JavaFxVersion(version);
		JavaFxVersion local = detectCachedVersion();
		if (latest == null)
			return local;
		if (local == null)
			force = true;
		if (force || latest.isNewer(local)) {
			try {
				updateTo(latest, force);
				logger.info("Updated to JavaFX '{}'", latest.getVersion());
				return latest;
			} catch (Throwable t) {
				logger.error("Failed updating JavaFX to '{}'", latest.getVersion(), t);
				return local;
			}
		}
		logger.info("Current JavaFX is up-to-date: '{}'", local.getVersion());
		return local;
	}

	/**
	 * Downloads and caches the requested version of JavaFX.
	 *
	 * @param version
	 * 		Version to update to.
	 * @param force
	 *        {@code true} to re-download the version even if a local one exists.
	 */
	public static void updateTo(@Nonnull JavaFxVersion version, boolean force) {
		JavaFxPlatform platform = detectSystemPlatform();
		if (platform == JavaFxPlatform.UNSUPPORTED) {
			logger.warn("Could not detect a supported version of JavaFX to use for this system:\n"
					+ " - OS:   {} ({})\n"
					+ " - Arch: {}\n", SystemInformation.OS_NAME, SystemInformation.OS_VERSION, SystemInformation.OS_ARCH);
			return;
		}

		// Need to download the artifacts with this pattern:
		//  https://repo1.maven.org/maven2/org/openjfx/<ARTIFACT>/<VERSION>/
		//   <ARTIFACT>-<VERSION>-<CLASSIFIER>.jar
		String versionName = version.getVersion();
		String classifier = platform.getClassifier();
		String[] artifacts = {"javafx-base", "javafx-graphics", "javafx-controls", "javafx-media"};
		for (String artifact : artifacts) {
			String artifactFormat = "%s-%s-%s.jar";
			String artifactUrlFormat = "https://repo1.maven.org/maven2/org/openjfx/%s/%s/" + artifactFormat;
			String localArtifact = String.format(artifactFormat, artifact, versionName, classifier);
			String artifactUrl = String.format(artifactUrlFormat, artifact, versionName, artifact, versionName, classifier);
			String artifactUrlSha1 = artifactUrl + ".sha1";
			Path dependenciesDir = CommonPaths.getDependenciesDir();
			Path localPath = dependenciesDir.resolve(localArtifact);
			boolean localPathExists = Files.exists(localPath);
			if (force || !localPathExists) {
				try {
					String expectedSha1 = Web.getText(artifactUrlSha1).trim();
					String actualSha1;
					if (localPathExists) {
						// Skip if the local file hash exactly matches the expected hash reported by maven central
						actualSha1 = Hashing.sha1(Files.newInputStream(localPath));
						if (actualSha1.equals(expectedSha1))
							continue;
					}

					// Ensure parent directory exists before writing
					if (!Files.isDirectory(dependenciesDir)) Files.createDirectories(dependenciesDir);

					int tries = 5;
					while (tries-- > 0) {
						// Download the file to the local path
						Web.consumeStream(artifactUrl, stream -> Files.copy(stream, localPath, StandardCopyOption.REPLACE_EXISTING));

						// Validate the file hash matches, try again if it does not match
						actualSha1 = Hashing.sha1(Files.newInputStream(localPath));
						if (actualSha1.equals(expectedSha1))
							break;
						else
							logger.error("Downloaded FX artifact '{}' but the SHA1 hash did not match " +
									"(expected={} vs local={}), retries remaining={}", artifact, tries, expectedSha1, actualSha1);
					}
				} catch (IOException ex) {
					logger.error("Failed downloading FX artifact: '{}'", artifactUrl, ex);
				}
			}
		}
	}
}
