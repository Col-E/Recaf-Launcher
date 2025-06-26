package software.coley.recaf.launcher.task;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.launcher.info.RecafVersion;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Stream;
import software.coley.recaf.launcher.util.TransferListener;
import software.coley.recaf.launcher.util.Web;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Tasks for Recaf versioning.
 */
public class RecafTasks {
	private static final String LATEST_RELEASE = "https://api.github.com/repos/Col-E/Recaf/releases/latest";
	private static final int RECAF_REPO_ID = 98499283; // See: https://api.github.com/repos/Col-E/Recaf
	private static final RecafVersion SNAPSHOT_VERSION = new RecafVersion("4.X.X-SNAPSHOT", 0);
	public static final int FALLBACK_RECAF_SIZE_BYTES = 80_000_000; // Rough over-estimated size of recaf jar in bytes (80 MB)
	private static TransferListener downloadListener;

	/**
	 * @param downloadListener
	 * 		Listener to be notified of Recaf download operations.
	 */
	public static void setDownloadListener(@Nullable TransferListener downloadListener) {
		RecafTasks.downloadListener = downloadListener;
	}

	/**
	 * Get the current installed version of Recaf.
	 *
	 * @return Version string of Recaf.
	 *
	 * @throws InvalidInstallationException
	 * 		When there is no valid-installed version of Recaf.
	 */
	@Nonnull
	public static RecafVersion getInstalledVersion() throws InvalidInstallationException {
		// Check if it exists.
		Path recafJar = CommonPaths.getRecafJar();
		if (!Files.exists(recafJar)) {
			String message = "Recaf jar file not found: '" + recafJar + "'";
			throw new InvalidInstallationException(InvalidInstallationException.FILE_DOES_NOT_EXIST, message);
		}

		// Extract the build config class data.
		byte[] buildConfigBytes;
		try (ZipFile zip = new ZipFile(recafJar.toFile())) {
			ZipEntry entry = zip.getEntry("software/coley/recaf/RecafBuildConfig.class");
			if (entry == null) {
				String message = "Recaf build config is not present in the jar: '" + recafJar + "'\n"
						+ "The launcher is only compatible with Recaf 4+";
				throw new InvalidInstallationException(InvalidInstallationException.MISSING_BUILD_INFO, message);
			}
			InputStream input = zip.getInputStream(entry);

			// Read contents of the class
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			Stream.transfer(2048, input, output);
			buildConfigBytes = output.toByteArray();
		} catch (IOException ex) {
			String message = "Invalid build config ZIP entry found in installed jar: '" + recafJar + "'";
			throw new InvalidInstallationException(InvalidInstallationException.INVALID_BUILD_INFO_ENTRY, message, ex);
		}

		try {
			// Hack to ensure no matter what the class file version is, ASM will read it.
			// This 'down-samples' it to Java 8.
			buildConfigBytes[7] = 52;

			// Extract field values.
			Map<String, String> fields = new HashMap<>();
			ClassReader reader = new ClassReader(buildConfigBytes);
			reader.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
					if (value != null) {
						String valueStr = value.toString();
						fields.put(name, valueStr);
					}
					return null;
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

			// Convert field values into version wrapper.
			String version = fields.getOrDefault("VERSION", "0.0.0");
			String gitRevision = fields.getOrDefault("GIT_REVISION", "-1");
			int revision = gitRevision.matches("\\d+") ? Integer.parseInt(gitRevision) : -1;
			if (revision == 1)
				revision = -1;
			return new RecafVersion(version, revision);
		} catch (Throwable t) {
			String message = "Invalid build config model found in installed jar: '" + recafJar + "'";
			throw new InvalidInstallationException(InvalidInstallationException.INVALID_BUILD_INFO_MODEL, message, t);
		}
	}

