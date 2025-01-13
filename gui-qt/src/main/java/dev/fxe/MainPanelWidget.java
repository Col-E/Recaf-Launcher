package dev.fxe;

import io.qt.core.QRunnable;
import io.qt.core.QSize;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QFileDialog;
import io.qt.widgets.QGridLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QLayout;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QWidget;
import org.slf4j.Logger;
import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.info.PlatformType;
import software.coley.recaf.launcher.info.RecafVersion;
import software.coley.recaf.launcher.task.JavaEnvTasks;
import software.coley.recaf.launcher.task.JavaFxTasks;
import software.coley.recaf.launcher.task.RecafTasks;
import software.coley.recaf.launcher.task.VersionUpdateResult;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.Loggers;
import software.coley.recaf.launcher.util.SymLinks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class MainPanelWidget extends QWidget {

	private static final Logger logger = Loggers.newLogger();
	private final QWrappedComponent recafProgressBar;
	private final QWrappedComponent jfxProgressBar;
	private final QComboBox javaVersionCombo;

	public MainPanelWidget() {
		QGridLayout layout = new QGridLayout();
		QSize qSize = layout.sizeHint();
		qSize.setHeight(100);
		qSize.setWidth(600);
		layout.setSizeConstraint(QLayout.SizeConstraint.SetFixedSize);

		QLabel recafVersion = new QLabel("Recaf Version:");
		recafProgressBar = new QWrappedComponent(RecafTasks.FALLBACK_RECAF_SIZE_BYTES);
		QPushButton updateRecaf = makeUpdateButton("Update", "down", this::updateRecafTask);


		QLabel jfxVersion = new QLabel("JFX Version:");
		jfxProgressBar = new QWrappedComponent(JavaFxTasks.FALLBACK_FX_SIZE_BYTES);
		QPushButton updateJfx = makeUpdateButton("Update", "down", this::updateJfxTask);


		QLabel javaVersion = new QLabel("Java Version:");
		javaVersionCombo = new QComboBox();
		goFindJavaInstallsAndPopulateCombo();
		QPushButton browseButton = makeUpdateButton("Browse", "select", this::browseJavaVersions);


		layout.addWidget(recafVersion, 1, 1);
		layout.addWidget(recafProgressBar, 1, 2);
		layout.setColumnMinimumWidth(2, 300);
		layout.addWidget(updateRecaf, 1, 3);

		layout.addWidget(jfxVersion, 2, 1);
		layout.addWidget(jfxProgressBar, 2, 2);
		layout.addWidget(updateJfx, 2, 3);

		layout.addWidget(javaVersion, 3, 1);
		layout.addWidget(javaVersionCombo, 3, 2);
		layout.addWidget(browseButton, 3, 3);

		setLayout(layout);

		this.updateRecafVersionLabel();
		this.updateJavafxLabel();
	}

	private QPushButton makeUpdateButton(String title, String icon, QRunnable runnable) {
		QPushButton qPushButton = new QPushButton();
		qPushButton.setText(title);
		// Don't use a fucking method reference it breaks
		// TODO don't let user click the button again
		qPushButton.clicked.connect(() -> runnable.run());
		QUtils.trySetIcon(qPushButton, icon);
		return qPushButton;
	}

	private void updateRecafVersionLabel() {
		try {
			RecafVersion installedVersion = RecafTasks.getInstalledVersion();
			recafProgressBar.setText(installedVersion.getVersion());
		} catch (InvalidInstallationException ex) {
			recafProgressBar.setText("Not installed");
		}
	}

	private void updateJavafxLabel() {
		JavaFxVersion version = JavaFxTasks.detectCachedVersion();
		if (version != null) {
			String versionName = version.getVersion();
			if (version.getMajorVersion() <= JavaFxVersion.MIN_SUGGESTED) {
				jfxProgressBar.setText(versionName + " (Outdated)");
			} else {
				jfxProgressBar.setText(versionName);
			}
		} else {
			jfxProgressBar.setText("Not installed");
		}
	}

	private void updateRecafTask() {
		recafProgressBar.setText("Downloading...");
		RecafTasks.setDownloadListener(recafProgressBar.progressBar());

		// FIXME CURSED
		recafProgressBar.progressBar();
		CompletableFuture.runAsync(() -> {
			VersionUpdateResult result = RecafTasks.updateFromSnapshot("master");

			Throwable error = result.getError();
			if (error != null) {
				logger.error("Encountered error updating Recaf from latest snapshot", error);
			} else if (Objects.equals(result.getFrom(), result.getTo())) {
				logger.info("No updates for Recaf available, we are up-to-date.");
			}
		}).thenAccept(o -> recafProgressBar.setText("Done"));


	}

	private void updateJfxTask() {
		jfxProgressBar.setText("Downloading...");
		JavaFxTasks.setDownloadListener(jfxProgressBar.progressBar());
		JavaFxTasks.checkClearCache(true, false, 0, 0);

		jfxProgressBar.showProgressBar();
		CompletableFuture.runAsync(() -> {
			if (JavaFxTasks.update(-1, true) == null) {
				logger.error("Failed fetching JavaFX after update was initiated");
			}
		}).thenAccept(o -> jfxProgressBar.setText("Done"));
	}


	private void browseJavaVersions() {
		String targetFile = PlatformType.isWindows() ? "java.exe" : "java";

		QFileDialog qFileDialog = new QFileDialog();
		qFileDialog.setFileMode(QFileDialog.FileMode.ExistingFile);
		qFileDialog.setNameFilter(targetFile);

		qFileDialog.fileSelected.connect((selectedFile) -> {
			Path selectedPath = Paths.get(selectedFile);
			if (Files.isSymbolicLink(selectedPath)) {
				selectedPath = SymLinks.resolveSymLink(selectedPath);
				if (selectedPath == null) {
					QUtils.showErrorMessage("Invalid symbolic link", "The selected sym-link could not be followed to a valid Java executable");
					return;
				}
			}
			JavaEnvTasks.AdditionResult addResult = JavaEnvTasks.addJavaInstall(selectedPath);
			if (addResult.wasSuccess()) {
				JavaInstall javaInstall = JavaEnvTasks.getByPath(selectedPath);
				if (javaInstall != null) {
					int version = javaInstall.getVersion();
					if (version < JavaVersion.MIN_COMPATIBLE) {
						String message = "The selected Java executable only supports up to: " + version + "\nThe minimum required version is: " + JavaVersion.MIN_COMPATIBLE;

						QUtils.showErrorMessage("Incompatible selection", message);
						return;
					}
					this.javaVersionCombo.addItem(QUtils.formatJavaInstall(javaInstall), javaInstall);
				}
			} else {
				QUtils.showErrorMessage("Incompatible selection", addResult.message());
			}
		});

		qFileDialog.show();
	}

	private void goFindJavaInstallsAndPopulateCombo() {
		CompletableFuture.supplyAsync(() -> {
			JavaEnvTasks.scanForJavaInstalls();
			SortedSet<JavaInstall> installs = new TreeSet<>(JavaInstall.COMPARE_VERSIONS);
			JavaEnvTasks.getJavaInstalls().stream().filter(i -> i.getVersion() >= JavaVersion.MIN_COMPATIBLE).forEach(installs::add);

			return installs;
		}).thenAccept(javaInstalls -> {
			for (JavaInstall javaInstall : javaInstalls) {
				this.javaVersionCombo.addItem(QUtils.formatJavaInstall(javaInstall), javaInstall);
			}

			if (!javaInstalls.isEmpty()) {
				this.javaVersionCombo.setCurrentIndex(0);
			}
		});
	}
}
