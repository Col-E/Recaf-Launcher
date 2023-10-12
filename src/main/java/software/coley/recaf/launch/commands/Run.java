package software.coley.recaf.launch.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launch.info.JavaFxPlatform;
import software.coley.recaf.launch.info.JavaFxVersion;
import software.coley.recaf.launch.info.RecafVersion;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.StreamGobbler;

import java.io.File;
import java.io.IOException;
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
public class Run implements Callable<Void> {
	private static final String MAIN_CLASS = "software.coley.recaf.Main";
	@Option(names = {"-j", "--java"}, description = {
			"Path of Java executable to use when running Recaf.",
			"Not specifying a value will use the same Java executable used by the launcher."
	})
	private File javaExecutable;

	@Override
	public Void call() {
		String javaExecutablePath = javaExecutable == null ? null : javaExecutable.getAbsolutePath();
		run(true, javaExecutablePath);
		return null;
	}

	/**
	 * @param inheritIO
	 *        {@code true} to pipe the started process's output into this one.
	 * @param javaExecutablePath
	 * 		Path to use for invoking Java.
	 * 		Use {@code null} to automatically match the current runtime's version.
	 */
	public static void run(boolean inheritIO, String javaExecutablePath) {
		RecafVersion installedVersion = RecafVersion.getInstalledVersion(true);
		if (installedVersion == null) {
			System.err.println("No local version of Recaf found.\n" +
					"- Try running with 'update'");
			return;
		}

		JavaFxPlatform javaFxPlatform = JavaFxPlatform.detect();
		JavaFxVersion javaFxVersion = UpdateJavaFX.getLocalVersion(true);
		if (javaFxVersion == null) {
			System.err.println("No local cached version of JavaFX found.\n" +
					"- Try running with 'update-jfx'");
			return;
		}
		try {
			// Build classpath:
			//  - Recaf jar
			//  - JavaFX jars
			Path recafDirectory = CommonPaths.getRecafDirectory();
			List<Path> javafxDependencies = Files.list(CommonPaths.getDependenciesDir())
					.filter(path -> {
						String fileName = path.getFileName().toString();
						return fileName.contains(javaFxVersion.getVersion() + "-" + javaFxPlatform.getClassifier());
					})
					.map(recafDirectory::relativize)
					.collect(Collectors.toList());
			List<Path> classpathItems = new ArrayList<>();
			classpathItems.add(recafDirectory.relativize(CommonPaths.getRecafJar()));
			classpathItems.addAll(javafxDependencies);

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
				recafProcess.waitFor();
			}
		} catch (IOException ex) {
			System.err.println("Failed to launch Recaf");
			ex.printStackTrace();
		} catch (InterruptedException ignored) {
			// Expected
		}
	}
}
