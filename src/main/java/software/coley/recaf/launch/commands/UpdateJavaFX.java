package software.coley.recaf.launch.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launch.info.JavaFxPlatform;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.info.SystemInformation;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.Web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Command for updating JavaFX.
 */
@Command(name = "update-jfx", description = "Updates JavaFX")
public class UpdateJavaFX implements Callable<JavaFxVersion> {
	private static final Logger logger = LoggerFactory.getLogger(UpdateJavaFX.class);

	@Option(names = {"-c", "--clear"}, description = "Clear the dependency cache")
	private boolean clear;
	@Option(names = {"-maxc", "--maxCacheCount"}, description = "Clear the dependency cache when this many files occupy it")
	private int maxCacheCount = Integer.MAX_VALUE;
	@Option(names = {"-maxs", "--maxCacheSize"}, description = "Clear the dependency cache when this many bytes occupy it")
	private long maxCacheSize = Integer.MAX_VALUE;
	@Option(names = {"-k", "--keepLatest"}, description = "Keep latest cached dependency in the cache when clearing")
	private boolean keepLatest;
	@Option(names = {"-f", "--force"}, description = "Force re-downloading even if the local install looks up-to-date")
	private boolean force;
	@Option(names = {"-v", "--version"}, description = "Target JavaFX version to use, instead of whatever is the latest")
	private int version;

	@Override
	public JavaFxVersion call() {
		checkClearCache(clear, keepLatest, maxCacheCount, maxCacheSize);
		return update(version, force);
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
		JavaFxVersion latestLocalVersion = JavaFxVersion.getLocalVersion();
		Path dependenciesDir = CommonPaths.getDependenciesDir();
		try {
			Files.walkFileTree(dependenciesDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (keepLatest) {
						// Only delete if it's an old version
						JavaFxVersion versionOfFile = JavaFxVersion.mapToVersion(file);
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
	 */
	public static JavaFxVersion update(int version, boolean force) {
		JavaFxVersion latest = version < 11 ? JavaFxVersion.getLatestVersion() : new JavaFxVersion(version);
		JavaFxVersion local = JavaFxVersion.getLocalVersion();
		if (latest == null)
			return local;
		if (local == null)
			force = true;
		if (force || latest.isNewer(local)) {
			updateTo(latest, force);
			logger.info("Updated to JavaFX '{}'", latest.getVersion());
			return latest;
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
	public static void updateTo(JavaFxVersion version, boolean force) {
		JavaFxPlatform platform = JavaFxPlatform.detect();
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
			String artifactUrlFormat = " https://repo1.maven.org/maven2/org/openjfx/%s/%s/" + artifactFormat;
			String localArtifact = String.format(artifactFormat, artifact, versionName, classifier);
			String artifactUrl = String.format(artifactUrlFormat, artifact, versionName, artifact, versionName, classifier);
			Path localPath = CommonPaths.getDependenciesDir().resolve(localArtifact);
			if (force || !Files.exists(localPath)) {
				try {
					// TODO: Validating these would be nice
					byte[] download = Web.getBytes(artifactUrl);
					Files.copy(new ByteArrayInputStream(download), localPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ex) {
					logger.error("Failed downloading FX artifact: '{}'", artifactUrl, ex);
				}
			}
		}
	}
}
