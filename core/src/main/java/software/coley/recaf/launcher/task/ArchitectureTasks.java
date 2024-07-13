package software.coley.recaf.launcher.task;

import software.coley.recaf.launcher.info.ArchitectureType;
import software.coley.recaf.launcher.info.SystemInformation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public class ArchitectureTasks {
	/**
	 * @return Current architecture.
	 */
	@Nonnull
	public static ArchitectureType get() {
		return get(SystemInformation.OS_ARCH);
	}

	/**
	 * @param architecture
	 * 		Architecture string.
	 *
	 * @return Architecture type.
	 */
	@Nonnull
	public static ArchitectureType get(@Nullable String architecture) {
		if (architecture == null)
			return ArchitectureType.UNKNOWN;
		architecture = architecture.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
		if (architecture.matches("^(x8664|amd64|ia32e|em64t|x64)$"))
			return ArchitectureType.X86_64;
		if (architecture.matches("^(x8632|x86|i[3-6]86|ia32|x32)$"))
			return ArchitectureType.X86_32;
		if (architecture.matches("^(arm|arm32)$"))
			return ArchitectureType.ARM32;
		if ("aarch64".equals(architecture))
			return ArchitectureType.AARCH64;
		return ArchitectureType.UNKNOWN;
	}
}
