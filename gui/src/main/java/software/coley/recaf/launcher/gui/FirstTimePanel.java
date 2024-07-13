package software.coley.recaf.launcher.gui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import software.coley.recaf.launcher.config.Config;
import software.coley.recaf.launcher.config.LaunchAction;
import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.JavaVersion;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Panel for walking the user through important first-time setup.
 */
public class FirstTimePanel extends BrowsableJavaVersionPanel {
	private static final int MAX_CARD = 2;
	private final Runnable onFinish;
	private int cardIndex;

	public FirstTimePanel(@Nonnull Runnable onFinish) {
		initComponents();

		this.onFinish = onFinish;

		// Setup radio button toggle behavior
		ButtonGroup behaviorButtonGroup = new ButtonGroup();
		behaviorButtonGroup.add(letMeChooseRadio);
		behaviorButtonGroup.add(runRadio);
		behaviorButtonGroup.add(updateRunRadio);
		enableBadOptionsCheck.addActionListener(e -> {
			boolean allowBad = enableBadOptionsCheck.isSelected();
			runRadio.setEnabled(allowBad);
			if (runRadio.isSelected() && !allowBad)
				updateRunRadio.setSelected(true);
		});

		// Setup install selection & style
		setupInstallCombo();
		repopulateInstallModel(true);
		cardInstalls.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				int width = Math.min(100, cardHolderPanel.getWidth() - installCombo.getX());
				Dimension d = new Dimension(width, installCombo.getHeight());
				installCombo.setPreferredSize(d);
				browseInstallButton.setPreferredSize(d);
			}
		});
		requirementsLabel.setText("Recaf 4.X requires Java " + JavaVersion.MIN_COMPATIBLE + " or higher");

		// Setup cards
		cardHolderPanel.add("0", cardWelcome);
		cardHolderPanel.add("1", cardOpen);
		cardHolderPanel.add("2", cardInstalls);
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
	 * Moves to the prior card.
	 */
	private void onPrev() {
		if (cardIndex > 0) {
			cardIndex--;
			updateCard();
		}
	}

	/**
	 * Moves to the next card.
	 */
	private void onNext() {
		if (cardIndex < MAX_CARD) {
			cardIndex++;
			updateCard();
		} else {
			onFinish();
		}
	}

	/**
	 * Finishes the first-time setup.
	 */
	private void onFinish() {
		// Update config with input options.
		Config config = Config.get();
		LaunchAction action = LaunchAction.SHOW_LAUNCHER;
		if (runRadio.isSelected()) {
			action = LaunchAction.RUN_RECAF;
		} else if (updateRunRadio.isSelected()) {
			action = LaunchAction.UPDATE_RUN_RECAF;
		}
		config.setLaunchJavaInstallation((JavaInstall) installCombo.getSelectedItem());
		config.setLaunchAction(action);
		config.invalidateFirstTime();

		// Delegate to on-finish task passed to constructor.
		onFinish.run();
	}

	/**
	 * Update the displayed card.
	 */
	private void updateCard() {
		prevButton.setVisible(cardIndex > 0);
		nextButton.setText(cardIndex < MAX_CARD ? "Next" : "Finish");

		CardLayout layout = (CardLayout) (cardHolderPanel.getLayout());
		layout.show(cardHolderPanel, String.valueOf(cardIndex));
	}

	/**
	 * Prompts the user to select a Java executable, and adds it to {@link #getInstallCombo()}.
	 *
	 * @see BrowsableJavaVersionPanel
	 */
    private void browseForInstall() {
        onBrowseForInstall();
    }

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        JPanel bottomNavPanel = new JPanel();
        prevButton = new JButton();
        nextButton = new JButton();
        cardHolderPanel = new JPanel();
        cardOpen = new JPanel();
        JLabel whenOpenLabel = new JLabel();
        letMeChooseRadio = new JRadioButton();
        updateRunRadio = new JRadioButton();
        runRadio = new JRadioButton();
        enableBadOptionsCheck = new JCheckBox();
        cardInstalls = new JPanel();
        JLabel launchWithLabel = new JLabel();
        JLabel installLabel = new JLabel();
        installCombo = new JComboBox<>();
        JLabel customLabel = new JLabel();
        browseInstallButton = new JButton();
        requirementsLabel = new JLabel();
        cardWelcome = new JPanel();
        JLabel logoLabel = new JLabel();
        JLabel welcomeLabel = new JLabel();
        JLabel noticeLabel = new JLabel();

        //======== this ========
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setLayout(new BorderLayout());

        //======== bottomNavPanel ========
        {
            bottomNavPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
            bottomNavPanel.setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default",
                "default"));

            //---- prevButton ----
            prevButton.setText("Previous");
            prevButton.setVisible(false);
            prevButton.addActionListener(e -> onPrev());
            bottomNavPanel.add(prevButton, CC.xy(1, 1));

            //---- nextButton ----
            nextButton.setText("Next");
            nextButton.addActionListener(e -> onNext());
            bottomNavPanel.add(nextButton, CC.xy(5, 1));
        }
        add(bottomNavPanel, BorderLayout.SOUTH);

        //======== cardHolderPanel ========
        {
            cardHolderPanel.setLayout(new CardLayout());
        }
        add(cardHolderPanel, BorderLayout.CENTER);

        //======== cardOpen ========
        {
            cardOpen.setLayout(new FormLayout(
                "2*(default, $lcgap), default:grow",
                "12dlu, 3*($lgap, default), $lgap, fill:default:grow, $lgap, default"));

            //---- whenOpenLabel ----
            whenOpenLabel.setText("When I open this launcher I want to:");
            whenOpenLabel.setFont(whenOpenLabel.getFont().deriveFont(whenOpenLabel.getFont().getStyle() | Font.BOLD));
            cardOpen.add(whenOpenLabel, CC.xywh(1, 1, 5, 2));

            //---- letMeChooseRadio ----
            letMeChooseRadio.setText("Let me choose what to do");
            letMeChooseRadio.setSelected(true);
            cardOpen.add(letMeChooseRadio, CC.xy(5, 3));

            //---- updateRunRadio ----
            updateRunRadio.setText("Update then run Recaf, do not show the launcher");
            cardOpen.add(updateRunRadio, CC.xy(5, 5));

            //---- runRadio ----
            runRadio.setText("Run Recaf, do not show the launcher");
            runRadio.setEnabled(false);
            cardOpen.add(runRadio, CC.xy(5, 7));

            //---- enableBadOptionsCheck ----
            enableBadOptionsCheck.setText("Enable bad options");
            cardOpen.add(enableBadOptionsCheck, CC.xy(5, 11));
        }

        //======== cardInstalls ========
        {
            cardInstalls.setLayout(new FormLayout(
                "2*(default, $lcgap), default:grow",
                "12dlu, 3*($lgap, default)"));

            //---- launchWithLabel ----
            launchWithLabel.setText("Launch Recaf with this version of Java:");
            launchWithLabel.setFont(launchWithLabel.getFont().deriveFont(launchWithLabel.getFont().getStyle() | Font.BOLD));
            cardInstalls.add(launchWithLabel, CC.xywh(1, 1, 5, 1));

            //---- installLabel ----
            installLabel.setText("Installation");
            cardInstalls.add(installLabel, CC.xy(3, 3));
            cardInstalls.add(installCombo, CC.xy(5, 3));

            //---- customLabel ----
            customLabel.setText("Add custom");
            cardInstalls.add(customLabel, CC.xy(3, 5));

            //---- browseInstallButton ----
            browseInstallButton.setText("Browse");
            browseInstallButton.setIcon(new ImageIcon(getClass().getResource("/images/select.png")));
            browseInstallButton.addActionListener(e -> browseForInstall());
            cardInstalls.add(browseInstallButton, CC.xy(5, 5));

            //---- requirementsLabel ----
            requirementsLabel.setText("Requirements:");
            requirementsLabel.setEnabled(false);
            cardInstalls.add(requirementsLabel, CC.xy(5, 7));
        }

        //======== cardWelcome ========
        {
            cardWelcome.setLayout(new FormLayout(
                "center:default:grow",
                "2*(default, $lgap), default"));

            //---- logoLabel ----
            logoLabel.setIcon(new ImageIcon(getClass().getResource("/images/logo.png")));
            cardWelcome.add(logoLabel, CC.xy(1, 1));

            //---- welcomeLabel ----
            welcomeLabel.setText("Welcome to the 4.X launcher");
            welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(welcomeLabel.getFont().getStyle() | Font.BOLD));
            cardWelcome.add(welcomeLabel, CC.xy(1, 3));

            //---- noticeLabel ----
            noticeLabel.setText("This setup will only appear once");
            cardWelcome.add(noticeLabel, CC.xy(1, 5));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JButton prevButton;
    private JButton nextButton;
    private JPanel cardHolderPanel;
    private JPanel cardOpen;
    private JRadioButton letMeChooseRadio;
    private JRadioButton updateRunRadio;
    private JRadioButton runRadio;
    private JCheckBox enableBadOptionsCheck;
    private JPanel cardInstalls;
    private JComboBox<JavaInstall> installCombo;
    private JButton browseInstallButton;
    private JLabel requirementsLabel;
    private JPanel cardWelcome;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
