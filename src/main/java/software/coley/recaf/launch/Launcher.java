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
import software.coley.recaf.launch.util.DefaultAction;

import javax.swing.*;
import java.util.Map;
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

		// Handle when no arguments are passed.
		if (args == null || args.length == 0) {
			// If there is no console instance, we've likely been launched via 'javaw'.
			// Since there is no console to work with it only makes sense to open the GUI.
			if (System.console() == null) {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Throwable ignored) {
					// Use the default ugly LaF then...
				}

				new LauncherWindow().setVisible(true);
				return;
			}

			// Run the default action handler.
			DefaultAction.handleDefaultAction();
			return;
		}

		// Run the console commands.
		new CommandLine(new Root()).execute(args);
	}

	public static String dumpInfo() {
		if (info == null) {
			// Print system info so that we don't have to ask users for it all the time.
			// If they screenshot or share the log it should be here.
			RecafVersion recafVersion = RecafVersion.getInstalledVersion();
			if (recafVersion == null) recafVersion = new RecafVersion("?", -1);
			StringBuilder sb = new StringBuilder("Java Properties:\n");
			Map<String, String> properties = new TreeMap<>(SystemInformation.ALL_PROPERTIES);
			properties.put("javafx.version", String.valueOf(JavaFxVersion.getRuntimeVersion()));
			properties.put("javafx.platform", JavaFxPlatform.detect().getClassifier());
			properties.put("recaf.version", recafVersion.getVersion());
			properties.forEach((key, value) -> {
				sb.append(String.format(" - %s = %s\n", key, value));
			});
			info = sb.toString();
		}
		return info;
	}
}
