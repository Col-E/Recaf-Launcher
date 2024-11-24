package software.coley.recaf.launcher.task;

import org.slf4j.Logger;
import software.coley.recaf.launcher.ApplicationLauncher;
import software.coley.recaf.launcher.info.JavaFxPlatform;
import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.info.RecafVersion;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Loggers;
import software.coley.recaf.launcher.util.StreamGobbler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tasks for executing Recaf.
 */
public class ExecutionTasks {
	private static final Logger logger = Loggers.newLogger();
	private static final String MAIN_CLASS = "software.coley.recaf.launcher.ApplicationLauncher";
	// Launcher specific constants
	public static final int ERR_NOT_INSTALLED = 50;
	public static final int ERR_NO_JFX = 51;
	// Error constants shared with Recaf
	public static final int SUCCESS = 0;
	public static final int ERR_FX_UNKNOWN = 100;
	public static final int ERR_FX_CLASS_NOT_FOUND = 101;
	public static final int ERR_FX_NO_SUCH_METHOD = 102;
	public static final int ERR_FX_INVOKE_TARGET = 103;
	public static final int ERR_FX_ACCESS_TARGET = 104;
	public static final int ERR_FX_OLD_VERSION = 105;
	public static final int ERR_FX_UNKNOWN_VERSION = 106;
	public static final int ERR_CDI_INIT_FAILURE = 150;
	public static final int ERR_NOT_A_JDK = 160;
	public static final int INTELLIJ_TERMINATION = 130;

