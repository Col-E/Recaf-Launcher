package software.coley.recaf.launch.commands;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger logger = LoggerFactory.getLogger(UpdateRecaf.class);

	private static final String LATEST_RELEASE = "https://api.github.com/repos/Col-E/Recaf/releases/latest";

	@Override
	public UpdateResult call() {
		return update();
	}

	/**
	 * @return {@code true} when an update occurred.
	 */
	public static UpdateResult update() {
		RecafVersion installedVersion = RecafVersion.getInstalledVersion();

		// Only run if the last update check wasn't too recent
		if (Config.getInstance().hasCheckedForUpdatesRecently())
			return UpdateResult.UP_TO_DATE;

		// Get release JSON model from GitHub
		JsonObject latestRelease;
		try {
			String latestReleaseJson = Web.getText(LATEST_RELEASE);
			latestRelease = Json.parse(latestReleaseJson).asObject();
		} catch (IOException ex) {
			logger.error("Failed reading latest release from GitHub", ex);
			return UpdateResult.FAILED_TO_FETCH;
		}

		// Check if latest release tag (version) is newer than the current one.
		String latestTag = latestRelease.getString("tag_name", "0.0.0");
		RecafVersion latestVersion = new RecafVersion(latestTag, -1);
		if (!latestVersion.isNewer(installedVersion)) {
			// Not newer, we're up-to-date.
			assert installedVersion != null;
			logger.debug("Current version '{}' is up-to-date", installedVersion.getVersion());
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
					logger.info("Updated Recaf to '{}'", latestVersion);
					return UpdateResult.UP_TO_DATE;
				} catch (IOException ex) {
					logger.error("Failed writing to Recaf jar location: '{}'", recafJar, ex);
					return UpdateResult.FAILED_TO_WRITE;
				}
			}
		}

		return UpdateResult.FAILED_NO_CANDIDATES;
	}
}
