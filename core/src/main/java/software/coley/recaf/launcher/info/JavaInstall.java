package software.coley.recaf.launcher.info;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Model of a Java installation.
 */
public class JavaInstall {
	/**
	 * Compare installs by path.
	 */
	public static Comparator<JavaInstall> COMPARE_PATHS = Comparator.comparing(o -> o.javaExecutable);
	/**
	 * Compare installs by version <i>(newest first)</i>.
	 */
	public static Comparator<JavaInstall> COMPARE_VERSIONS = (o1, o2) -> {
		// Negated so newer versions are sorted to be first
		int cmp = -Integer.compare(o1.version, o2.version);
		if (cmp == 0)
			return COMPARE_PATHS.compare(o1, o2);
		return cmp;
	};
	private final Path javaExecutable;
	private final int version;

	/**
	 * @param javaExecutable
	 * 		Path to the Java executable.
	 * @param version
	 * 		Major version of the installation.
	 */
	public JavaInstall(@Nonnull Path javaExecutable, int version) {
		this.javaExecutable = javaExecutable;
		this.version = version;
	}

	/**
	 * @return Path to the Java executable.
	 */
	@Nonnull
	public Path getJavaExecutable() {
		return javaExecutable;
	}

	/**
	 * @return Major version of the installation.
	 */
	public int getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JavaInstall that = (JavaInstall) o;

		if (version != that.version) return false;
		return javaExecutable.equals(that.javaExecutable);
	}

	@Override
	public int hashCode() {
		int result = javaExecutable.hashCode();
		result = 31 * result + version;
		return result;
	}

	@Override
	public String toString() {
		return "JavaInstall{" +
				"path=" + javaExecutable +
				", version=" + version +
				'}';
	}

}
