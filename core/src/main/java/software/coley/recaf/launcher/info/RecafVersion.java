package software.coley.recaf.launcher.info;

import java.util.Objects;

/**
 * Recaf version model.
 */
public class RecafVersion implements Version {
	private final String version;
	private final int revision;

	/**
	 * @param version
	 * 		Version string.
	 * @param revision
	 * 		Git revision number. Negative if unknown.
	 */
	public RecafVersion(String version, int revision) {
		this.version = Objects.requireNonNull(version, "Version string cannot be null");
		this.revision = Math.max(-1, revision);
	}

	/**
	 * @return Git revision number. Negative if unknown.
	 */
	public int getRevision() {
		return revision;
	}

	@Override
	public RecafVersion withoutSnapshot() {
		String subversion = version.substring(0, version.length() - SNAPSHOT_SUFFIX.length());
		return new RecafVersion(subversion, revision);
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public int compareTo(Version other) {
		int cmp = Version.super.compareTo(other);
		if (cmp == 0 && other instanceof RecafVersion) {
			// If the versions are the same, check the rev.
			// If one rev is known, it will always be preferred over the other.
			RecafVersion otherRecafVersion = (RecafVersion) other;
			return Integer.compare(revision, otherRecafVersion.revision);
		}
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RecafVersion that = (RecafVersion) o;

		if (revision != that.revision) return false;
		return version.equals(that.version);
	}

	@Override
	public int hashCode() {
		int result = version.hashCode();
		result = 31 * result + revision;
		return result;
	}

	@Override
	public String toString() {
		if (revision > 0)
			return version + " (" + revision + ")";
		return version;
	}
}
