package software.coley.recaf.launcher.gui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import software.coley.recaf.launcher.task.ExecutionTasks;
import software.coley.recaf.launcher.util.DesktopUtil;

import javax.annotation.Nonnull;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Panel for showing {@link ExecutionTasks.RunResult} content when Recaf exited with an error based exit code.
 *
 * @author Matt Coley
 */
public class ErrorPanel extends JPanel {
	/**
	 * @param result Result to show contents of.
	 */
	public ErrorPanel(@Nonnull ExecutionTasks.RunResult result) {
		initComponents();

		descriptionLabel.setText(result.getCodeDescription());

		// Standard out content
		String out = result.getOut();
		if (!out.trim().isEmpty()) {
			outText.setText(out);
		} else {
			outLabel.setVisible(false);
			outScroll.setVisible(false);
		}

		// Standard err content
		String err = result.getErr();
		if (!err.trim().isEmpty()) {
			errText.setText(err);
		} else {
			errLabel.setVisible(false);
			errScroll.setVisible(false);
		}

		setPreferredSize(new Dimension(700, 800));
	}

	private void onSubmit() {
		try {
			onCopyToClipboard();
			String body = "Diagnostics:\n" +
					"```\n" +
					"\nDiagnostic data has been copied to your clipboard. Please paste it here!\n\n" +
					"```\n";
			String bodyEncoded = URLEncoder.encode(body, StandardCharsets.UTF_8.name());
			DesktopUtil.showDocument(new URI("https://github.com/Col-E/Recaf/issues/new?labels=bug&title=Recaf%20crash&body={B}"
					.replace("{B}", bodyEncoded))
			);
		} catch (IOException | URISyntaxException ex) {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	private void onCopyToClipboard() {
		StringSelection stringSelection = new StringSelection(export());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	@Nonnull
	private String export() {
		return (descriptionLabel.getText() + "\n\n" + outText.getText() + "\n" + errText.getText()).trim();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        descriptionLabel = new JLabel();
        buttonPanel = new JPanel();
        submitButton = new JButton();
        copyButton = new JButton();
        outLabel = new JLabel();
        outScroll = new JScrollPane();
        outText = new JTextArea();
        errLabel = new JLabel();
        errScroll = new JScrollPane();
        errText = new JTextArea();

        //======== this ========
        setLayout(new FormLayout(
            "[600px,default,1000px]:grow",
            "3*(default, $lgap), fill:default:grow, $lgap, default, $lgap, fill:default:grow"));

        //---- descriptionLabel ----
        descriptionLabel.setText("Recaf failed to launch");
        add(descriptionLabel, CC.xy(1, 1, CC.CENTER, CC.DEFAULT));

        //======== buttonPanel ========
        {
            buttonPanel.setLayout(new FormLayout(
                "default:grow, 2*($lcgap, default), $lcgap, default:grow",
                "default"));

            //---- submitButton ----
            submitButton.setText("Submit Report");
            submitButton.setIcon(new ImageIcon(getClass().getResource("/images/report.png")));
            submitButton.addActionListener(e -> onSubmit());
            buttonPanel.add(submitButton, CC.xy(3, 1));

            //---- copyButton ----
            copyButton.setText("Copy to Clipboard");
            copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
            copyButton.addActionListener(e -> onCopyToClipboard());
            buttonPanel.add(copyButton, CC.xy(5, 1));
        }
        add(buttonPanel, CC.xy(1, 3));

        //---- outLabel ----
        outLabel.setText("Console output [out]");
        outLabel.setFont(outLabel.getFont().deriveFont(outLabel.getFont().getStyle() | Font.BOLD));
        add(outLabel, CC.xy(1, 5));

        //======== outScroll ========
        {

            //---- outText ----
            outText.setFont(new Font(Font.MONOSPACED, outText.getFont().getStyle(), outText.getFont().getSize()));
            outScroll.setViewportView(outText);
        }
        add(outScroll, CC.xy(1, 7));

        //---- errLabel ----
        errLabel.setText("Console output [err]");
        errLabel.setFont(errLabel.getFont().deriveFont(errLabel.getFont().getStyle() | Font.BOLD));
        add(errLabel, CC.xy(1, 9));

        //======== errScroll ========
        {

            //---- errText ----
            errText.setFont(new Font(Font.MONOSPACED, errText.getFont().getStyle(), errText.getFont().getSize()));
            errScroll.setViewportView(errText);
        }
        add(errScroll, CC.xy(1, 11));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JLabel descriptionLabel;
    private JPanel buttonPanel;
    private JButton submitButton;
    private JButton copyButton;
    private JLabel outLabel;
    private JScrollPane outScroll;
    private JTextArea outText;
    private JLabel errLabel;
    private JScrollPane errScroll;
    private JTextArea errText;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
