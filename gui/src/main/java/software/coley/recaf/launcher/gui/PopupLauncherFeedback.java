package software.coley.recaf.launcher.gui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import software.coley.recaf.launcher.LauncherFeedback;
import software.coley.recaf.launcher.LauncherGui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.util.concurrent.CompletableFuture;

/**
 * Popup window implementation of {@link LauncherFeedback}.
 *
 * @author Matt Coley
 */
public class PopupLauncherFeedback extends JDialog implements LauncherFeedback {
	public PopupLauncherFeedback(@Nullable Window owner) {
		super(owner);
		LauncherGui.setupLookAndFeel();
		setLocationRelativeTo(owner);
		setVisible(true);
		initComponents();
	}

	@Override
	public void updateLaunchProgressMessage(@Nonnull String message) {
		feedbackLabel.setText(message);
	}

	@Override
	public void finishLaunchProgress() {
		// Keep the last progress message of 'launching recaf' open for a bit since Recaf
		// takes a short bit to open up anyways.
		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException ignored) {}
			setVisible(false);
		});
	}

	private void onCancel() {
		System.exit(0);
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        feedbackLabel = new JLabel();
        feedbackBar = new JProgressBar();
        JPanel buttons = new JPanel();
        JButton cancelButton = new JButton();

        //======== this ========
        setTitle("Recaf is updating...");
        setMinimumSize(new Dimension(390, 190));
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "15dlu, default:grow, $lcgap, 15dlu",
            "fill:default:grow, 2*($lgap, default), $lgap, default:grow, $lgap, bottom:default"));

        //---- feedbackLabel ----
        feedbackLabel.setText("...");
        contentPane.add(feedbackLabel, CC.xy(2, 3, CC.CENTER, CC.DEFAULT));

        //---- feedbackBar ----
        feedbackBar.setValue(-1);
        feedbackBar.setIndeterminate(true);
        contentPane.add(feedbackBar, CC.xy(2, 5));

        //======== buttons ========
        {
            buttons.setLayout(new FormLayout(
                "default:grow, $lcgap, default",
                "fill:default:grow, $lgap, 8dlu"));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.setIcon(new ImageIcon(getClass().getResource("/images/stop.png")));
            cancelButton.addActionListener(e -> onCancel());
            buttons.add(cancelButton, CC.xy(3, 1));
        }
        contentPane.add(buttons, CC.xy(2, 9));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JLabel feedbackLabel;
    private JProgressBar feedbackBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
