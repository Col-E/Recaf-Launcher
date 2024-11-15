package software.coley.recaf.launcher.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * String-oriented utilities.
 */
public class StringUtil {
	/**
	 * See: <a href="https://stackoverflow.com/a/3758880">This stackoverflow post</a>
	 *
	 * @param bytes
	 * 		Byte count.
	 *
	 * @return Human legible byte count.
	 */
	public static String humanReadableByteCountSI(long bytes) {
		if (-1000 < bytes && bytes < 1000)
			return bytes + " B";
		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}
}
