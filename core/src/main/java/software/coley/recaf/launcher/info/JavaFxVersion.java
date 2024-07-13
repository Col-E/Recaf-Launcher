package software.coley.recaf.launcher.info;

/**
 * JavaFX version model.
 */
public class JavaFxVersion implements Version {

	/**
	 * Oldest version we'd suggest using.
	 */
	public static final int MIN_SUGGESTED = 21;

	private final String version;

	public JavaFxVersion(String version) {
		this.version = version;
	}

	public JavaFxVersion(int version) {
		this.version = String.valueOf(version);
	}

	/**
	 * @return Major release version of JavaFX.
	 */
	public int getMajorVersion() {
		if (version.matches("\\d+"))
			return Integer.parseInt(version);
		if (version.contains("."))
			return Integer.parseInt(version.substring(0, version.indexOf('.')));
		if (version.contains("-"))
			return Integer.parseInt(version.substring(0, version.indexOf('-')));
		throw new IllegalStateException("Cannot map JFX version to major version: " + version);
	}

	@Override
	public boolean isSnapshot() {
		return version.contains("-ea+");
	}

	@Override
	public JavaFxVersion withoutSnapshot() {
		String subversion = version.substring(0, version.length() - SNAPSHOT_SUFFIX.length());
		return new JavaFxVersion(subversion);
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JavaFxVersion that = (JavaFxVersion) o;

		return version.equals(that.version);
	}

	@Override
	public int hashCode() {
		return version.hashCode();
	}

	@Override
	public String toString() {
		return version;
	}
}
