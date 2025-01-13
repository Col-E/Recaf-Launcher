package dev.fxe;

import io.qt.widgets.QProgressBar;
import software.coley.recaf.launcher.util.StringUtil;
import software.coley.recaf.launcher.util.TransferListener;

import javax.annotation.Nonnull;

public class QProgressBarWidget extends QProgressBar implements TransferListener {
	private final int fallbackMax;

	public QProgressBarWidget(int fallbackMax) {
		this.fallbackMax = fallbackMax;
	}

	@Override
	public void init(@Nonnull String name) {
		TransferListener.super.init(name);
	}

	@Override
	public void start(int max) {
		if (max <= 0)
			max = fallbackMax;
		this.setFormat("");
		this.setValue(0);
		this.setMaximum(max);
	}

	@Override
	public void progress(int current, int max) {
		this.setFormat(StringUtil.humanReadableByteCountSI(current));
		this.setValue(current);
	}

	@Override
	public void end(int current, int max) {
		this.setValue(current);
		if (current >= max) {
			this.setFormat("Done");
		}
	}
}
