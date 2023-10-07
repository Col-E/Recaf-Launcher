package software.coley.recaf.launch.util;

import java.io.IOException;

/**
 * Function that throws an {@link IOException}.
 *
 * @param <T>
 * 		Input type.
 * @param <R>
 * 		Return type.
 */
public interface IOFunction<T, R> {
	/**
	 * @param value
	 * 		Input value.
	 *
	 * @return Return value.
	 *
	 * @throws IOException
	 * 		When the function mapping fails.
	 */
	R apply(T value) throws IOException;
}
