package software.coley.recaf.launcher.gui;

import software.coley.recaf.launcher.LauncherGui;
import software.coley.recaf.launcher.config.Config;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.info.PlatformType;
import software.coley.recaf.launcher.task.JavaEnvTasks;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/**
 * Common parent class for handling selection of installed Java versions.
 */
public abstract class BrowsableJavaVersionPanel extends JPanel {
	private static File lastJavaInstallSelectionDir;

	@Nonnull
	protected abstract JButton getBrowseButton();

	@Nonnull
	protected abstract JComboBox<JavaInstall> getInstallCombo();

	/**
	 * Set up the {@link #getInstallCombo() Java installation combobox}.
	 * <ul>
	 *     <li>Selection of an installation updates the config</li>
	 *     <li>Custom renderer for {@link JavaInstall} instances</li>
	 * </ul>
	 */
	protected void setupInstallCombo() {
		JComboBox<JavaInstall> combo = getInstallCombo();
		combo.addItemListener(e -> {
			Object item = combo.getSelectedItem();
			if (item instanceof JavaInstall) {
				Config.get().setLaunchJavaInstallation((JavaInstall) item);
			}
		});
		combo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof JavaInstall) {
					JavaInstall installValue = (JavaInstall) value;
					Path homePath = installValue.getJavaExecutable().getParent().getParent();
					Path vendorPath = homePath.getParent();
					setText("<html><b style=\"font-family: monospace;\">" + installValue.getVersion() + "</b> : " + vendorPath.getFileName() + " / " + homePath.getFileName() + "</html>");
				} else if (value == null) {
					setText("<html><b style=\"font-family: monospace;\">" + JavaVersion.get() + "</b> : " + System.getProperty("java.vendor") + " / Current JVM</html>");
				}
				return this;
			}
		});
	}

	/**
	 * Prompts the user to select a Java executable, and adds it to {@link #getInstallCombo()}.
	 */
	protected void onBrowseForInstall() {
		FileDialog dialog = new FileDialog((Frame) null);
		if (lastJavaInstallSelectionDir != null)
			dialog.setDirectory(lastJavaInstallSelectionDir.getAbsolutePath());
		String targetFile = PlatformType.isWindows() ? "java.exe" : "java";

		dialog.setTitle("Select a '" + targetFile + "' executable");
		dialog.setFilenameFilter((dir, name) -> name.equalsIgnoreCase(targetFile));
		dialog.setMode(FileDialog.LOAD);
		dialog.setVisible(true);

		if (dialog.getFile() == null) {
			return;
		}

		File chosenFile = new File(dialog.getDirectory(), dialog.getFile());
		dialog.dispose();

		Path selectedPath = chosenFile.toPath();
		if (JavaEnvTasks.addJavaInstall(selectedPath)) {
			// Validate the selected installation was a compatible version
			JavaInstall install = JavaEnvTasks.getByPath(selectedPath);
			if (install != null) {
				int version = install.getVersion();
				if (version < JavaVersion.MIN_COMPATIBLE) {
					String message = "The selected Java executable only supports up to: " + version +
							"\nThe minimum required version is: " + JavaVersion.MIN_COMPATIBLE;
					JOptionPane.showMessageDialog(null, message, "Incompatible selection", JOptionPane.ERROR_MESSAGE, LauncherGui.recafIcon);
					Toolkit.getDefaultToolkit().beep();
					return;
				}

				// Set this as the selected version to run with.
				JComboBox<JavaInstall> combo = getInstallCombo();
				ComboBoxModel<JavaInstall> model = combo.getModel();
				if (model instanceof DefaultComboBoxModel) {
					model.setSelectedItem(install);
				}
			}

			// Refresh the combo-model list.
			repopulateInstallModel(false);
		} else {
			JOptionPane.showMessageDialog(null, "The selected file was not a Java executable", "Incompatible selection", JOptionPane.ERROR_MESSAGE, LauncherGui.recafIcon);
			Toolkit.getDefaultToolkit().beep();
		}
		lastJavaInstallSelectionDir = chosenFile.getParentFile();
	}

	/**
	 * Repopulate the {@link #getInstallCombo() Java installation combobox}'s model.
	 *
	 * @param doScan
	 *        {@code true} to live-scan for new installed versions of Java on the machine.
	 */
	protected void repopulateInstallModel(boolean doScan) {
		CompletableFuture.runAsync(() -> {
			// Scan for installs
			if (doScan)
				JavaEnvTasks.scanForJavaInstalls();
			SortedSet<JavaInstall> installs = new TreeSet<>(JavaInstall.COMPARE_VERSIONS);
			JavaEnvTasks.getJavaInstalls().stream()
					.filter(i -> i.getVersion() >= JavaVersion.MIN_COMPATIBLE)
					.forEach(installs::add);

			// Populate model
			if (!installs.isEmpty()) {
				SwingUtilities.invokeLater(() -> {
					JComboBox<JavaInstall> combo = getInstallCombo();

					// Copy all matched installations to the model.
					DefaultComboBoxModel<JavaInstall> model = new DefaultComboBoxModel<>();
					installs.forEach(model::addElement);

					// If the combo-box hasn't had a selection specified, put it here.
					if (combo.getSelectedItem() == null) {
						JavaInstall selection = Config.get().getJavaInstall();
						if (selection == null) selection = installs.first();
						model.setSelectedItem(selection);
					}

					// Update the combo-box with the model.
					combo.setModel(model);
				});
			}
		});
	}
}
