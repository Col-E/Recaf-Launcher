package software.coley.recaf.launcher.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.task.JavaFxTasks;

import java.util.concurrent.Callable;

/**
 * Command for updating JavaFX.
 */
@Command(name = "update-jfx", description = "Updates JavaFX")
public class UpdateJavaFX implements Callable<JavaFxVersion> {
	@Option(names = {"-c", "--clear"}, description = "Clear the dependency cache")
	private boolean clear;
	@Option(names = {"-maxc", "--maxCacheCount"}, description = "Clear the dependency cache when this many files occupy it")
	private int maxCacheCount = Integer.MAX_VALUE;
	@Option(names = {"-maxs", "--maxCacheSize"}, description = "Clear the dependency cache when this many bytes occupy it")
	private long maxCacheSize = Integer.MAX_VALUE;
	@Option(names = {"-k", "--keepLatest"}, description = "Keep latest cached dependency in the cache when clearing")
	private boolean keepLatest;
	@Option(names = {"-f", "--force"}, description = "Force re-downloading even if the local install looks up-to-date")
	private boolean force;
	@Option(names = {"-v", "--version"}, description = "Target JavaFX version to use, instead of whatever is the latest")
	private int version;

	@Override
	public JavaFxVersion call() {
		JavaFxTasks.checkClearCache(clear, keepLatest, maxCacheCount, maxCacheSize);
		return JavaFxTasks.update(version, force);
	}
}