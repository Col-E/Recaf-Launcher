package software.coley.recaf.launch.commands;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import picocli.CommandLine.Command;
import software.coley.recaf.launch.info.RecafVersion;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.Config;
import software.coley.recaf.launch.util.UpdateResult;
import software.coley.recaf.launch.util.Web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

/**
 * Command for updating Recaf.
 */
@Command(name = "update", description = "Updates Recaf")
public class UpdateRecaf implements Callable<UpdateResult> {
	private static final String LATEST_RELEASE = "https://api.github.com/repos/Col-E/Recaf/releases/latest";

	@Override
	public UpdateResult call() {
		return update(true);
	}

	/**
	 * @param log
	 *        {@code true} to log failure cases.
	 *
	 * @return {@code true} when an update occurred.
	 */
	public static UpdateResult update(boolean log) {
		RecafVersion installedVersion = RecafVersion.getInstalledVersion(true);

		// Only run if the last update check wasn't too recent
		if (Config.hasCheckedForUpdatesRecently(log))
			return UpdateResult.UP_TO_DATE;

		// Get release JSON model from GitHub
		JsonObject latestRelease;
		try {
			String latestReleaseJson = Web.getText(LATEST_RELEASE);
			latestRelease = Json.parse(latestReleaseJson).asObject();
		} catch (IOException ex) {
			if (log) {
				System.err.println("Failed reading latest release from GitHub");
				ex.printStackTrace();
			}
			return UpdateResult.FAILED_TO_FETCH;
		}

		// Check if latest release tag (version) is newer than the current one.
		String latestTag = latestRelease.getString("tag_name", "0.0.0");
		RecafVersion latestVersion = new RecafVersion(latestTag, -1);
		if (!latestVersion.isNewer(installedVersion)) {
			// Not newer, we're up-to-date.
			assert installedVersion != null;
			if (log) System.out.println("Current version '" + installedVersion.getVersion() + "' is up-to-date");
			return UpdateResult.UP_TO_DATE;
		}

		JsonArray assets = latestRelease.get("assets").asArray();
		for (JsonValue assetValue : assets) {
			JsonObject asset = assetValue.asObject();
			String name = asset.getString("name", "").toLowerCase();

			// Get the first asset that indicates a fat-jar
			if (name.endsWith("-all.jar") || name.endsWith("-jar-with-dependencies.jar")) {
				Path recafJar = CommonPaths.getRecafJar();
				String downloadUrl = asset.getString("browser_download_url", null);
				try {
					byte[] download = Web.getBytes(downloadUrl);
					Files.copy(new ByteArrayInputStream(download), recafJar, StandardCopyOption.REPLACE_EXISTING);
					if (log)
						System.out.println("Updated Recaf to " + latestVersion);
					return UpdateResult.UP_TO_DATE;
				} catch (IOException ex) {
					if (log) {
						System.err.println("Failed writing to Recaf jar location: " + recafJar);
						ex.printStackTrace();
					}
					return UpdateResult.FAILED_TO_WRITE;
				}
			}
		}

		return UpdateResult.FAILED_NO_CANDIDATES;
	}

}
