package software.coley.recaf.launch.info;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.launch.util.CommonPaths;
import software.coley.recaf.launch.util.Stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Wrapper for Recaf version information.
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
	 * Get the current installed version of Recaf.
	 *
	 * @param log
	 *        {@code true} to log failure cases.
	 *
	 * @return Version string of Recaf, or {@code null} if not known/installed.
	 */
	public static RecafVersion getInstalledVersion(boolean log) {
		Path recafJar = CommonPaths.getRecafJar();

		// Check if it exists.
		if (!Files.exists(recafJar)) {
			if (log) System.err.println("Recaf jar file not found: " + recafJar);
			return null;
		}

		// Extract the build config class data.
		byte[] buildConfigBytes;
		try (ZipFile zip = new ZipFile(recafJar.toFile())) {
			ZipEntry entry = zip.getEntry("software/coley/recaf/RecafBuildConfig.class");
			if (entry == null) {
				if (log)
					System.err.println("Recaf build config is not present in the jar: " + recafJar);
				return null;
			}
			InputStream input = zip.getInputStream(entry);

			// Read contents of the class
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			Stream.transfer(2048, input, output);
			buildConfigBytes = output.toByteArray();
		} catch (IOException ex) {
			if (log) {
				System.err.println("Could not extract build config from the installed Recaf jar");
				ex.printStackTrace();
			}
			return null;
		}

		try {
			// Hack to ensure no matter what the class file version is, ASM will read it.
			// This 'down-samples' it to Java 8.
			buildConfigBytes[7] = 52;

			// Extract field values.
			Map<String, String> fields = new HashMap<>();
			ClassReader reader = new ClassReader(buildConfigBytes);
			reader.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
					if (value != null)
						fields.put(name, value.toString());
					return null;
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

			// Convert field values into version wrapper.
			String version = fields.get("VERSION");
			String gitRevision = fields.get("GIT_REVISION");
			int revision = gitRevision.matches("\\d+") ? Integer.parseInt(gitRevision) : -1;
			return new RecafVersion(version, revision);
		} catch (Throwable t) {
			if (log) {
				System.err.println("An error occurred parsing the Recaf build config");
				t.printStackTrace();
			}
			return null;
		}
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
			return revision + " (" + revision + ")";
		return version;
	}
}
