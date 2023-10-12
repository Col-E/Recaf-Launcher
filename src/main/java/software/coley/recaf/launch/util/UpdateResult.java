package software.coley.recaf.launch.util;

/**
 * Result type.
 */
public enum UpdateResult {
	UP_TO_DATE,
	FAILED_TO_FETCH,
	FAILED_TO_WRITE,
	FAILED_NO_CANDIDATES;

	/**
	 * @return {@code true} when the update was a success.
	 */
	public boolean isSuccess() {
		return this == UP_TO_DATE;
	}
}
