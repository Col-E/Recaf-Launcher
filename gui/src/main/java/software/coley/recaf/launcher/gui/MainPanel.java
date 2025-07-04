package software.coley.recaf.launcher.gui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import software.coley.recaf.launcher.LauncherFeedback;
import software.coley.recaf.launcher.LauncherGui;
import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.RecafVersion;
import software.coley.recaf.launcher.task.JavaEnvTasks;
import software.coley.recaf.launcher.task.JavaFxTasks;
import software.coley.recaf.launcher.task.RecafTasks;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.ResettableDelayTask;
import software.coley.recaf.launcher.util.TransferListener;

import javax.annotation.Nonnull;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Panel for updating Recaf, JavaFX, and picking which JVM to run Recaf with.
 */
public class MainPanel extends BrowsableJavaVersionPanel {
	private static final String CARD_INFO = "info";
	private static final String CARD_FEEDBACK = "feedback";
	private final JFrame frame;
	private final LauncherFeedback feedback = new FeedbackImpl(this);
	private CompletableFuture<Void> watchFuture;
	private boolean watching = true;

	/**
	 * @param frame
	 * 		Parent frame this panel belongs to.
	 */
	public MainPanel(@Nonnull JFrame frame) {
		this.frame = frame;

		initComponents();

		// Initially show the version labels
		recafVersionWrapper.add(recafVersionLabel, BorderLayout.CENTER);
		javafxVersionWrapper.add(javafxVersionLabel, BorderLayout.CENTER);

		// Setup install selection & style
		// When complete, do enable version label state tracking
		setupInstallCombo();
		repopulateInstallModel(true).thenRun(this::setupVersionTracking);

		// Setup cards
		add(CARD_INFO, versionsCard);
		add(CARD_FEEDBACK, feedbackCard);
	}

	/**
	 * Setup automatic version label tracking.
	 */
	private void setupVersionTracking() {
		updateRecafLabel();
		updateJavafxLabel();

		watchFuture = CompletableFuture.runAsync(() -> {
			try {
				ResettableDelayTask delayableUpdate = new ResettableDelayTask("Label-Update", () -> {
					updateJavafxLabel();
					updateRecafLabel();
				}, 500, TimeUnit.MILLISECONDS);
				Path recafDirectory = CommonPaths.getRecafDirectory();
				WatchService watchService = FileSystems.getDefault().newWatchService();
				recafDirectory.register(watchService,
						StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_DELETE,
						StandardWatchEventKinds.ENTRY_MODIFY);
				// Infinite loop to continuously watch for events
				while (watching) {
					WatchKey key = watchService.take();
					for (WatchEvent<?> event : key.pollEvents()) {
						// So normally you'd want to do something like this:
						/*
						Path localEventPath = (Path) event.context();
						Path actualPath = recafDirectory.resolve(localEventPath);
						if (actualPath.startsWith(CommonPaths.getDependenciesDir())) {
							updateJavafxLabel();
						} else if (actualPath.equals(CommonPaths.getRecafJar())) {
							updateRecafLabel();
						}
						 */

						// However, the order of events makes that not super reliable.
						// So instead we just refresh the labels for any change in the directory.
						delayableUpdate.startOrReset();
					}

					// To receive further events, reset the key
					key.reset();
				}
			} catch (IOException | InterruptedException ignored) {
			}
		});

		installCombo.addItemListener(e -> updateJavafxLabel());
	}

	/**
	 * Update the Recaf label to match what is found at {@link CommonPaths#getRecafJar()}.
	 */
	private void updateRecafLabel() {
		try {
			RecafVersion installedVersion = RecafTasks.getInstalledVersion();
			recafVersionLabel.setText(installedVersion.getVersion());
		} catch (InvalidInstallationException ex) {
			recafVersionLabel.setText("<html><p style=\"color: #780000; font-weight: bold;\">Not installed</p></html>");
		}
	}

	/**
	 * Update the JavaFX label to match what is in the {@link CommonPaths#getDependenciesDir()}.
	 */
	private void updateJavafxLabel() {
		String updateText = "Update";
		JavaFxVersion fxVersion = JavaFxTasks.detectCachedVersion();
		if (fxVersion != null) {
			String versionName = fxVersion.getVersion();
			int requiredJavaVersion = fxVersion.getRequiredJavaVersion();
			if (requiredJavaVersion > getSelectedJavaInstall().getVersion()) {
				// JavaFX depends on newer version of Java
				javafxVersionLabel.setText("<html><p>" + versionName +
						"<span style=\"color: #780000; font-weight: bold;\"> " +
						"(Requires Java " + requiredJavaVersion + ")</span></p></html>");
				updateText = "Downgrade";
			} else if (fxVersion.getMajorVersion() <= JavaFxVersion.MIN_SUGGESTED_JFX_VERSION) {
				// JavaFX is older than what Recaf requires
				javafxVersionLabel.setText("<html><p>" + versionName +
						"<span style=\"color: #780000; font-weight: bold;\"> " +
						"(Outdated)</span></p></html>");
			} else {
				javafxVersionLabel.setText(versionName);
			}
		} else {
			// JavaFX is not installed
			javafxVersionLabel.setText("<html><p style=\"color: #780000; font-weight: bold;\">Not installed</p></html>");
		}
		updateJavafxButton.setText(updateText);
	}

