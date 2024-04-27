package software.coley.recaf.launch.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launch.info.JavaFxPlatform;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.info.RecafVersion;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.StreamGobbler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Command for checking the currently installed version of Recaf.
 */
@Command(name = "run", description = "Runs the installed version of Recaf")
public class Run implements Callable<Run.RunResult> {
	private static final Logger logger = LoggerFactory.getLogger(Run.class);

	private static final String MAIN_CLASS = "software.coley.recaf.Main";
	@Option(names = {"-j", "--java"}, description = {
			"Path of Java executable to use when running Recaf.",
			"Not specifying a value will use the same Java executable used by the launcher."
	})
	private File javaExecutable;

	@Override
	public Run.RunResult call() {
		String javaExecutablePath = javaExecutable == null ? null : javaExecutable.getAbsolutePath();
		return run(true, javaExecutablePath);
	}

	/**
	 * @param inheritIO
	 *        {@code true} to pipe the started process's output into this one.
	 * @param javaExecutablePath
	 * 		Path to use for invoking Java.
	 * 		Use {@code null} to automatically match the current runtime's version.
	 */
	public static Run.RunResult run(boolean inheritIO, String javaExecutablePath) {
		Path recafDirectory = CommonPaths.getRecafDirectory();
		logger.debug("Looking in '{}' for Recaf/dependencies...", recafDirectory);

		RecafVersion installedVersion = RecafVersion.getInstalledVersion();
		if (installedVersion == null) {
			logger.error("No local version of Recaf found.\n" +
					"- Try running with 'update'");
			return RunResult.ERR_NOT_INSTALLED;
		}

		JavaFxPlatform javaFxPlatform = JavaFxPlatform.detect();
		JavaFxVersion javaFxVersion;
		int javaFxRuntimeVersion = JavaFxVersion.getRuntimeVersion();
		if (javaFxRuntimeVersion > 0) {
			javaFxVersion = new JavaFxVersion(javaFxRuntimeVersion);
		} else {
			javaFxVersion = JavaFxVersion.getLocalVersion();
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
			if (javaFxRuntimeVersion > 0) {
				// If the current runtime has JavaFX we don't need to include the dependencies dir.
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
					// TODO: cases
				}
			}
		} catch (InterruptedException ignored) {
			// Expected
		} catch (Throwable t) {
			logger.error("Failed to launch Recaf", t);
			return RunResult.ERR_LAUNCH;
		}

		return RunResult.SUCCESS;
	}

	public enum RunResult {
		SUCCESS,
		ERR_NOT_INSTALLED,
		ERR_NO_JFX,
		ERR_LAUNCH;

		public boolean isSuccess() {
			return this == SUCCESS;
		}
	}
}
