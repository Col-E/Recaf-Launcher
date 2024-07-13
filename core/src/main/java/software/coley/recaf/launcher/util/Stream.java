package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
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
	 * @param input
	 * 		Stream to copy from.
	 * @param output
	 * 		Stream to feed into.
	 *
	 * @throws IOException
	 * 		When the streams cannot be read or written to.
	 */
	public static void transfer(int bufferSize, @Nonnull InputStream input, @Nonnull OutputStream output) throws IOException {
		int read;
		byte[] data = new byte[Math.max(bufferSize, MAX_BUFFER_SIZE)];
		while ((read = input.read(data, 0, data.length)) != -1) {
			output.write(data, 0, read);
		}
	}
}
