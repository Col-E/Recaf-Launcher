package software.coley.recaf.launch.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Common stream utils.
 */
public class Stream {
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
	public static void transfer(int bufferSize, InputStream input, ByteArrayOutputStream output) throws IOException {
		int read;
		byte[] data = new byte[Math.max(bufferSize, 2048)];
		while ((read = input.read(data, 0, data.length)) != -1) {
			output.write(data, 0, read);
		}
	}
}
