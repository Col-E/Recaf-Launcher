package software.coley.recaf.launcher;

import software.coley.recaf.launcher.task.ExecutionTasks;
import software.coley.recaf.launcher.util.TransferListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Outline of UX feedback for launching.
 */
public interface LauncherFeedback {
	/**
	 * Implementation for no feedback.
	 */
	LauncherFeedback NOOP = new LauncherFeedback() {};

	/**
	 * @return Transfer listener to use for downloading JavaFX artifacts.
	 */
	@Nullable
	default TransferListener provideJavaFxDownloadListener() { return null; }

	/**
	 * @return Transfer listener to use for downloading Recaf.
	 */
	@Nullable
	default TransferListener provideRecafDownloadListener() { return null; }

	/**
	 * Called to notify the feedback implementation of our current state.
	 *
	 * @param message
	 * 		Message detailing current launcher state.
	 */
	default void updateLaunchProgressMessage(@Nonnull String message) {}

	/**
	 * Called to notify the feedback implementation the launcher is finished {@link ExecutionTasks#run(boolean, String)}.
	 */
	default void finishLaunchProgress() {}
}
