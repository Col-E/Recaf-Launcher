package software.coley.recaf.launcher;

import software.coley.recaf.launcher.task.ExecutionTasks;

import javax.annotation.Nonnull;

/**
 * Outline of UX feedback for launching.
 */
public interface LauncherFeedback {
	/**
	 * Implementation for no feedback.
	 */
	LauncherFeedback NOOP = new LauncherFeedback() {};

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
