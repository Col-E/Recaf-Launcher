package software.coley.recaf.launcher;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.recaf.launcher.config.Config;
import software.coley.recaf.launcher.gui.ErrorPanel;
import software.coley.recaf.launcher.gui.FirstTimePanel;
import software.coley.recaf.launcher.gui.MainPanel;
import software.coley.recaf.launcher.gui.PopupLauncherFeedback;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.task.ExecutionTasks;
import software.coley.recaf.launcher.task.JavaFxTasks;
import software.coley.recaf.launcher.task.RecafTasks;
import software.coley.recaf.launcher.task.VersionUpdateResult;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Loggers;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
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
 * Entry point class for GUI usage.
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
		initLogging();
		switch (config.getLaunchAction()) {
			case SHOW_LAUNCHER:
				uiContext = true;
				showLauncher();
				break;
			case RUN_RECAF:
				launch(LauncherFeedback.NOOP, false);
				break;
			case UPDATE_RUN_RECAF:
				launch(new PopupLauncherFeedback(null), true);
				break;
		}
	}

	/**
	 * Direct launcher logs to be written to {@code %RECAF%/launcher/launcher-log.txt}.
	 * We'll only keep the one file which overwrites whenever running the launcher again.
	 */
	@SuppressWarnings("all")
	private static void initLogging() {
		// Setup appender
		Path logFile = CommonPaths.getLauncherDir().resolve("launcher-log.txt");
		if (Files.exists(logFile)) {
			try {
				Files.deleteIfExists(logFile);
			} catch (IOException ex) {
				logger.error("Failed to delete old log file: {}", logFile, ex);
			}
		}

		// We do it this way so the file path can be set at runtime.
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		FileAppender fileAppender = new FileAppender<>();
		fileAppender.setFile(logFile.toString());
		fileAppender.setContext(loggerContext);
		fileAppender.setPrudent(true);
		fileAppender.setAppend(true);
		fileAppender.setImmediateFlush(true);

		// Pattern
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%d{HH:mm:ss.SSS} [%logger{0}/%thread] %-5level: %msg%n");
		encoder.start();
		fileAppender.setEncoder(encoder);

		// Start file appender
		fileAppender.start();

		// Create logger
		ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger)
				LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		logbackLogger.addAppender(fileAppender);
		logbackLogger.setAdditive(false);

		// Set default error handler
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			logger.error("Uncaught exception on thread '{}'", t.getName(), e);
		});
	}

	/**
	 * Shows the main launcher GUI.
	 */
	private static void showLauncher() {
		setupLookAndFeel();

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
		frame.setMinimumSize(new Dimension(550, 180));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	/**
	 * Assign look-and-feel plus font sizes.
	 */
	public static void setupLookAndFeel() {
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
			logger.error("Failed to set launcher LAF", t);
		}
	}

	/**
	 * Handles launching Recaf.
	 *
	 * @param feedback
	 * 		Feedback mechanism for launch progress.
	 * @param update
	 *        {@code true} when the user indicated they have opted to update Recaf.
	 */
	public static void launch(@Nonnull LauncherFeedback feedback, boolean update) {
		// Handle updating, even in cases where the user did not opt to update but one is required.
		if (update || recafRequiresUpdate()) {
			feedback.updateLaunchProgressMessage("Updating Recaf...");
			updateRecaf(feedback);
		}

		// Get Java version from config.
		JavaInstall javaInstall = config.getJavaInstall();
		int javaVersion = javaInstall == null ? JavaVersion.get() : javaInstall.getVersion();

		// Only update JavaFX when needed.
		if (javafxRequiresUpdate()) {
			feedback.updateLaunchProgressMessage("Updating JavaFX...");
			updateJavafx(feedback, javaVersion);
		}

		// Notify we're at the launch step.
		feedback.updateLaunchProgressMessage("Launching Recaf...");

		// Create a future to call the feedback finish method.
		// We may need to do this async since a successful run actually blocks this thread.
		CompletableFuture<Boolean> launchFuture = new CompletableFuture<>();
		launchFuture.whenComplete((success, error) -> {
			if (error != null) success = false;
			feedback.finishLaunchProgress(success);
		});

		try {
			String javaExecutablePath = javaInstall == null ? null : javaInstall.getJavaExecutable().toString();

			// The "run" task blocks the thread. So if we don't see any result in the next ~2 seconds we assume
			// that it was a success. If the launch fails it will usually be pretty much an instant failure.
			new Thread(() -> {
				try {Thread.sleep(2000);} catch (InterruptedException ignored) {}
				launchFuture.complete(true);
			}).start();
			ExecutionTasks.RunResult result = ExecutionTasks.run(true, false, javaExecutablePath);

			// At this point Recaf has closed. We want to complete the launch future if it hasn't been completed already.
			// If Recaf closed normally we want to kill the launcher process. Otherwise, we want to stick around to
			// report any problems.
			if (!result.isSuccess()) {
				logger.error("Failed launching Recaf: " + result.getCodeDescription());
				launchFuture.complete(false);
			} else {
				launchFuture.complete(true);
			}
			int code = result.getCode();
			switch (code) {
				case ExecutionTasks.SUCCESS:
					System.exit(0);
					break;
				case ExecutionTasks.ERR_NOT_INSTALLED:
					if (uiContext)
						JOptionPane.showMessageDialog(null, "Recaf is not installed", "Failed launching Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
					break;
				case ExecutionTasks.ERR_NO_JFX:
					if (uiContext)
						JOptionPane.showMessageDialog(null, "JavaFX is not installed", "Failed launching Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
					break;
				default:
					if (uiContext) {
						JScrollPane scroll = new JScrollPane(new ErrorPanel(result));
						scroll.getVerticalScrollBar().setUnitIncrement(25);
						scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						scroll.addHierarchyListener(e -> {
							Window window = SwingUtilities.getWindowAncestor(scroll);
							if (window instanceof Dialog) {
								Dialog dialog = (Dialog) window;
								if (!dialog.isResizable()) {
									dialog.setResizable(true);
								}
							}
						});
						JOptionPane.showMessageDialog(null, scroll, "Failed launching Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
					}
					break;
			}
		} catch (IOException ex) {
			logger.error("Encountered error running Recaf", ex);
			if (uiContext) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				JOptionPane.showMessageDialog(null, sw.toString(), "Error encountered launching Recaf", JOptionPane.ERROR_MESSAGE, recafIcon);
			}
			launchFuture.completeExceptionally(ex);
		}
	}

	/**
	 * Downloads the latest Recaf jar.
	 *
	 * @param feedback
	 * 		Feedback mechanism for update progress.
	 */
	public static void updateRecaf(@Nonnull LauncherFeedback feedback) {
		RecafTasks.setDownloadListener(feedback.provideRecafDownloadListener());

		// Update from snapshots
		VersionUpdateResult result = RecafTasks.updateFromSnapshot("master");

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
		} else if (Objects.equals(result.getFrom(), result.getTo())) {
			logger.info("No updates for Recaf available, we are up-to-date.");
		}
	}

	/**
	 * Cleans the dependencies directory and downloads the latest JavaFX.
	 *
	 * @param feedback
	 * 		Feedback mechanism for update progress.
	 * @param javaVersion
	 * 		Version of Java to use for compatibility filtering.
	 */
	public static void updateJavafx(@Nonnull LauncherFeedback feedback, int javaVersion) {
		JavaFxTasks.setDownloadListener(feedback.provideJavaFxDownloadListener());

		// Clean the slate by clearing the cache
		JavaFxTasks.checkClearCache(true, false, 0, 0);

		// Ensure the update passed (null means it failed)
		if (JavaFxTasks.update(-1, javaVersion, true) == null) {
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
