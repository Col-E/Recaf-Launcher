package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Consumer that throws an {@link IOException}.
 *
 * @param <T>
 * 		Input type.
 */
public interface IOConsumer<T> {
	/**
	 * @param value
	 * 		Input value.
	 *
	 * @throws IOException
	 * 		When the consumer handling fails.
	 */
	void accept(@Nonnull T value) throws IOException;
}
