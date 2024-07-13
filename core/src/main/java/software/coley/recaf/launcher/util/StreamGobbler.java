package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * Used to circumvent locking with {@link ProcessBuilder#inheritIO()}.
 */
public class StreamGobbler implements Runnable {
	private final InputStream input;
	private final Consumer<String> lineConsumer;

	/**
	 * @param input
	 * 		A process IO stream like {@link Process#getOutputStream()} or {@link Process#getErrorStream()}.
	 * @param lineConsumer
	 * 		Consumer to handle text lines coming from the process.
	 */
	public StreamGobbler(@Nonnull InputStream input, @Nonnull Consumer<String> lineConsumer) {
		this.input = input;
		this.lineConsumer = lineConsumer;
	}

	@Override
	public void run() {
		new BufferedReader(new InputStreamReader(input))
				.lines()
				.forEach(lineConsumer);
	}
}