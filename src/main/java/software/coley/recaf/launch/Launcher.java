package software.coley.recaf.launch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import software.coley.recaf.launch.commands.Root;
import software.coley.recaf.launch.gui.LauncherWindow;
import software.coley.recaf.launch.info.JavaFxPlatform;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.info.RecafVersion;
import software.coley.recaf.launch.info.SystemInformation;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Entry point class.
 */
public class Launcher {
	private static final Logger logger = LoggerFactory.getLogger(Launcher.class);
	public static String info;

	public static void main(String[] args) {
		// Always dump info first so the log file generates with system information that can be used to diagnose problems.
		logger.info(dumpInfo());

		// Check if user tried to run by double-clicking the jar instead of running from a console.
		// If so, open the GUI.
		if (System.console() == null && (args == null || args.length == 0)) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Throwable ignored) {
				// Use the default ugly LaF then...
			}

			new LauncherWindow().setVisible(true);
			return;
		}

		// Run the console commands.
		CommandLine cmd = new CommandLine(new Root());
		cmd.execute(args);
	}

	public static String dumpInfo() {
		if (info == null) {
			// Print system info so that we don't have to ask users for it all the time.
			// If they screenshot or share the log it should be here.
			StringBuilder sb = new StringBuilder("Java Properties:\n");
			Map<String, String> properties = new TreeMap<>(SystemInformation.ALL_PROPERTIES);
			properties.put("javafx.version", String.valueOf(JavaFxVersion.getRuntimeVersion()));
			properties.put("javafx.platform", JavaFxPlatform.detect().getClassifier());
			properties.put("recaf.version", Objects.requireNonNullElse(RecafVersion.getInstalledVersion(), new RecafVersion("?", -1)).getVersion());
			properties.forEach((key, value) -> {
				sb.append(String.format(" - %s = %s\n", key, value));
			});
			info = sb.toString();
		}
		return info;
	}
}
