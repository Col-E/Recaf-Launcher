package software.coley.recaf.launcher.task;

import software.coley.recaf.launcher.info.Version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Outline of an update task.
 */
public class VersionUpdateResult {
	private final Version from;
	private final Version to;
	private final VersionUpdateStatusType type;
	private Throwable error;

	/**
	 * @param from
	 * 		Existing version prior to update, or {@code null} if no version was installed locally.
	 * @param to
	 * 		Target version of update, or {@code null} if no version to update to could be found.
	 * @param type
	 * 		Version update status type.
	 */
	public VersionUpdateResult(@Nullable Version from, @Nullable Version to, @Nonnull VersionUpdateStatusType type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	/**
	 * @param error Error to attach.
	 * @return Self.
	 */
	public VersionUpdateResult withError(@Nullable Throwable error) {
		this.error =  error;
		return this;
	}

	/**
	 * @return Existing version prior to update, or {@code null} if no version was installed locally.
	 */
	@Nullable
	public Version getFrom() {
		return from;
	}

	/**
	 * @return Target version of update.
	 */
	@Nullable
	public Version getTo() {
		return to;
	}

	/**
	 * @return Version update status type.
	 */
	@Nonnull
	public VersionUpdateStatusType getType() {
		return type;
	}

	/**
	 * @return Error associated with the unsuccessful update. Can be {@code null} when the update is a success.
	 */
	@Nullable
	public Throwable getError() {
		return error;
	}

	/**
	 * @return {@code true} when the update was a success.
	 */
	public boolean isSuccess() {
		return type == VersionUpdateStatusType.UP_TO_DATE || type == VersionUpdateStatusType.UPDATE_TO_NEW;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		VersionUpdateResult that = (VersionUpdateResult) o;

		if (!Objects.equals(from, that.from)) return false;
		if (!Objects.equals(to, that.to)) return false;
		return type == that.type;
	}

	@Override
	public int hashCode() {
		int result = from != null ? from.hashCode() : 0;
		result = 31 * result + (to != null ? to.hashCode() : 0);
		result = 31 * result + type.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "VersionUpdateResult{" +
				"from=" + from +
				", to=" + to +
				", type=" + type +
				'}';
	}
}
