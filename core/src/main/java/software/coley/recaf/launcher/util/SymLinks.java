package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Symbolic link utils
 */
public class SymLinks {
	private static final int MAX_LINK_DEPTH = 10;

	/**
	 * @param path
	 * 		Symbolic link to follow.
	 *
	 * @return Target path, or {@code null} if the path could not be resolved.
	 */
	@Nullable
	public static Path resolveSymLink(@Nonnull Path path) {
		try {
			int linkDepth = 0;
			while (Files.isSymbolicLink(path)) {
				if (linkDepth > MAX_LINK_DEPTH)
					throw new IOException("Sym-link path too deep");
				path = Files.readSymbolicLink(path);
				linkDepth++;
			}
			return path;
		} catch (IOException ex) {
			return null;
		}
	}
}