	/**
	 * @return Result of attempting to update from the stable releases.
	 */
	@Nonnull
	public static VersionUpdateResult updateFromStable() {
		RecafVersion installedVersion;
		try {
			installedVersion = getInstalledVersion();
		} catch (InvalidInstallationException e) {
			// Expected behavior if there is no installed version or the current version is too old (2.X or 3.X)
			installedVersion = null;
		}

		// Get release JSON model from GitHub
		JsonObject latestRelease;
		try {
			String latestReleaseJson = Web.getText(LATEST_RELEASE);
			latestRelease = Json.parse(latestReleaseJson).asObject();
		} catch (IOException ex) {
			return new VersionUpdateResult(installedVersion, null, VersionUpdateStatusType.FAILED_TO_FETCH)
					.withError(ex);
		}

		// Check if latest release tag (version) is newer than the current one.
		String latestTag = latestRelease.getString("tag_name", "0.0.0");
		RecafVersion latestVersion = new RecafVersion(latestTag, -1);
		if (installedVersion != null && !latestVersion.isNewer(installedVersion)) {
			return new VersionUpdateResult(installedVersion, latestVersion, VersionUpdateStatusType.UP_TO_DATE);
		}

		JsonArray assets = latestRelease.get("assets").asArray();
		for (JsonValue assetValue : assets) {
			JsonObject asset = assetValue.asObject();
			String name = asset.getString("name", "").toLowerCase();

			// Get the first asset that indicates a fat-jar
			if (name.endsWith("-all.jar") || name.endsWith("-jar-with-dependencies.jar")) {
				Path recafJar = CommonPaths.getRecafJar();
				Path recafJarTemp = CommonPaths.getRecafTempJar();
				String downloadUrl = asset.getString("browser_download_url", null);
				try {
					if (downloadListener != null) downloadListener.init(downloadUrl);
					byte[] download = Web.getBytes(downloadUrl, downloadListener);
					Files.copy(new ByteArrayInputStream(download), recafJarTemp, StandardCopyOption.REPLACE_EXISTING);
					try {
						Files.move(recafJarTemp, recafJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
					} catch (Exception ignored) {
						Files.move(recafJarTemp, recafJar, StandardCopyOption.REPLACE_EXISTING);
					}
					return new VersionUpdateResult(installedVersion, latestVersion, VersionUpdateStatusType.UPDATE_TO_NEW);
				} catch (IOException ex) {
					return new VersionUpdateResult(installedVersion, latestVersion, VersionUpdateStatusType.FAILED_TO_WRITE)
							.withError(ex);
				}
			}
		}

		return new VersionUpdateResult(installedVersion, latestVersion, VersionUpdateStatusType.FAILED_NO_CANDIDATES);
	}

	/**
	 * @param branch
	 * 		Name of branch to match (equality), or {@code null} for any branch.
	 *
	 * @return Result of attempting to update from the snapshot releases.
	 */
	@Nonnull
	public static VersionUpdateResult updateFromSnapshot(@Nullable String branch) {
		return updateFromSnapshot(name -> branch == null || branch.equalsIgnoreCase(name));
	}

	// TODO: Snapshot update info
	//  - https://api.github.com/repos/Col-E/Recaf/commits
	//    - array[0].commit.author.date --> latest update
	//  - display "there are x commits ahead" for snapshots + the date of the latest vs current version

	/**
	 * @param branchMatcher
	 * 		Filter to whitelist only certain branches, or {@code null} for any branch.
	 *
	 * @return Result of attempting to update from the snapshot releases.
	 */
	@Nonnull
	public static VersionUpdateResult updateFromSnapshot(@Nullable Predicate<String> branchMatcher) {
		RecafVersion installedVersion;
		try {
			installedVersion = getInstalledVersion();
		} catch (InvalidInstallationException e) {
			// Expected behavior if there is no installed version or the current version is too old (2.X or 3.X)
			installedVersion = null;
		}

		try {
			// Get artifacts.
			// They appear in sorted order by time.
			String artifactsJson = Web.getText("https://api.github.com/repos/Col-E/Recaf/actions/artifacts");
			JsonObject artifacts = Json.parse(artifactsJson).asObject();
			JsonArray listing = artifacts.get("artifacts").asArray();
			for (JsonValue artifactValue : listing) {
				JsonObject artifact = artifactValue.asObject();

				// Skip expired artifacts
				boolean expired = artifact.getBoolean("expired", false);
				if (expired)
					continue;

				// Skip non snapshot builds
				String name = artifact.getString("name", "?");
				if (!name.equals("snapshot-build"))
					continue;

				// Skip branches if predicate is given
				JsonObject workflowRun = artifact.get("workflow_run").asObject();
				if (branchMatcher == null)
					continue;
				String branch = workflowRun.getString("head_branch", "?");
				if (!branchMatcher.test(branch))
					continue;

				// Skip if the repository isn't Col-E/Recaf
				int repositoryId = workflowRun.getInt("repository_id", 0);
				if (repositoryId != RECAF_REPO_ID)
					continue;
				int headRepositoryId = workflowRun.getInt("head_repository_id", -1);
				if (headRepositoryId != RECAF_REPO_ID)
					continue;

				// Size sanity check
				long size = artifact.getLong("size_in_bytes", -1);
				if (size <= 0)
					continue;

				// You can't just use the 'archive_download_url' value because we don't have permissions without
				// including an access token... which, I'm not going to do.
				// You also cannot reconstruct the URL: https://github.com/Col-E/Recaf/suites/<check_suite_id>/artifacts/<artifact_id>
				// as that is also locked behind requiring an account or access token.
				long workflowRunId = workflowRun.getLong("id", -1);

				// Compare to what we have locally installed. We can skip updating if the ids match.
				Path snapshotWorkflowFile = CommonPaths.getSnapshotWorkflowFile();
				if (Files.exists(snapshotWorkflowFile) && Files.exists(CommonPaths.getRecafJar())) {
					try {
						String existingWorkflowRunId = new String(Files.readAllBytes(snapshotWorkflowFile), StandardCharsets.UTF_8);
						if (existingWorkflowRunId.endsWith(String.valueOf(workflowRunId))) {
							return new VersionUpdateResult(installedVersion, installedVersion, VersionUpdateStatusType.UP_TO_DATE);
						}
					} catch (IOException ignored) {
						// We handle checking if the file exists, so this should never occur.
					}
				}

				// Write the workflow id so that we can compare against it later.
				try {
					if (Files.isDirectory(snapshotWorkflowFile.getParent()))
						Files.createDirectories(snapshotWorkflowFile.getParent());
					Files.write(snapshotWorkflowFile, String.valueOf(workflowRunId).getBytes(StandardCharsets.UTF_8));
				} catch (IOException ignored) {
					// We handle checking if the file/parent-dirs exists, so this should never occur.
				}

				// Instead we use a graciously hosted public service that will generate a link for you.
				// https://nightly.link/Col-E/Recaf/actions/runs/<run-id>/snapshot-build.zip
				//  - Feeling generous? You can sponsor the nightly link mirror service: https://github.com/sponsors/oprypin
				String downloadUrl = "https://nightly.link/Col-E/Recaf/actions/runs/" + workflowRunId + "/snapshot-build.zip";
				if (downloadListener != null) downloadListener.init(downloadUrl);
				byte[] download = Web.getBytes(downloadUrl, downloadListener);
				try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(download))) {
					// Extract the jar from the zip
					while (true) {
						ZipEntry entry = zip.getNextEntry();
						if (entry == null) break;
						if (entry.getName().toLowerCase().contains(".jar")) {
							Files.copy(zip, CommonPaths.getRecafTempJar(), StandardCopyOption.REPLACE_EXISTING);
							try {
								Files.move(CommonPaths.getRecafTempJar(), CommonPaths.getRecafJar(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
							} catch (Exception ignored) {
								Files.move(CommonPaths.getRecafTempJar(), CommonPaths.getRecafJar(), StandardCopyOption.REPLACE_EXISTING);
							}
						}
					}
				} catch (IOException ex) {
					return new VersionUpdateResult(installedVersion, SNAPSHOT_VERSION, VersionUpdateStatusType.FAILED_TO_WRITE)
							.withError(ex);
				}
				return new VersionUpdateResult(installedVersion, installedVersion, VersionUpdateStatusType.UPDATE_TO_NEW);
			}

			return new VersionUpdateResult(installedVersion, SNAPSHOT_VERSION, VersionUpdateStatusType.FAILED_NO_CANDIDATES);
		} catch (Throwable t) {
			return new VersionUpdateResult(installedVersion, SNAPSHOT_VERSION, VersionUpdateStatusType.FAILED_TO_FETCH)
					.withError(t);
		}
	}
}
