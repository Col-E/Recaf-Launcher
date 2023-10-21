package software.coley.recaf.launch.util;

import dev.dirs.BaseDirectories;
import software.coley.recaf.launch.info.PlatformType;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Key directories for Recaf.
 */
public class CommonPaths {
	/**
	 * @return Recaf root directory.
	 */
	public static Path getRecafDirectory() {
		// Try environment variable first.
		String recafDir = System.getenv("RECAF");
		if (recafDir != null) {
			return Paths.get(recafDir);
		}

		// Use generic data/config location.
		try {
			// Windows: %APPDATA%/
			// Mac:     $HOME/Library/Application Support/
			// Linux:   $XDG_CONFIG_HOME/   or   $HOME/.config
			String dir = BaseDirectories.get().configDir;
			if (dir == null)
				throw new IllegalStateException("BaseDirectories did not yield an initial directory");
			return Paths.get(dir).resolve("Recaf");
		} catch (Throwable t) {
			// The lookup only seems to fail on windows.
			// And we can look up the APPDATA folder easily.
			if (PlatformType.get() == PlatformType.WINDOWS) {
				return Paths.get(System.getenv("APPDATA"), "Recaf");
			} else {
				throw new IllegalStateException("Failed to get Recaf directory", t);
			}
		}
	}

	/**
	 * @return Path to Recaf's dependencies directory.
	 */
	public static Path getDependenciesDir() {
		return getRecafDirectory().resolve("dependencies");
	}

	/**
	 * @return Path to the Recaf launcher's directory for additional resource/config storage.
	 */
	public static Path getLauncherDir() {
		return getRecafDirectory().resolve("launcher");
	}

	/**
	 * @return Path to Recaf jar.
	 */
	public static Path getRecafJar() {
		return getRecafDirectory().resolve("recaf.jar");
	}

	/**
	 * @return Path to launcher config.
	 */
	public static Path getLauncherConfigFile() {
		return getLauncherDir().resolve("config.json");
	}
}
