package software.coley.recaf.launch.gui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import software.coley.recaf.launch.commands.Compatibility;
import software.coley.recaf.launch.commands.Run;
import software.coley.recaf.launch.commands.UpdateJavaFX;
import software.coley.recaf.launch.commands.UpdateRecafFromCI;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.info.RecafVersion;
import software.coley.recaf.launch.util.UpdateResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Basic launcher window with buttons to update Recaf and its dependencies.
 */
public class LauncherWindow extends JFrame {
	private final ExecutorService service = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	});

	public LauncherWindow() {
		initComponents();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		updateJavafxVersionLabel();
		updateRecafVersionLabel();
		compatibilityCheck();
	}

	/**
	 * Refresh version label.
	 */
	private void updateRecafVersionLabel() {
		service.submit(() -> {
			RecafVersion recafVersion = RecafVersion.getInstalledVersion();
			recafVersionValueLabel.setText("<html>" + (recafVersion == null ? "<span style=\"color:red;\">Not installed</span>" : recafVersion) + "</html>");
		});
	}

	/**
	 * Refresh version label.
	 */
	private void updateJavafxVersionLabel() {
		service.submit(() -> {
			JavaFxVersion jfxVersion = JavaFxVersion.getLocalVersion();
			jfxVersionValueLabel.setText("<html>" + (jfxVersion == null ? "<span style=\"color:red;\">Not installed</span>" : jfxVersion) + "</html>");
		});
	}

	/**
	 * Updates Recaf.
	 */
	private void recafVersionUpdate() {
		launchButton.setEnabled(false);
		recafVersionUpdateButton.setEnabled(false);
		service.submit(() -> {
			// TODO: When released, replace with - UpdateRecaf.update(true);
			UpdateResult result = UpdateRecafFromCI.update("dev4");
			if (result.isSuccess())
				updateRecafVersionLabel();
			recafVersionUpdateButton.setEnabled(true);
			compatibilityCheck();
		});
	}

	/**
	 * Updates JavaFX.
	 */
	private void jfxVersionUpdate() {
		service.submit(() -> {
			JavaFxVersion javaFxVersion = UpdateJavaFX.update(true);
			if (javaFxVersion != null)
				updateJavafxVersionLabel();
			compatibilityCheck();
		});
	}

	/**
	 * Runs compatibility check, refreshes output labels.
	 */
	private void compatibilityCheck() {
		service.submit(() -> {
			EnumSet<Compatibility.CompatibilityProblem> problems = Compatibility.getCompatibilityProblem();
			if (problems.isEmpty()) {
				compatibilityValueLabel.setText("<html><span style=\"color:green;\">✔</span></html>");
			} else {
				int count = problems.size();
				String word = count == 1 ? "issue" : "issues";
				compatibilityValueLabel.setText("<html><span style=\"color:red;\">" + count + " " + word + ", see below</span></html>");
			}

			RecafVersion installedVersion = RecafVersion.getInstalledVersion();
			launchButton.setEnabled(problems.isEmpty() && installedVersion != null);
			if (installedVersion == null) {
				output.setText("- Recaf is not installed");
			} else if (problems.isEmpty()) {
				output.setText("Ready to launch Recaf " + installedVersion);
			} else {
				StringBuilder sb = new StringBuilder();
				for (Compatibility.CompatibilityProblem problem : problems)
					sb.append(" - ").append(problem.getMessage()).append("\n");
				output.setText(sb.toString());
			}
		});
	}

	/**
	 * Runs Recaf.
	 */
	private void launch() {
		Run.RunResult runResult = Run.run(false, null);
		if (runResult.isSuccess())
			dispose();
		else
			// TODO: Provide better user feedback
			output.setText("Failed to run Recaf: " + runResult.name());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
		// Generated using JFormDesigner non-commercial license
		inputs = new JPanel();
		recafVersionLabel = new JLabel();
		recafVersionValueLabel = new JLabel();
		recafVersionUpdateButton = new JButton();
		jfxVersionLabel = new JLabel();
		jfxVersionValueLabel = new JLabel();
		jfxVersionUpdateButton = new JButton();
		compatibilityLabel = new JLabel();
		compatibilityValueLabel = new JLabel();
		compatibilityCheckButton = new JButton();
		actionsPanel = new JPanel();
		launchButton = new JButton();
		outputs = new JPanel();
		outputScroll = new JScrollPane();
		output = new JTextArea();

		//======== this ========
		setTitle("Recaf Launcher");
		setName("launcher");
		setMinimumSize(new Dimension(420, 280));
		setPreferredSize(new Dimension(420, 280));
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"default:grow",
			"fill:default, fill:default:grow"));

		//======== inputs ========
		{
			inputs.setBorder(new EmptyBorder(8, 8, 8, 8));
			inputs.setLayout(new FormLayout(
				"default, $lcgap, default:grow, $lcgap, default",
				"3*(default, $lgap), default"));

			//---- recafVersionLabel ----
			recafVersionLabel.setText("Recaf Version:");
			inputs.add(recafVersionLabel, CC.xy(1, 1));

			//---- recafVersionValueLabel ----
			recafVersionValueLabel.setText("<pending...>");
			inputs.add(recafVersionValueLabel, CC.xy(3, 1));

			//---- recafVersionUpdateButton ----
			recafVersionUpdateButton.setText("Update");
			recafVersionUpdateButton.addActionListener(e -> recafVersionUpdate());
			inputs.add(recafVersionUpdateButton, CC.xy(5, 1));

			//---- jfxVersionLabel ----
			jfxVersionLabel.setText("JavaFX Version:");
			inputs.add(jfxVersionLabel, CC.xy(1, 3));

			//---- jfxVersionValueLabel ----
			jfxVersionValueLabel.setText("<pending...>");
			inputs.add(jfxVersionValueLabel, CC.xy(3, 3));

			//---- jfxVersionUpdateButton ----
			jfxVersionUpdateButton.setText("Update");
			jfxVersionUpdateButton.addActionListener(e -> jfxVersionUpdate());
			inputs.add(jfxVersionUpdateButton, CC.xy(5, 3));

			//---- compatibilityLabel ----
			compatibilityLabel.setText("System Compatibility:");
			inputs.add(compatibilityLabel, CC.xy(1, 5));

			//---- compatibilityValueLabel ----
			compatibilityValueLabel.setText("<pending...>");
			inputs.add(compatibilityValueLabel, CC.xy(3, 5));

			//---- compatibilityCheckButton ----
			compatibilityCheckButton.setText("Recheck");
			compatibilityCheckButton.addActionListener(e -> compatibilityCheck());
			inputs.add(compatibilityCheckButton, CC.xy(5, 5));

			//======== actionsPanel ========
			{
				actionsPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
				actionsPanel.setLayout(new FormLayout(
					"center:default:grow",
					"default"));

				//---- launchButton ----
				launchButton.setText("Launch");
				launchButton.setPreferredSize(new Dimension(100, 35));
				launchButton.addActionListener(e -> launch());
				actionsPanel.add(launchButton, CC.xy(1, 1));
			}
			inputs.add(actionsPanel, CC.xywh(1, 7, 5, 1));
		}
		contentPane.add(inputs, CC.xy(1, 1));

		//======== outputs ========
		{
			outputs.setBorder(new EmptyBorder(10, 10, 10, 10));
			outputs.setLayout(new FormLayout(
				"default:grow",
				"fill:default:grow"));

			//======== outputScroll ========
			{

				//---- output ----
				output.setEditable(false);
				output.setTabSize(4);
				outputScroll.setViewportView(output);
			}
			outputs.add(outputScroll, CC.xy(1, 1));
		}
		contentPane.add(outputs, CC.xy(1, 2));
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
	// Generated using JFormDesigner non-commercial license
	private JPanel inputs;
	private JLabel recafVersionLabel;
	private JLabel recafVersionValueLabel;
	private JButton recafVersionUpdateButton;
	private JLabel jfxVersionLabel;
	private JLabel jfxVersionValueLabel;
	private JButton jfxVersionUpdateButton;
	private JLabel compatibilityLabel;
	private JLabel compatibilityValueLabel;
	private JButton compatibilityCheckButton;
	private JPanel actionsPanel;
	private JButton launchButton;
	private JPanel outputs;
	private JScrollPane outputScroll;
	private JTextArea output;
	// JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
