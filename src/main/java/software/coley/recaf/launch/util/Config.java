package software.coley.recaf.launch.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Basic persistent launcher config.
 */
public class Config {
	private static Config instance;

	private Instant lastUpdate = Instant.EPOCH;
	private Duration updateCheckRate = Duration.ofDays(1);

	private Config() {
		// Deny construction
	}

	/**
	 * @return Config singleton.
	 */
	public static Config getInstance() {
		if (instance == null) {
			instance = new Config();
			instance.load();
		}
		return instance;
	}

	/**
	 * @param log
	 *        {@code true} to log extra info.
	 *
	 * @return {@code true} when we've already checked updates recently.
	 */
	public static boolean hasCheckedForUpdatesRecently(boolean log) {
		Instant lastUpdate = instance.getLastUpdateCheck();
		Instant nextCheckTime = lastUpdate.plus(instance.getUpdateCheckRate());
		Instant now = Instant.now();
		if (now.isBefore(nextCheckTime)) {
			if (log)
				System.out.println("Checked recently (" + lastUpdate + "), skipping update until " + nextCheckTime);
			return true;
		}
		instance.setLastUpdateCheck(now);
		return false;
	}

	/**
	 * @return Time of last update check.
	 */
	public Instant getLastUpdateCheck() {
		return lastUpdate;
	}

	/**
	 * @param lastUpdate
	 * 		Time of last update check.
	 */
	public void setLastUpdateCheck(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
		save();
	}

	/**
	 * @return Duration of time to wait between update checks.
	 */
	public Duration getUpdateCheckRate() {
		return updateCheckRate;
	}

	private void load() {
		Path path = CommonPaths.getLauncherConfigFile();
		if (Files.isRegularFile(path)) {
			try {
				String configJson = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
				JsonObject configObj = Json.parse(configJson).asObject();
				String lastUpdateTimeStr = configObj.getString("last-update", null);
				String upateCheckRateStr = configObj.getString("update-check-rate", null);
				if (lastUpdateTimeStr != null)
					lastUpdate = Instant.parse(lastUpdateTimeStr);
				if (upateCheckRateStr != null)
					updateCheckRate = Duration.parse(upateCheckRateStr);
			} catch (Throwable t) {
				System.err.println("Could not read launcher config contents");
				t.printStackTrace();
				path.toFile().deleteOnExit();
			}
		}
	}

	private void save() {
		WriterConfig config = WriterConfig.PRETTY_PRINT;
		JsonObject object = Json.object();
		object.set("last-update", lastUpdate.toString());
		object.set("update-check-rate", updateCheckRate.toString());
		try {
			Path launcherDir = CommonPaths.getLauncherDir();
			if (!Files.isDirectory(launcherDir))
				Files.createDirectories(launcherDir);
			Files.write(CommonPaths.getLauncherConfigFile(), object.toString(config).getBytes(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			System.err.println("Could not write launcher config contents");
			ex.printStackTrace();
		}
	}
}
