package dev.fxe;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import io.qt.widgets.QApplication;
import io.qt.widgets.QMainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.recaf.launcher.config.Config;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Loggers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class LauncherGuiQt extends QMainWindow {
	private static final Logger logger = Loggers.newLogger();
	private static final Config config = Config.get();

	public LauncherGuiQt() {
		setWindowTitle("Recaf Launcher Qt");
		QUtils.trySetLogo(this);

		switch (config.getLaunchAction()) {
			case SHOW_LAUNCHER:
				setCentralWidget(new MainPanelWidget());
				break;
			case RUN_RECAF:
				break;
			case UPDATE_RUN_RECAF:
				break;
		}
	}

	public static void main(String[] args) {
		initLogging();

		QApplication.initialize(args);
		String darkStyleSheet = "QWidget {\n"
			+ "    background-color: #2e2e2e;\n"
			+ "    color: #ffffff;\n"
			+ "    font-size: 14px;\n"
			+ "}\n"
			+ "QPushButton {\n"
			+ "    background-color: #444444;\n"
			+ "    border: 1px solid #555555;\n"
			+ "    border-radius: 5px;\n"
			+ "    padding: 5px;\n"
			+ "}\n"
			+ "QPushButton:hover {\n"
			+ "    background-color: #555555;\n"
			+ "}\n"
			+ "QComboBox {\n"
			+ "    background-color: #444444;\n"
			+ "    border: 1px solid #555555;\n"
			+ "    border-radius: 5px;\n"
			+ "    padding: 3px;\n"
			+ "}\n"
			+ "QComboBox QAbstractItemView {\n"
			+ "    background-color: #2e2e2e;\n"
			+ "    border: 1px solid #555555;\n"
			+ "}";

		Objects.requireNonNull(QApplication.instance()).setStyleSheet(darkStyleSheet);
		QApplication.font("Roboto");

		LauncherGuiQt launcherGuiQt = new LauncherGuiQt();
		launcherGuiQt.show();

		QApplication.exec();
		QApplication.shutdown();
	}


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
}