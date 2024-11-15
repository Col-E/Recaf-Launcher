package software.coley.recaf.launcher.gui;

import software.coley.recaf.launcher.util.StringUtil;
import software.coley.recaf.launcher.util.TransferListener;

import javax.annotation.Nonnull;
import javax.swing.JProgressBar;

/**
 * Transfer listener that displays feedback in a {@link JProgressBar}.
 */
public class ProgressBarTransferListener implements TransferListener {
	private final JProgressBar progressBar;
	private final int fallbackMax;

	/**
	 * @param fallbackMax
	 * 		Fallback max value if the transfer max value is invalid.
	 * @param progressBar
	 * 		Bar to show transfer progress with.
	 */
	public ProgressBarTransferListener(int fallbackMax, @Nonnull JProgressBar progressBar) {
		this.progressBar = progressBar;
		this.fallbackMax = fallbackMax;
	}

	@Override
	public void start(int max) {
		if (max <= 0)
			max = fallbackMax;
		progressBar.setValue(0);
		progressBar.setIndeterminate(false);
		progressBar.setStringPainted(false);
		progressBar.setMaximum(max);
	}

	@Override
	public void progress(int current, int max) {
		progressBar.setIndeterminate(false);
		progressBar.setStringPainted(true);
		progressBar.setString(StringUtil.humanReadableByteCountSI(current));
		progressBar.setValue(current);
	}

	@Override
	public void end(int current, int max) {
		progressBar.setIndeterminate(false);
		progressBar.setValue(current);
		if (current >= max)
			progressBar.setStringPainted(false);
	}
}