	/**
	 * @return Selected Java install.
	 */
	@Nonnull
	private JavaInstall getSelectedJavaInstall() {
		Object selectedItem = installCombo.getSelectedItem();
		if (selectedItem instanceof JavaInstall)
			return (JavaInstall) selectedItem;

		// Fall back to first available install
		Collection<JavaInstall> installs = JavaEnvTasks.getJavaInstalls();
		if (installs.isEmpty())
			throw new IllegalStateException("Failed to populate JVM install combo-box");
		return installs.iterator().next();
	}

	@Nonnull
	@Override
	protected JButton getBrowseButton() {
		return browseInstallButton;
	}

	@Nonnull
	@Override
	protected JComboBox<JavaInstall> getInstallCombo() {
		return installCombo;
	}

	/**
	 * Update Recaf.
	 *
	 * @see #updateRecafButton
	 */
	private void updateRecaf() {
		CompletableFuture.runAsync(() -> {
			// Swap out version label for progress bar
			recafVersionProgress.setIndeterminate(true);
			recafVersionWrapper.removeAll();
			recafVersionWrapper.add(recafVersionProgress, BorderLayout.CENTER);
			recafVersionWrapper.revalidate();

			// Disable button and update the label
			updateRecafButton.setEnabled(false);
			recafVersionLabel.setText("<html><i>Updating...</i></html>");

			LauncherGui.updateRecaf(feedback);

			// Put the label back in its original place and re-enable the button
			recafVersionProgress.setIndeterminate(false);
			recafVersionWrapper.removeAll();
			recafVersionWrapper.add(BorderLayout.CENTER, recafVersionLabel);
			recafVersionWrapper.revalidate();
			updateRecafButton.setEnabled(true);

			// Ensure the label is updated in case no actual updated were determined to have been necessary
			updateRecafLabel();
		});
	}

	/**
	 * Update local JavaFX files.
	 *
	 * @see #updateJavafxButton
	 */
	private void updateJavafx() {
		CompletableFuture.runAsync(() -> {
			// Swap out version label for progress bar
			javafxVersionProgress.setIndeterminate(true);
			javafxVersionWrapper.removeAll();
			javafxVersionWrapper.add(javafxVersionProgress, BorderLayout.CENTER);
			javafxVersionWrapper.revalidate();

			// Disable button and update the label
			updateJavafxButton.setEnabled(false);
			javafxVersionLabel.setText("<html><i>Updating...</i></html>");

			LauncherGui.updateJavafx(feedback, getSelectedJavaInstall().getVersion());

			// Put the label back in its original place and re-enable the button
			javafxVersionProgress.setIndeterminate(false);
			javafxVersionWrapper.removeAll();
			javafxVersionWrapper.add(BorderLayout.CENTER, javafxVersionLabel);
			javafxVersionWrapper.revalidate();
			updateJavafxButton.setEnabled(true);
		});
	}

	/**
	 * Launch Recaf.
	 *
	 * @see #launchButton
	 */
	private void launch() {
		CompletableFuture.runAsync(() -> {
			// Swap to the feedback display card.
			CardLayout layout = (CardLayout) getLayout();
			layout.show(this, CARD_FEEDBACK);

			// Initiate launch.
			LauncherGui.launch(feedback, false);
		}, LauncherGui.nonExitingAsyncExecutor);
	}

	/**
	 * Prompts the user to select a Java executable, and adds it to {@link #getInstallCombo()}.
	 *
	 * @see BrowsableJavaVersionPanel
	 */
	private void browseForInstall() {
		onBrowseForInstall();
	}

	private class FeedbackImpl implements LauncherFeedback {
		private final Container container;

		public FeedbackImpl(@Nonnull Container container) {
			this.container = container;
		}

		@Nonnull
		@Override
		public TransferListener provideJavaFxDownloadListener() {
			return new ProgressBarTransferListener(JavaFxTasks.FALLBACK_FX_SIZE_BYTES, javafxVersionProgress);
		}

		@Nonnull
		@Override
		public TransferListener provideRecafDownloadListener() {
			return new ProgressBarTransferListener(RecafTasks.FALLBACK_RECAF_SIZE_BYTES, recafVersionProgress);
		}

		@Override
		public void updateLaunchProgressMessage(@Nonnull String message) {
			feedbackLabel.setText(message);
		}

