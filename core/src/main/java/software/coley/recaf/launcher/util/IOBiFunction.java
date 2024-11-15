package software.coley.recaf.launcher.util;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Function that throws an {@link IOException}.
 *
 * @param <T1>
 * 		Input type.
 * @param <T2>
 * 		Input type.
 * @param <R>
 * 		Return type.
 */
public interface IOBiFunction<T1, T2, R> {
	/**
	 * @param value1
	 * 		Input value.
	 * @param value2
	 * 		Input value.
	 *
	 * @return Return value. Should never be {@code null}.
	 *
	 * @throws IOException
	 * 		When the function mapping fails.
	 */
	@Nonnull
	R apply(@Nonnull T1 value1, @Nonnull T2 value2) throws IOException;
}
