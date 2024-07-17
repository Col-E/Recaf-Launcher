package software.coley.recaf.launcher;

import org.slf4j.Logger;
import software.coley.recaf.launcher.config.Config;
import software.coley.recaf.launcher.gui.FirstTimePanel;
import software.coley.recaf.launcher.gui.MainPanel;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.task.ExecutionTasks;
import software.coley.recaf.launcher.task.JavaFxTasks;
import software.coley.recaf.launcher.task.RecafTasks;
import software.coley.recaf.launcher.task.VersionUpdateResult;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Loggers;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  Entry point class for GUI usage.
 */
public class LauncherGui {
	private static final Logger logger = Loggers.newLogger();
	private static final Config config = Config.get();
	/** Executor to use for async-tasks that block the JVM from exiting after the main thread has ended */
	public static final Executor nonExitingAsyncExecutor = command -> {
		Thread thread = new Thread(command);
		thread.setName("Launch-Recaf");
		thread.setDaemon(false);
		thread.start();
	};
	/** Recaf icon */
	public static BufferedImage recafImage;
	/** Recaf icon */
	public static Icon recafIcon;
	/** Flag for when the user is launching this for the first time */
	private static boolean firstTime;
	/** Flag for when the intent is to show the UI */
	private static boolean uiContext;

	public static void main(String[] args) {
		switch (config.getLaunchAction()) {
			case SHOW_LAUNCHER:
				uiContext = true;
				showLauncher();
				break;
			case RUN_RECAF:
				launch(LauncherFeedback.NOOP, false);
				break;
			case UPDATE_RUN_RECAF:
				launch(LauncherFeedback.NOOP, true);
				break;
		}
	}

