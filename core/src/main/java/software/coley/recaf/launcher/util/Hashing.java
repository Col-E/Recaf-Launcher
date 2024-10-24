package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashing utils.
 */
public class Hashing {
	/**
	 * @param is
	 * 		Input to hash.
	 *
	 * @return SHA1 hash of input.
	 *
	 * @throws IOException
	 * 		When the input cannot be read.
	 */
	@Nonnull
	@SuppressWarnings("all") // empty while
	public static String sha1(@Nonnull InputStream is) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-1 not recognized as a supported digest instance", ex);
		}
		DigestInputStream dis = new DigestInputStream(is, digest);
		byte[] bytes = new byte[1024];
		while (dis.read(bytes) != -1) ;
		return toHexString(digest.digest());
	}

	/**
	 * Input:
	 * <pre>
	 *   [ 0x1, 0xFF, 0xA ]
	 * </pre>
	 * Output:
	 * <pre>
	 *   "01ff0a"
	 * </pre>
	 *
	 * @param bytes
	 * 		Bytes to convert to a hex string.
	 *
	 * @return Hex string representation of bytes. Chars are lower case.
	 */
	@Nonnull
	private static String toHexString(@Nonnull byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			int value = b & 0xFF;
			if (value <= 0xF)
				sb.append('0');
			sb.append(Integer.toHexString(value));
		}
		return sb.toString();
	}
}