	/**
	 * @param inheritIO
	 *        {@code true} to pipe the started process's output into this one.
	 * @param javaExecutablePath
	 * 		Path to use for invoking Java.
	 * 		Use {@code null} to automatically match the current runtime's version.
	 *
	 * @throws IOException
	 * 		When the process couldn't be launched.
	 */
	@Nonnull
	public static RunResult run(boolean inheritIO, @Nullable String javaExecutablePath) throws IOException {
		Path recafDirectory = CommonPaths.getRecafDirectory();
		logger.debug("Looking in '{}' for Recaf/dependencies...", recafDirectory);

		RecafVersion installedVersion;
		try {
			installedVersion = RecafTasks.getInstalledVersion();
		} catch (InvalidInstallationException e) {
			logger.error("No local version of Recaf found.\n" +
					"- Try running with 'update'");
			return new RunResult(ERR_NOT_INSTALLED);
		}

		JavaFxPlatform javaFxPlatform = JavaFxTasks.detectSystemPlatform();

		// Pull the JavaFX version from our dependency download cache.
		JavaFxVersion javaFxVersion = JavaFxTasks.detectCachedVersion();

		// Ensure a version was found
		if (javaFxVersion == null) {
			logger.error("No local cached version of JavaFX found.\n" +
					"- Try running with 'update-jfx'");
			return new RunResult(ERR_NO_JFX);
		}

		// Ensure a valid version was found
		if (javaFxVersion.getMajorVersion() < JavaFxVersion.MIN_SUGGESTED) {
			logger.error("The cached version of JavaFX was too old ({}).\n" +
					"- Try running with 'update-jfx'", javaFxVersion);
			return new RunResult(ERR_FX_OLD_VERSION);
		}

		logger.info("Using cached version of JavaFX: {}", javaFxVersion);

		// Build classpath:
		//  - Recaf jar
		//  - JavaFX jars
		List<Path> classpathItems = new ArrayList<>();
		classpathItems.add(recafDirectory.relativize(CommonPaths.getRecafJar()));
		{
			Path dependenciesDir = CommonPaths.getDependenciesDir();
			if (!Files.isDirectory(dependenciesDir))
				return new RunResult(ERR_NO_JFX);
			String versionIdentifier = javaFxVersion.getVersion() + "-" + javaFxPlatform.getClassifier();
			List<Path> javafxDependencies;
			try (Stream<Path> pathStream = Files.list(dependenciesDir)) {
				javafxDependencies = pathStream
						.filter(path -> {
							String fileName = path.getFileName().toString();
							return fileName.contains(versionIdentifier);
						})
						.map(recafDirectory::relativize)
						.collect(Collectors.toList());
			}

			// Validate we found:
			// - base
			// - graphics
			// - controls
			// - media
			List<String> expected = new ArrayList<>();
			expected.add("javafx-base");
			expected.add("javafx-graphics");
			expected.add("javafx-controls");
			expected.add("javafx-media");
			for (Path fxDependency : javafxDependencies) {
				String name = fxDependency.getFileName().toString();
				expected.removeIf(name::contains);
			}
			if (!expected.isEmpty()) {
				logger.error("Missing the following JavaFX artifacts: {}", String.join(", ", expected));
				return new RunResult(ERR_NO_JFX);
			}

			// Add to -cp
			classpathItems.addAll(javafxDependencies);
		}

		// Get location of the launch wrapper.
		String classpath;
		try {
			classpath = Paths.get(ApplicationLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
		} catch (URISyntaxException ex) {
			throw new IOException("Error constructing classpath", ex);
		}

		// Resolve the Java executable used by the current JVM.
		if (javaExecutablePath == null)
			javaExecutablePath = Paths.get(System.getProperty("java.home"))
					.resolve("bin")
					.resolve("java")
					.toString();

		logger.info("Running Recaf '{}' with JavaFX '{}:{}'",
				installedVersion.getVersion(), javaFxVersion.getVersion(), javaFxPlatform.getClassifier());

		// Create the process.
		ProcessBuilder builder = new ProcessBuilder(javaExecutablePath, "-cp", classpath, MAIN_CLASS);
		builder.directory(recafDirectory.toFile());
		Process recafProcess = builder.start();

		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		if (inheritIO) {
			// ProcessBuilder.inheritIO() locks the current thread, even after the process dies.
			// So we have this work-around.
			StreamGobbler outputGobbler = new StreamGobbler(recafProcess.getInputStream(), s -> {
				System.out.println(s);
				out.append(s).append('\n');
			});
			StreamGobbler errorGobbler = new StreamGobbler(recafProcess.getErrorStream(), s -> {
				System.err.println(s);
				err.append(s).append('\n');
			});
			new Thread(outputGobbler).start();
			new Thread(errorGobbler).start();
		}

		try {
			// Write classpath entries to the launcher wrapper
			try (DataOutputStream pout = new DataOutputStream(recafProcess.getOutputStream())) {
				for (Path classpathItem : classpathItems)
					pout.writeUTF(classpathItem.toString());
				pout.writeUTF("");
			}

			// Handle non-standard exit codes. Recaf has a few for special cases.
			int exitCode = recafProcess.waitFor();
			switch (exitCode) {
				case ERR_FX_UNKNOWN:
				case ERR_FX_UNKNOWN_VERSION:
					logger.error("Recaf encountered an unknown JavaFX validation error");
					break;
				case ERR_FX_CLASS_NOT_FOUND:
					logger.error("Recaf did not find JavaFX on its classpath");
					break;
				case ERR_FX_NO_SUCH_METHOD:
					logger.error("Recaf found JavaFX on its classpath but couldn't determine what version (missing method)");
					break;
				case ERR_FX_INVOKE_TARGET:
					logger.error("Recaf found JavaFX on its classpath but couldn't determine what version (invoke target failure)");
					break;
				case ERR_FX_ACCESS_TARGET:
					logger.error("Recaf found JavaFX on its classpath but couldn't determine what version (invoke access failure)");
					break;
				case ERR_FX_OLD_VERSION:
					logger.error("Recaf found JavaFX on its classpath but it was an unsupported older version");
					break;
				case ERR_CDI_INIT_FAILURE:
					logger.error("Recaf failed creating the CDI container, try re-downloading the Recaf jar & JavaFX dependencies");
					break;
				case ERR_NOT_A_JDK:
					logger.error("Recaf requires a JDK but was run with a JRE");
					break;
				case 0:
					// Expected after normal closure
					break;
			}
			return new RunResult(exitCode, out, err);
		} catch (InterruptedException ignored) {
			return new RunResult(SUCCESS);
		}
	}

	public static class RunResult {
		private final String out;
		private final String err;
		private final int code;

		public RunResult(int code) {
			this.code = code;
			this.out = "";
			this.err = "";
		}

		public RunResult(int code, @Nonnull StringBuilder out, @Nonnull StringBuilder err) {
			this.code = code;
			this.out = out.toString();
			this.err = err.toString();
		}

		public int getCode() {
			return code;
		}

		@Nonnull
		public String getOut() {
			return out;
		}

		@Nonnull
		public String getErr() {
			return err;
		}

		public boolean isSuccess() {
			return code == SUCCESS;
		}

		@Nonnull
		public String getCodeDescription() {
			switch (code) {
				case ERR_NOT_INSTALLED:
					return "Recaf is not installed";
				case ERR_NO_JFX:
					return "JavaFX is not installed";
				case ERR_FX_UNKNOWN:
					return "An unknown error occurred";
				case ERR_FX_CLASS_NOT_FOUND:
					return "JavaFX has missing classes";
				case ERR_FX_NO_SUCH_METHOD:
					return "JavaFX has unexpected incompatible API changes";
				case ERR_FX_INVOKE_TARGET:
				case ERR_FX_ACCESS_TARGET:
					return "Recaf failed to access JavaFX (Reflection)";
				case ERR_FX_OLD_VERSION:
					return "JavaFX on Recaf's classpath was too old";
				case ERR_FX_UNKNOWN_VERSION:
					return "JavaFX on Recaf's classpath couldn't be identified";
				case ERR_CDI_INIT_FAILURE:
					return "Recaf failed to create its CDI container";
				case ERR_NOT_A_JDK:
					return "Recaf must be run with a JDK, but was run with a JRE";
			}
			return "<unknown error>";
		}
	}
}