	/**
	 * Shows the main launcher GUI.
	 */
	private static void showLauncher() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			// Scale up the global font size.
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			List<String> fonts = Arrays.asList(env.getAvailableFontFamilyNames());
			int fontSize = 13;
			if (fonts.contains("Segoe UI"))
				setUIFont(new FontUIResource("Segoe UI", Font.PLAIN, fontSize));
			else if (fonts.contains("Helvetica"))
				setUIFont(new FontUIResource("Helvetica", Font.PLAIN, fontSize));
			else if (fonts.contains("Trebuchet MS"))
				setUIFont(new FontUIResource("Trebuchet MS", Font.PLAIN, fontSize));
			else if (fonts.contains("Tahoma"))
				setUIFont(new FontUIResource("Tahoma", Font.PLAIN, fontSize));
			else
				setUIFont(new FontUIResource("Arial", Font.PLAIN, fontSize));
		} catch (Throwable t) {
			logger.error("Failed to set launcher LAF");
		}

		JFrame frame = new JFrame();
		frame.setTitle("Recaf Launcher");
		try {
			recafImage = ImageIO.read(Objects.requireNonNull(LauncherGui.class.getResource("/images/logo.png")));
			frame.setIconImage(recafImage);
			recafIcon = new ImageIcon(recafImage);
		} catch (IOException ex) {
			logger.error("Failed to set launcher icon");
		}
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		firstTime = config.isFirstTime();
		if (firstTime) {
			Container contentPane = frame.getContentPane();
			contentPane.add(new FirstTimePanel(() -> {
				switch (config.getLaunchAction()) {
					case SHOW_LAUNCHER:
						contentPane.removeAll();
						contentPane.add(new MainPanel(frame));
						contentPane.revalidate();
						contentPane.repaint();
						break;
					case RUN_RECAF:
						frame.setVisible(false);
						frame.dispose();
						CompletableFuture.runAsync(() -> launch(LauncherFeedback.NOOP, false), nonExitingAsyncExecutor);
						break;
					case UPDATE_RUN_RECAF:
						frame.setVisible(false);
						frame.dispose();
						CompletableFuture.runAsync(() -> launch(LauncherFeedback.NOOP, true), nonExitingAsyncExecutor);
						break;
				}
			}));
		} else {
			frame.getContentPane().add(new MainPanel(frame));
		}
		frame.setMinimumSize(new Dimension(550, 300));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	/**
	 * Handles launching Recaf.
	 *
	 * @param update
	 *        {@code true} when the user indicated they have opted to update Recaf.
	 */
	public static void launch(@Nonnull LauncherFeedback feedback, boolean update) {
		// Handle updating, even in cases where the user did not opt to update but one is required.
		if (update || recafRequiresUpdate()) {
			feedback.updateLaunchProgressMessage("Updating Recaf...");
			updateRecaf();
		}
		if (javafxRequiresUpdate()) // Only update JavaFX when needed.
		{
			feedback.updateLaunchProgressMessage("Updating JavaFX...");
			updateJavafx();
		}
		feedback.finishLaunchProgress();

		try {
			JavaInstall javaInstall = config.getJavaInstall();
			String javaExecutablePath = javaInstall == null ? null : javaInstall.getJavaExecutable().toString();
			ExecutionTasks.RunResult result = ExecutionTasks.run(true, javaExecutablePath);
			switch (result) {
				case ERR_NOT_INSTALLED:
					logger.error("Failed launching Recaf: Recaf is not installed");
					if (uiContext)
						JOptionPane.showMessageDialog(null, "Recaf is not installed", "Failed launching Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
					break;
				case ERR_NO_JFX:
					logger.error("Failed launching Recaf: JavaFX is not installed");
					if (uiContext)
						JOptionPane.showMessageDialog(null, "JavaFX is not installed", "Failed launching Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
					break;
				case SUCCESS:
					// no-op
					break;
			}
		} catch (IOException ex) {
			logger.error("Encountered error running Recaf", ex);
			if (uiContext) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				JOptionPane.showMessageDialog(null, sw.toString(), "Error encountered launching Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
			}
		}
	}

	/**
	 * Downloads the latest Recaf jar.
	 */
	public static void updateRecaf() {
		VersionUpdateResult result = RecafTasks.updateFromSnapshot("dev4");

		// Ensure the update passed
		Throwable error = result.getError();
		if (error != null) {
			logger.error("Encountered error updating Recaf from latest snapshot", error);
			if (uiContext) {
				Path recafPath = CommonPaths.getRecafJar();
				StringWriter sw = new StringWriter();
				error.printStackTrace(new PrintWriter(sw));
				String message = "Recaf could not be downloaded to:\n" + recafPath + "\n\nError details:\n" + sw;
				JOptionPane.showMessageDialog(null, message, "Failed updating Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
			}
		}
	}

	/**
	 * Cleans the dependencies directory and downloads the latest JavaFX.
	 */
	public static void updateJavafx() {
		// Clean the slate by clearing the cache
		JavaFxTasks.checkClearCache(true, false, 0, 0);

		// Ensure the update passed (null means it failed)
		if (JavaFxTasks.update(-1, true) == null) {
			logger.error("Failed fetching JavaFX after update was initiated");
			if (uiContext) {
				Path dependenciesDir = CommonPaths.getDependenciesDir();
				JOptionPane.showMessageDialog(null, "JavaFX could not be downloaded to:\n" + dependenciesDir,
						"Failed updating JavaFX", JOptionPane.ERROR_MESSAGE, recafIcon);
			}
		}
	}

	/**
	 * @return {@code true} when an update to Recaf is required, even if not requested by the user in their config choice.
	 */
	private static boolean recafRequiresUpdate() {
		// If they've never run the launcher before we need to download a clean slate.
		if (firstTime) return true;

		// Ensure Recaf is installed locally and is not invalid in any way.
		try {
			RecafTasks.getInstalledVersion();
		} catch (InvalidInstallationException ex) {
			// Installation is not valid (missing jar, invalid jar, etc) so we need to download Recaf.
			return true;
		}

		return false;
	}

	/**
	 * @return {@code true} when an update to JavaFX is required, even if not requested by the user in their config choice.
	 */
	private static boolean javafxRequiresUpdate() {
		// JavaFX dependencies must exist.
		Path dependenciesDir = CommonPaths.getDependenciesDir();
		if (!Files.isDirectory(dependenciesDir))
			return true;

		try (Stream<Path> dependencyPathStream = Files.list(dependenciesDir)) {
			List<Path> dependencyPaths = dependencyPathStream.collect(Collectors.toList());

			// Ensure the dependencies exist.
			if (dependencyPaths.isEmpty())
				return true;

			// Ensure they are not obviously malformed.
			for (Path dependencyPath : dependencyPaths) {
				if (isInvalidJar(dependencyPath))
					return true;
			}
		} catch (IOException ex) {
			throw new RuntimeException("Files.list failed even though dependencies dir exists!", ex);
		}

		return false;
	}

	/**
	 * @param path
	 * 		Path to check for jar integrity.
	 *
	 * @return {@code true} when the jar roughly passes basic integrity checking.
	 */
	private static boolean isInvalidJar(@Nonnull Path path) {
		if (!path.endsWith(".jar"))
			return false;

		// Each dependency jar should be fully readable.
		try (JarFile jar = new JarFile(path.toFile())) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.isDirectory())
					continue;
				if (jar.getInputStream(entry) == null)
					return true;
			}
			return false;
		} catch (IOException ex) {
			return true;
		}
	}

	/**
	 * @param resource
	 * 		Font to set.
	 *
	 * @author Romain Hippeau - https://stackoverflow.com/a/7434935/
	 */
	public static void setUIFont(@Nonnull FontUIResource resource) {
		Enumeration<?> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof FontUIResource)
				UIManager.put(key, resource);
		}
	}
}