		@Override
		public void finishLaunchProgress(boolean success) {
			if (success) {
				feedbackLabel.setText("Launched!");
				feedbackProgressBar.setIndeterminate(false);
				feedbackProgressBar.setValue(100);

				// Cancel watch service
				watching = false;
				if (watchFuture != null) watchFuture.cancel(true);

				// Hide the UI now that the launch is complete.
				frame.setVisible(false);
				frame.dispose();
			} else {
				// Swap back to the info display card.
				CardLayout layout = (CardLayout) getLayout();
				layout.show(container, CARD_INFO);
			}
		}
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        versionsCard = new JPanel();
        JLabel recafVersionPrefix = new JLabel();
        recafVersionWrapper = new JPanel();
        updateRecafButton = new JButton();
        JLabel javafxVersionPrefix = new JLabel();
        javafxVersionWrapper = new JPanel();
        updateJavafxButton = new JButton();
        JLabel installLabel = new JLabel();
        installCombo = new JComboBox<>();
        browseInstallButton = new JButton();
        launchButton = new JButton();
        feedbackCard = new JPanel();
        feedbackLabel = new JLabel();
        feedbackProgressBar = new JProgressBar();
        recafVersionLabel = new JLabel();
        recafVersionProgress = new JProgressBar();
        javafxVersionLabel = new JLabel();
        javafxVersionProgress = new JProgressBar();

        //======== this ========
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setLayout(new CardLayout());

        //======== versionsCard ========
        {
            versionsCard.setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default",
                "3*(default, $lgap), default"));

            //---- recafVersionPrefix ----
            recafVersionPrefix.setText("Recaf Version:");
            versionsCard.add(recafVersionPrefix, CC.xy(1, 1));

            //======== recafVersionWrapper ========
            {
                recafVersionWrapper.setLayout(new BorderLayout());
            }
            versionsCard.add(recafVersionWrapper, CC.xy(3, 1));

            //---- updateRecafButton ----
            updateRecafButton.setText("Update");
            updateRecafButton.setIcon(new ImageIcon(getClass().getResource("/images/down.png")));
            updateRecafButton.addActionListener(e -> updateRecaf());
            versionsCard.add(updateRecafButton, CC.xy(5, 1));

            //---- javafxVersionPrefix ----
            javafxVersionPrefix.setText("JavaFX Version:");
            versionsCard.add(javafxVersionPrefix, CC.xy(1, 3));

            //======== javafxVersionWrapper ========
            {
                javafxVersionWrapper.setLayout(new BorderLayout());
            }
            versionsCard.add(javafxVersionWrapper, CC.xy(3, 3));

            //---- updateJavafxButton ----
            updateJavafxButton.setText("Update");
            updateJavafxButton.setIcon(new ImageIcon(getClass().getResource("/images/down.png")));
            updateJavafxButton.addActionListener(e -> updateJavafx());
            versionsCard.add(updateJavafxButton, CC.xy(5, 3));

            //---- installLabel ----
            installLabel.setText("Java Version:");
            versionsCard.add(installLabel, CC.xy(1, 5));
            versionsCard.add(installCombo, CC.xy(3, 5));

            //---- browseInstallButton ----
            browseInstallButton.setText("Browse");
            browseInstallButton.setIcon(new ImageIcon(getClass().getResource("/images/select.png")));
            browseInstallButton.addActionListener(e -> browseForInstall());
            versionsCard.add(browseInstallButton, CC.xy(5, 5));

            //---- launchButton ----
            launchButton.setText("Launch");
            launchButton.setIcon(new ImageIcon(getClass().getResource("/images/run.png")));
            launchButton.addActionListener(e -> launch());
            versionsCard.add(launchButton, CC.xy(5, 7));
        }

        //======== feedbackCard ========
        {
            feedbackCard.setLayout(new FormLayout(
                "center:default:grow",
                "default:grow, 2*($lgap, default), $lgap, default:grow"));
            feedbackCard.add(feedbackLabel, CC.xy(1, 3));

            //---- feedbackProgressBar ----
            feedbackProgressBar.setIndeterminate(true);
            feedbackProgressBar.setValue(-1);
            feedbackCard.add(feedbackProgressBar, CC.xy(1, 5));
        }

        //---- recafVersionProgress ----
        recafVersionProgress.setIndeterminate(true);
        recafVersionProgress.setStringPainted(true);

        //---- javafxVersionProgress ----
        javafxVersionProgress.setIndeterminate(true);
        javafxVersionProgress.setStringPainted(true);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JPanel versionsCard;
    private JPanel recafVersionWrapper;
    private JButton updateRecafButton;
    private JPanel javafxVersionWrapper;
    private JButton updateJavafxButton;
    private JComboBox<JavaInstall> installCombo;
    private JButton browseInstallButton;
    private JButton launchButton;
    private JPanel feedbackCard;
    private JLabel feedbackLabel;
    private JProgressBar feedbackProgressBar;
    private JLabel recafVersionLabel;
    private JProgressBar recafVersionProgress;
    private JLabel javafxVersionLabel;
    private JProgressBar javafxVersionProgress;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
