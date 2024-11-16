package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;

/**
 * Listener for IO transfer operations.
 *
 * @see Stream
 * @see Web
 */
public interface TransferListener {
	/**
	 * Called before the transfer begins.
	 *
	 * @param name Name of transfer.
	 */
	default void init(@Nonnull String name) {}

	/**
	 * Called when the transfer begins.
	 *
	 * @param max
	 * 		Max length of transfer. Can be negative for unknown transfer content length.
	 */
	void start(int max);

	/**
	 * Called during progress updates for the transfer.
	 *
	 * @param current
	 * 		Current amount of bytes transferred.
	 * @param max
	 * 		Max length of transfer. Can be negative for unknown transfer content length.
	 */
	void progress(int current, int max);

	/**
	 * Called when the transfer completes.
	 *
	 * @param current
	 * 		Current amount of bytes transferred. Should be equal to {@code max} when the transfer was a success.
	 * @param max
	 * 		Max length of transfer. Can be negative for unknown transfer content length.
	 */
	void end(int current, int max);
}
