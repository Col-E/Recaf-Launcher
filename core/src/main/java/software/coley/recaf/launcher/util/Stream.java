package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Common stream utils.
 */
public class Stream {
	private static final int MAX_BUFFER_SIZE = 65536;

	/**
	 * @param bufferSize
	 * 		Buffer size.
	 *
	 * @return Array buffer of the given size, capped to {@link #MAX_BUFFER_SIZE}.
	 */
	private static byte[] newBuffer(int bufferSize) {
		return new byte[Math.max(bufferSize, MAX_BUFFER_SIZE)];
	}

	/**
	 * @param bufferSize
	 * 		Buffer size.
	 * @param input
	 * 		Stream to copy from.
	 * @param output
	 * 		Stream to feed into.
	 *
	 * @throws IOException
	 * 		When the streams cannot be read or written to.
	 */
	public static void transfer(int bufferSize, @Nonnull InputStream input, @Nonnull OutputStream output) throws IOException {
		transfer(bufferSize, input, output, -1, null);
	}

	/**
	 * @param bufferSize
	 * 		Buffer size.
	 * @param input
	 * 		Stream to copy from.
	 * @param output
	 * 		Stream to feed into.
	 * @param max
	 * 		Expected max length of input content.
	 * @param listener
	 * 		Optional listener for transfer progress notifications.
	 *
	 * @throws IOException
	 * 		When the streams cannot be read or written to.
	 */
	public static void transfer(int bufferSize, @Nonnull InputStream input, @Nonnull OutputStream output,
	                            int max, @Nullable TransferListener listener) throws IOException {
		if (listener != null) listener.start(max);
		int read;
		int written = 0;
		byte[] data = newBuffer(bufferSize);
		while ((read = input.read(data)) != -1) {
			output.write(data, 0, read);
			written += read;
			if (listener != null) listener.progress(written, max);
		}
		if (listener != null) listener.end(written, max);
	}
}
