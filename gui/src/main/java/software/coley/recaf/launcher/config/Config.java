package software.coley.recaf.launcher.config;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.task.JavaEnvTasks;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Loggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GUI config.
 */
public class Config {
	private static final Config INSTANCE = new Config();
	private static final Logger logger = Loggers.newLogger();
	private LaunchAction launchAction = LaunchAction.SHOW_LAUNCHER;
	private JavaInstall javaInstall;
	private boolean isFirst = true;

	/**
	 * Initialize config from storage.
	 */
	private Config() {
		Path configFile = CommonPaths.getGuiConfigFile();
		if (Files.exists(configFile)) {
			isFirst = false;
			try {
				String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
				JsonValue parsed = Json.parse(content);
				if (parsed instanceof JsonObject) {
					JsonObject root = (JsonObject) parsed;
					String action = root.getString("action", null);
					String java = root.getString("java", null);
					if (action != null) {
						try {
							launchAction = LaunchAction.valueOf(action);
						} catch (IllegalArgumentException ignored) {
							// If the value doesn't match an existing key, whatever we have a default.
						}
					}
					if (java != null) {
						Path javaPath = Paths.get(java);
						if (JavaEnvTasks.addJavaInstall(javaPath)) {
							JavaInstall configInstall = JavaEnvTasks.getByPath(javaPath);
							if (configInstall != null)
								javaInstall = configInstall;
						}
					}
				}
			} catch (Throwable t) {
				logger.error("Failed to read launcher config", t);
			}
		}
	}

	/**
	 * Write config to storage.
	 */
	private void persist() {
		JsonObject root = Json.object();
		if (launchAction != null)
			root.set("action", launchAction.name());
		if (javaInstall != null)
			root.set("java", javaInstall.getJavaExecutable().toString());
		try {
			Path configFile = CommonPaths.getGuiConfigFile();
			Path parentDir = configFile.getParent();
			if (!Files.isDirectory(parentDir))
				Files.createDirectories(parentDir);
			Files.write(configFile, root.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			logger.error("Failed to persist launcher config", ex);
		}
	}

	/**
	 * @return Shared config instance.
	 */
	@Nonnull
	public static Config get() {
		return INSTANCE;
	}

	/**
	 * @return {@code true} when the user has not opened the launcher before <i>(according to our config)</i>.
	 */
	public boolean isFirstTime() {
		return isFirst;
	}

	/**
	 * Marks the first time as having been taken.
	 */
	public void invalidateFirstTime() {
		isFirst = false;
	}

	/**
	 * @return Configured action to take when running the launcher.
	 */
	@Nonnull
	public LaunchAction getLaunchAction() {
		return launchAction;
	}

	/**
	 * @param launchAction
	 * 		Action to take when running the launcher.
	 */
	public void setLaunchAction(@Nonnull LaunchAction launchAction) {
		this.launchAction = launchAction;
		persist();
	}

	/**
	 * @return Target Java version to use when running Recaf.
	 * {@code null} initally when {@link #isFirstTime()} is {@code true}.
	 */
	@Nullable
	public JavaInstall getJavaInstall() {
		return javaInstall;
	}

	/**
	 * @param javaInstall
	 * 		Target Java version to use when running Recaf.
	 */
	public void setLaunchJavaInstallation(@Nonnull JavaInstall javaInstall) {
		this.javaInstall = javaInstall;
		persist();
	}
}
