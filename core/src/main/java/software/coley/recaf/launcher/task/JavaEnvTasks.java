package software.coley.recaf.launcher.task;

import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.info.PlatformType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Tasks for Java environments.
 */
public class JavaEnvTasks {
	private static final Set<JavaInstall> javaInstalls = new HashSet<>();

	/**
	 * Must call {@link #scanForJavaInstalls()} before this list will be populated.
	 *
	 * @return Set of discovered Java installations.
	 */
	@Nonnull
	public static Collection<JavaInstall> getJavaInstalls() {
		return javaInstalls;
	}

	/**
	 * Detect common Java installations for the current platform.
	 */
	public static void scanForJavaInstalls() {
		if (PlatformType.isWindows()) {
			scanForWindowsJavaPaths();
		} else {
			// TODO: Support other platforms
		}
	}

	/**
	 * Detect common Java installations on Windows.
	 */
	private static void scanForWindowsJavaPaths() {
		String homeProp = System.getProperty("java.home");
		if (homeProp != null) {
			addJavaInstall(Paths.get(homeProp).resolve("bin/java.exe"));
		}

		String homeEnv = System.getenv("JAVA_HOME");
		if (homeEnv != null) {
			Path homePath = Paths.get(homeEnv);
			if (Files.isDirectory(homePath)) {
				String dirName = homePath.getFileName().toString();
				int version = JavaVersion.fromVersionString(dirName);
				if (version >= 8) {
					Path javaPath = homePath.resolve("bin/java.exe");
					addJavaInstall(new JavaInstall(javaPath, version));
				}
			}
		}

		String path = System.getenv("PATH");
		if (path != null) {
			String[] entries = path.split(";");
			for (String entry : entries) {
				if (entry.endsWith("bin")) {
					Path javaPath = Paths.get(entry).resolve("java.exe");
					addJavaInstall(javaPath);
				}
			}
		}
		
		String[] javaRoots = {
				"C:/Program Files/Amazon Corretto/",
				"C:/Program Files/Eclipse Adoptium/",
				"C:/Program Files/BellSoft/",
				"C:/Program Files/Java/",
				"C:/Program Files/Microsoft/",
				"C:/Program Files/SapMachine/JDK/",
				"C:/Program Files/Zulu/",
		};
		for (String root : javaRoots) {
			Path rootPath = Paths.get(root);
			if (Files.isDirectory(rootPath)) {
				try (Stream<Path> subDirStream = Files.list(rootPath)) {
					subDirStream.filter(subDir -> Files.exists(subDir.resolve("bin/java.exe")))
							.forEach(subDir -> {
								String dirName = subDir.getFileName().toString();
								int version = JavaVersion.fromVersionString(dirName);
								if (version >= 8) {
									Path javaPath = subDir.resolve("bin/java.exe");
									addJavaInstall(new JavaInstall(javaPath, version));
								}
							});
				} catch (IOException ignored) {
					// Skip
				}
			}
		}
	}

	/**
	 * @param javaExecutable
	 * 		Path to executable to add.
	 *
	 * @return {@code true} when the path was recognized as a valid executable.
	 * {@code false} when discarded.
	 */
	public static boolean addJavaInstall(@Nonnull Path javaExecutable) {
		Path binDir = javaExecutable.getParent();
		if (binDir == null)
			return false;
		Path jdkDir = binDir.getParent();
		if (jdkDir == null)
			return false;
		String jdkDirName = jdkDir.getFileName().toString();
		int version = JavaVersion.fromVersionString(jdkDirName);
		if (version > 8) {
			addJavaInstall(new JavaInstall(javaExecutable, version));
			return true;
		}
		return false;
	}

	/**
	 * @param path
	 * 		Path to executable to look up.
	 *
	 * @return Install entry for path, or {@code null} if not previously recorded as a valid installation.
	 */
	@Nullable
	public static JavaInstall getByPath(@Nonnull Path path) {
		return javaInstalls.stream()
				.filter(i -> i.getJavaExecutable().equals(path))
				.findFirst().orElse(null);
	}

	private static void addJavaInstall(@Nonnull JavaInstall install) {
		javaInstalls.add(install);
	}
}