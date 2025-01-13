package dev.fxe;

import io.qt.gui.QIcon;
import io.qt.widgets.QErrorMessage;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QWidget;
import org.slf4j.Logger;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.util.Loggers;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

public class QUtils {
	private static final Logger logger = Loggers.newLogger();

	public static void trySetLogo(QWidget widget) {
		try {
			URL resource = Objects.requireNonNull(LauncherGuiQt.class.getResource("/images/logo.png"));
			widget.setWindowIcon(new QIcon(resource.toURI().getPath()));
		} catch (Exception e) {
			logger.error("Failed to set icon", e);
		}
	}

	public static void trySetIcon(QPushButton widget, String icon) {
		try {
			URL resource = Objects.requireNonNull(LauncherGuiQt.class.getResource("/images/" + icon + ".png"));
			widget.setIcon(new QIcon(resource.toURI().getPath()));
		} catch (Exception e) {
			logger.error("Failed to set icon {}", icon, e);
		}
	}

	public static void showErrorMessage(String title, String message) {
		QErrorMessage errorMessage = new QErrorMessage();
		errorMessage.setWindowTitle(title);
		errorMessage.showMessage(message);
		trySetLogo(errorMessage);
		errorMessage.exec();
	}

	public static String formatJavaInstall(Object value) {
		if (value instanceof JavaInstall) {
			JavaInstall installValue = (JavaInstall) value;
			Path homePath = installValue.getJavaExecutable().getParent().getParent();
			Path vendorPath = homePath.getParent();
			return installValue.getVersion() + ": " + vendorPath.getFileName() + " / " + homePath.getFileName();
		}
		return JavaVersion.get() + ": " + System.getProperty("java.vendor") + " / Current JVM";
	}
}
