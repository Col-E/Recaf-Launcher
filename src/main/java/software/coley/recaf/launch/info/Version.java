package software.coley.recaf.launch.info;

/**
 * Version information outline.
 */
public interface Version extends Comparable<Version> {
	String SNAPSHOT_SUFFIX = "-snapshot";

	/**
	 * @return Self, but modified to not include the snapshot suffix.
	 */
	Version withoutSnapshot();

	/**
	 * @return Version string.
	 */
	String getVersion();

	/**
	 * @return Array of version parts, where the first value is the major release,
	 * and the last is the most insignificant version section.
	 */
	default int[] getVersionGroups() {
		String version = getVersion();

		// The early-access '-ea+' modifier should be cut off.
		int eaIndex = version.indexOf("-ea+");
		if (eaIndex > 0)
			version = version.substring(0, eaIndex);

		// Split by non integer characters, mapping sections to ints.
		String[] split = version.split("[^0-9]+");
		int[] groups = new int[split.length];
		for (int i = 0; i < split.length; i++)
			groups[i] = Integer.parseInt(split[i]);
		return groups;
	}

	/**
	 * @return {@code true} when the version is a snapshot.
	 */
	default boolean isSnapshot() {
		return getVersion().toLowerCase().endsWith(SNAPSHOT_SUFFIX);
	}

	/**
	 * @param other
	 * 		Other version to compare to.
	 * 		If {@code null} then this will be {@code true}.
	 *
	 * @return {@code true} when our version is newer.
	 */
	default boolean isNewer(Version other) {
		if (other == null)
			return true;
		return compareTo(other) > 0;
	}

	/**
	 * @param other
	 * 		Other version to compare to.
	 * 		If {@code null} then this will be {@code false}.
	 *
	 * @return {@code true} when our version is older.
	 */
	default boolean isOlder(Version other) {
		if (other == null)
			return false;
		return compareTo(other) < 0;
	}

	@Override
	default int compareTo(Version other) {
		// Compare semantic versions, greatest significant portion first.
		int[] versionGroups = getVersionGroups();
		int[] otherVersionGroups = other.getVersionGroups();
		int max = Math.min(versionGroups.length, otherVersionGroups.length);
		for (int i = 0; i < max; i++) {
			int cmp = Integer.compare(versionGroups[i], otherVersionGroups[i]);
			if (cmp != 0)
				return cmp;

			// Check for cases like '4.0.0' vs '4.0.0-SNAPSHOT' where the non-snapshots are preferred.
			// Generally, snapshots of a version come out before the stable version.
			if (isSnapshot() && !other.isSnapshot())
				return -1;
			else if (!isSnapshot() && other.isSnapshot())
				return 1;
		}

		return 0;
	}
}
