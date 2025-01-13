package dev.fxe;

import io.qt.widgets.QLabel;
import io.qt.widgets.QStackedWidget;
import software.coley.recaf.launcher.util.TransferListener;

public class QWrappedComponent extends QStackedWidget {

	private final QLabel qLabel;
	private final QProgressBarWidget qProgressBarWidget;

	public QWrappedComponent(int fallback) {
		qLabel = new QLabel();
		qProgressBarWidget = new QProgressBarWidget(fallback);
		this.addWidget(qLabel);
		this.addWidget(qProgressBarWidget);
	}

	public void setText(String text) {
		setCurrentIndex(0);
		qLabel.setText(text);
	}

	public void showProgressBar() {
		setCurrentIndex(1);
	}

	public TransferListener progressBar() {
		return qProgressBarWidget;
	}
}

