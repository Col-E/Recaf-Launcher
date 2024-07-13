package software.coley.recaf.launcher.task.error;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exception outlining Recaf update failure cases.
 */
public class InvalidInstallationException extends Exception {
	public static final int FILE_DOES_NOT_EXIST = 0;
	public static final int MISSING_BUILD_INFO = 1;
	public static final int INVALID_BUILD_INFO_ENTRY = 2;
	public static final int INVALID_BUILD_INFO_MODEL = 3;
	private final int code;

	/**
	 * @param code
	 * 		Failure case.
	 * @param message
	 * 		Detail message.
	 */
	public InvalidInstallationException(int code, @Nonnull String message) {
		this(code, message, null);
	}

	/**
	 * @param code
	 * 		Failure case.
	 * @param message
	 * 		Detail message.
	 * @param cause
	 * 		Cause of the installation failure.
	 */
	public InvalidInstallationException(int code, @Nonnull String message, @Nullable Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * @return Failure case.
	 */
	public int getCode() {
		return code;
	}
}
