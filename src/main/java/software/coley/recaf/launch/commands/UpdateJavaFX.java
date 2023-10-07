package software.coley.recaf.launch.commands;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.json.XML;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launch.info.JavaFxPlatform;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.Web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Command for updating JavaFX.
 */
@Command(name = "update-jfx", description = "Updates JavaFX")
public class UpdateJavaFX implements Callable<JavaFxVersion> {
	private static final String JFX_METADATA = "https://repo1.maven.org/maven2/org/openjfx/javafx-base/maven-metadata.xml";
	@Option(names = "c", description = "Clear the dependency cache")
	private boolean clear;
	@Option(names = "maxc", description = "Clear the dependency cache when this many files occupy it")
	private int maxCacheCount = Integer.MAX_VALUE;
	@Option(names = "maxs", description = "Clear the dependency cache when this many bytes occupy it")
	private long maxCacheSize = Integer.MAX_VALUE;
	@Option(names = "k", description = "Keep latest cached dependency in the cache when clearing")
	private boolean keepLatest;
	@Option(names = "f", description = "Force re-downloading even if the local install looks up-to-date")
	private boolean force;
	@Option(names = "v", description = "Target JavaFX version to use, instead of whatever is the latest")
	private int version;

	@Override
	public JavaFxVersion call() {
		checkClearCache(clear, keepLatest, maxCacheCount, maxCacheSize);
		return update(version, force, true);
	}

	/**
	 * @param clear
	 * 		Clear the dependency cache
	 * @param keepLatest
	 * 		{@code true} to keep the latest version in the cache, clearing only older items.
	 * @param maxCacheCount
	 * 		Clear the dependency cache when this many files occupy it
	 * @param maxCacheSize
	 * 		Clear the dependency cache when this many bytes occupy it
	 */
	public static void checkClearCache(boolean clear, boolean keepLatest, int maxCacheCount, long maxCacheSize) {
		if (clear ||
				getCachedFileCount() > maxCacheCount ||
				getCachedFileSize() > maxCacheSize)
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
	 * @param keepLatest {@code true} to keep the latest version in the cache, clearing only older items.
	 */
	public static void clearCache(boolean keepLatest) {
		JavaFxVersion latestLocalVersion = getLocalVersion(false);
		Path dependenciesDir = CommonPaths.getDependenciesDir();
		try {
			Files.walkFileTree(dependenciesDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (keepLatest) {
						// Only delete if it's an old version
						JavaFxVersion versionOfFile = mapToVersion(file);
						if (versionOfFile == null || versionOfFile.isOlder(latestLocalVersion))
							Files.delete(file);
					} else {
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
			System.err.println("Failed clearing dependency cache");
			ex.printStackTrace();
		}
	}

	/**
	 * Downloads and caches the latest version of JavaFX if we're not already up-to-date.
	 *
	 * @param force
	 *        {@code true} to re-download the version even if a local one exists.
	 * @param log
	 *        {@code true} to log failure cases.
	 *
	 * @return Local version of JavaFX after update process.
	 */
	public static JavaFxVersion update(boolean force, boolean log) {
		return update(-1, force, log);
	}

	/**
	 * Downloads and caches the latest version of JavaFX if we're not already up-to-date.
	 *
	 * @param version
	 * 		Target JavaFX version to use, instead of whatever is the latest.
	 * 		Any value less than 11 <i>(JavaFX's first maven version)</i> to use the latest.
	 * @param force
	 *        {@code true} to re-download the version even if a local one exists.
	 * @param log
	 *        {@code true} to log failure cases.
	 *
	 * @return Local version of JavaFX after update process.
	 */
	public static JavaFxVersion update(int version, boolean force, boolean log) {
		JavaFxVersion latest = version < 11 ? getLatestVersion(log) : new JavaFxVersion(version);
		JavaFxVersion local = getLocalVersion(log);
		if (latest == null)
			return local;
		if (local == null)
			force = true;
		if (force || latest.isNewer(local)) {
			updateTo(latest, force, log);
			if (log) System.out.println("Updated to JavaFX " + latest.getVersion());
			return latest;
		}
		if (log) System.out.println("Current JavaFX is up-to-date " + local.getVersion());
		return local;
	}

	/**
	 * Downloads and caches the requested version of JavaFX.
	 *
	 * @param version
	 * 		Version to update to.
	 * @param force
	 *        {@code true} to re-download the version even if a local one exists.
	 * @param log
	 *        {@code true} to log failure cases.
	 */
	public static void updateTo(JavaFxVersion version, boolean force, boolean log) {
		JavaFxPlatform platform = JavaFxPlatform.detect();
		if (platform == JavaFxPlatform.UNSUPPORTED) {
			System.err.println("Could not detect a supported version of JavaFX to use for this system");
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
			String artifactUrlFormat = " https://repo1.maven.org/maven2/org/openjfx/%s/%s/" + artifactFormat;
			String localArtifact = String.format(artifactFormat, artifact, versionName, classifier);
			String artifactUrl = String.format(artifactUrlFormat, artifact, versionName, artifact, versionName, classifier);
			Path localPath = CommonPaths.getDependenciesDir().resolve(localArtifact);
			if (force || !Files.exists(localPath)) {
				try {
					byte[] download = Web.getBytes(artifactUrl);
					Files.copy(new ByteArrayInputStream(download), localPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ex) {
					if (log) {
						System.err.println("Failed downloading FX artifact: " + artifactUrl);
						ex.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * @param log
	 *        {@code true} to log failure cases.
	 *
	 * @return Locally cached JavaFX version in the Recaf directory.
	 */
	public static JavaFxVersion getLocalVersion(boolean log) {
		Path dependenciesDir = CommonPaths.getDependenciesDir();

		try {
			Optional<JavaFxVersion> maxVersion = Files.list(dependenciesDir)
					.map(UpdateJavaFX::mapToVersion)
					.filter(Objects::nonNull)
					.max(Comparator.naturalOrder());
			return maxVersion.orElse(null);
		} catch (IOException ex) {
			if (log) {
				System.err.println("Could not determine latest JavaFX version from local cache");
				ex.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * @param log
	 *        {@code true} to log failure cases.
	 *
	 * @return Latest remote JavaFX version.
	 */
	public static JavaFxVersion getLatestVersion(boolean log) {
		try {
			String metadataXml = Web.getText(JFX_METADATA);
			String metadataJson = XML.toJSONObject(metadataXml).toString();
			JsonObject metadata = Json.parse(metadataJson).asObject();
			JsonObject versioning = metadata.get("metadata").asObject().get("versioning").asObject();
			String version = versioning.getString("release", String.valueOf(JavaFxVersion.MIN_SUGGESTED));
			return new JavaFxVersion(version);
		} catch (IOException ex) {
			if (log) {
				System.err.println("Failed to retrieve latest JavaFX version information");
				ex.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * @param javafxDependency
	 * 		Local file path.
	 *
	 * @return Extracted version based on file name pattern.
	 */
	private static JavaFxVersion mapToVersion(Path javafxDependency) {
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
}
