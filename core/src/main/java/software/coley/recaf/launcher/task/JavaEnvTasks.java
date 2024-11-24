package software.coley.recaf.launcher.task;

import software.coley.recaf.launcher.info.JavaInstall;
import software.coley.recaf.launcher.info.JavaVersion;
import software.coley.recaf.launcher.info.PlatformType;
import software.coley.recaf.launcher.util.SymLinks;

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
		} else if (PlatformType.isLinux()) {
			scanForLinuxJavaPaths();
		} else {
			// TODO: Support other platforms
		}
	}

	/**
	 * Detect common Java installations on Linux.
	 */
	private static void scanForLinuxJavaPaths() {
		// Check java alternative link.
		Path altJava = Paths.get("/etc/alternatives/java");
		if (Files.exists(altJava)) {
			addJavaInstall(altJava);
		}

		// Check home
		String homeEnv = System.getenv("JAVA_HOME");
		if (homeEnv != null) {
			Path homePath = Paths.get(homeEnv);
			if (Files.isDirectory(homePath)) {
				Path javaPath = homePath.resolve("bin/java");
				if (Files.exists(javaPath))
					addJavaInstall(javaPath);
			}
		}

		// Check common install locations.
		String[] javaRoots = {
				"/usr/lib/jvm/",
				System.getenv("HOME") + "/.jdks/"
		};
		for (String root : javaRoots) {
			Path rootPath = Paths.get(root);
			if (Files.isDirectory(rootPath)) {
				try (Stream<Path> subDirStream = Files.list(rootPath)) {
					subDirStream.filter(subDir -> Files.exists(subDir.resolve("bin/java")))
							.forEach(subDir -> {
								Path javaPath = subDir.resolve("bin/java");
								if (Files.exists(javaPath))
									addJavaInstall(javaPath);
							});
				} catch (IOException ignored) {
					// Skip
				}
			}
		}
	}

	/**
	 * Detect common Java installations on Windows.
	 */
	private static void scanForWindowsJavaPaths() {
		String homeProp = System.getProperty("java.home");
		if (homeProp != null)
			addJavaInstall(Paths.get(homeProp).resolve("bin/java.exe"));

		// Check home
		String homeEnv = System.getenv("JAVA_HOME");
		if (homeEnv != null) {
			Path homePath = Paths.get(homeEnv);
			if (Files.isDirectory(homePath))
				addJavaInstall(homePath.resolve("bin/java.exe"));
		}

		// Check system path for java entries.
		String path = System.getenv("PATH");
		if (path != null) {
			String[] entries = path.split(";");
			for (String entry : entries)
				if (entry.endsWith("bin"))
					addJavaInstall(Paths.get(entry).resolve("java.exe"));
		}

		// Check common install locations.
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
							.forEach(subDir -> addJavaInstall(subDir.resolve("bin/java.exe")));
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
	@Nonnull
	public static AdditionResult addJavaInstall(@Nonnull Path javaExecutable) {
		// Resolve sym-links
		if (Files.isSymbolicLink(javaExecutable)) {
			javaExecutable = SymLinks.resolveSymLink(javaExecutable);
			if (javaExecutable == null)
				return AdditionResult.ERR_RESOLVE_SYM_LINK;
		}

		// Validate executable is 'java' or 'javaw'
		String execName = javaExecutable.getFileName().toString();
		if (!execName.endsWith("java") && !javaExecutable.endsWith("java.exe")
				&& !execName.endsWith("javaw") && !javaExecutable.endsWith("javaw.exe"))
			return AdditionResult.ERR_NOT_JAVA_EXEC;

		// Validate the given path points to a file that exists
		if (!Files.exists(javaExecutable))
			return AdditionResult.ERR_NOT_JAVA_EXEC;

		// Validate path structure
		Path binDir = javaExecutable.getParent();
		if (binDir == null)
			return AdditionResult.ERR_PARENT;
		Path jdkDir = binDir.getParent();
		if (jdkDir == null)
			return AdditionResult.ERR_PARENT;

		// Validate it's a JDK and not a JRE
		if (Files.notExists(binDir.resolve("javac")) && Files.notExists(binDir.resolve("javac.exe")))
			return AdditionResult.ERR_JRE_NOT_JDK;

		// Validate version
		String jdkDirName = jdkDir.getFileName().toString();
		int version = JavaVersion.fromVersionString(jdkDirName);
		if (version == JavaVersion.UNKNOWN_VERSION)
			return AdditionResult.ERR_UNRESOLVED_VERSION;
		if (version > 8) {
			addJavaInstall(new JavaInstall(javaExecutable, version));
			return AdditionResult.SUCCESS;
		}
		return AdditionResult.ERR_TOO_OLD;
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

	public enum AdditionResult {
		SUCCESS,
		ERR_NOT_JAVA_EXEC,
		ERR_RESOLVE_SYM_LINK,
		ERR_PARENT,
		ERR_JRE_NOT_JDK,
		ERR_UNRESOLVED_VERSION,
		ERR_TOO_OLD;

		public boolean wasSuccess() {
			return this == SUCCESS;
		}

		@Nonnull
		public String message() {
			switch (this) {
				case SUCCESS:
					return "";
				case ERR_RESOLVE_SYM_LINK:
					return "The selected symbolic-link could not be resolved";
				case ERR_NOT_JAVA_EXEC:
					return "The selected file was not 'java' or 'javaw'";
				case ERR_UNRESOLVED_VERSION:
					return "The selected java executable could not have its version resolved";
				case ERR_PARENT:
					return "The selected java executable could not have its parent directories";
				case ERR_JRE_NOT_JDK:
					return "The selected java executable belongs to a JRE and not a JDK";
				case ERR_TOO_OLD:
					return "The selected java executable is from a outdated/unsupported version of Java";
			}
			return "The selected executable was not valid: " + name();
		}
	}
}