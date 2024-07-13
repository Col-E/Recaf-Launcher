package software.coley.recaf.launcher.task;

/**
 * Result type.
 */
public enum VersionUpdateStatusType {
	UPDATE_TO_NEW,
	UP_TO_DATE,
	FAILED_TO_FETCH,
	FAILED_TO_WRITE,
	FAILED_NO_CANDIDATES;
}
