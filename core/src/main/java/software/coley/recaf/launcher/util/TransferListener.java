package software.coley.recaf.launcher.util;

/**
 * Listener for IO transfer operations.
 *
 * @see Stream
 * @see Web
 */
public interface TransferListener {
	/**
	 * @param max
	 * 		Max length of transfer. Can be negative for unknown transfer content length.
	 */
	void start(int max);

	/**
	 * @param current
	 * 		Current amount of bytes transferred.
	 * @param max
	 * 		Max length of transfer. Can be negative for unknown transfer content length.
	 */
	void progress(int current, int max);

	/**
	 * @param current
	 * 		Current amount of bytes transferred. Should be equal to {@code max} when the transfer was a success.
	 * @param max
	 * 		Max length of transfer. Can be negative for unknown transfer content length.
	 */
	void end(int current, int max);
}
