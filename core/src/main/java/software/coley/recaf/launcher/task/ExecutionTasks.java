package software.coley.recaf.launcher.task;

import org.slf4j.Logger;
import software.coley.recaf.launcher.info.JavaFxPlatform;
import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.info.RecafVersion;
import software.coley.recaf.launcher.task.error.InvalidInstallationException;
import software.coley.recaf.launcher.util.CommonPaths;
import software.coley.recaf.launcher.util.Loggers;
import software.coley.recaf.launcher.util.StreamGobbler;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tasks for executing Recaf.
 */
public class ExecutionTasks {
	private static final Logger logger = Loggers.newLogger();
	private static final String MAIN_CLASS = "software.coley.recaf.Main";
	// Error constants shared with Recaf
	private static final int ERR_UNKNOWN = 100;
	private static final int ERR_CLASS_NOT_FOUND = 101;
	private static final int ERR_NO_SUCH_METHOD = 102;
	private static final int ERR_INVOKE_TARGET = 103;
	private static final int ERR_ACCESS_TARGET = 104;
	private static final int ERR_OLD_JFX_VERSION = 105;
	private static final int ERR_UNKNOWN_JFX_VERSION = 106;

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
	public static RunResult run(boolean inheritIO, @Nullable String javaExecutablePath) throws IOException {
		Path recafDirectory = CommonPaths.getRecafDirectory();
		logger.debug("Looking in '{}' for Recaf/dependencies...", recafDirectory);

		RecafVersion installedVersion;
		try {
			installedVersion = RecafTasks.getInstalledVersion();
		} catch (InvalidInstallationException e) {
			logger.error("No local version of Recaf found.\n" +
					"- Try running with 'update'");
			return RunResult.ERR_NOT_INSTALLED;
		}

		JavaFxPlatform javaFxPlatform = JavaFxTasks.detectSystemPlatform();
		JavaFxVersion javaFxVersion;
		int javaFxRuntimeVersion = JavaFxTasks.detectClasspathVersion();
		if (javaExecutablePath != null && javaFxRuntimeVersion > 0) {
			// Only use runtime version if we're using the current runtime's executable to launch the process.
			javaFxVersion = new JavaFxVersion(javaFxRuntimeVersion);
		} else {
			javaFxVersion = JavaFxTasks.detectCachedVersion();
		}

		// Ensure a version was chosen.
		if (javaFxVersion == null) {
			logger.error("No local cached version of JavaFX found.\n" +
					"- Try running with 'update-jfx'\n" +
					"- Or use a JDK that bundles JavaFX");
			return RunResult.ERR_NO_JFX;
		}

		try {
			// Build classpath:
			//  - Recaf jar
			//  - JavaFX jars
			List<Path> classpathItems = new ArrayList<>();
			classpathItems.add(recafDirectory.relativize(CommonPaths.getRecafJar()));
			if (javaFxRuntimeVersion < 0) {
				// If the current runtime has JavaFX we don't need to include the dependencies' dir.
				// The runtime should provide JavaFX's classes and native libraries.
				Path dependenciesDir = CommonPaths.getDependenciesDir();
				if (!Files.isDirectory(dependenciesDir)) return RunResult.ERR_NO_JFX;
				List<Path> javafxDependencies = Files.list(dependenciesDir)
						.filter(path -> {
							String fileName = path.getFileName().toString();
							return fileName.contains(javaFxVersion.getVersion() + "-" + javaFxPlatform.getClassifier());
						})
						.map(recafDirectory::relativize)
						.collect(Collectors.toList());
				classpathItems.addAll(javafxDependencies);
			}

			// Build classpath string.
			String classpath = classpathItems.stream()
					.map(Path::toString)
					.collect(Collectors.joining(File.pathSeparator));

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

			if (inheritIO) {
				// ProcessBuilder.inheritIO() locks the current thread, even after the process dies.
				// So we have this work-around.
				StreamGobbler outputGobbler = new StreamGobbler(recafProcess.getInputStream(), System.out::println);
				StreamGobbler errorGobbler = new StreamGobbler(recafProcess.getErrorStream(), System.err::println);
				new Thread(outputGobbler).start();
				new Thread(errorGobbler).start();

				// Handle non-standard exit codes. Recaf has a few for special cases.
				int exitCode = recafProcess.waitFor();
				switch (exitCode) {
					case ERR_UNKNOWN:
					case ERR_UNKNOWN_JFX_VERSION:
						logger.error("Recaf encountered an unknown JavaFX validation error");
						break;
					case ERR_CLASS_NOT_FOUND:
						logger.error("Recaf did not find JavaFX on its classpath");
						break;
					case ERR_NO_SUCH_METHOD:
						logger.error("Recaf found JavaFX on its classpath but couldn't determine what version (missing method)");
						break;
					case ERR_INVOKE_TARGET:
						logger.error("Recaf found JavaFX on its classpath but couldn't determine what version (invoke target failure)");
						break;
					case ERR_ACCESS_TARGET:
						logger.error("Recaf found JavaFX on its classpath but couldn't determine what version (invoke access failure)");
						break;
					case ERR_OLD_JFX_VERSION:
						logger.error("Recaf found JavaFX on its classpath but it was an unsupported older version");
						break;
					case 0:
						// Expected after normal closure
						break;
				}
			}
			return RunResult.SUCCESS;
		} catch (InterruptedException ignored) {
			return RunResult.SUCCESS;
		}
	}

	public enum RunResult {
		SUCCESS,
		ERR_NOT_INSTALLED,
		ERR_NO_JFX;

		public boolean isSuccess() {
			return this == SUCCESS;
		}
	}
}
