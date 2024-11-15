package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Consumer that throws an {@link IOException}.
 *
 * @param <T1>
 * 		Input type.
 * @param <T2>
 * 		Input type.
 */
public interface IOBiConsumer<T1, T2> {
	/**
	 * @param value1
	 * 		Input value.
	 * @param value2
	 * 		Input value.
	 *
	 * @throws IOException
	 * 		When the consumer handling fails.
	 */
	void accept(@Nonnull T1 value1, @Nonnull T2 value2) throws IOException;
}
