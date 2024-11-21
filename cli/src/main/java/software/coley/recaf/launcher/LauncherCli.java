package software.coley.recaf.launcher;

import org.slf4j.Logger;
import picocli.CommandLine;
import software.coley.recaf.launcher.commands.Root;
import software.coley.recaf.launcher.info.RecafVersion;
import software.coley.recaf.launcher.info.SystemInformation;
import software.coley.recaf.launcher.task.JavaFxTasks;
import software.coley.recaf.launcher.task.RecafTasks;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.Loggers;

import java.util.Map;
import java.util.TreeMap;

/**
 * Entry point class for CLI usage.
 */
public class LauncherCli {
	private static final Logger logger = Loggers.newLogger();

	public static void main(String[] args) {
		dumpInfo();
		new CommandLine(new Root()).execute(args);
	}

	public static void dumpInfo() {
		// Print system info so that we don't have to ask users for it all the time.
		// If they screenshot or share the log it should be here.
		RecafVersion recafVersion;
		try {
			recafVersion = RecafTasks.getInstalledVersion();
		} catch (InvalidInstallationException e) {
			recafVersion = new RecafVersion("?", -1);
		}
		StringBuilder sb = new StringBuilder("Java Properties:\n");
		Map<String, String> properties = new TreeMap<>(SystemInformation.ALL_PROPERTIES);
		properties.put("javafx.version.cached", String.valueOf(JavaFxTasks.detectCachedVersion()));
		properties.put("javafx.platform", JavaFxTasks.detectSystemPlatform().getClassifier());
		properties.put("recaf.version", recafVersion.getVersion());
		properties.forEach((key, value) -> {
			sb.append(String.format(" - %s = %s\n", key, value));
		});
		logger.info(sb.toString());
	}
}
