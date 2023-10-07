package software.coley.recaf.launch.info;

import java.util.Locale;

/**
 * Architecture enumeration for JavaFX.
 */
public enum ArchitectureType {
	X86_32,
	X86_64, // linux, mac, mac-monocle, win, win-monocle
	AARCH64, // linux-aarch64, linux-aarch64-monocle, mac-aarch64, mac-aarch64-monocle
	ARM32, // linux-arm32
	ARM64,
	UNKNOWN;

	/**
	 * @return Current architecture.
	 */
	public static ArchitectureType get() {
		return get(SystemInformation.OS_ARCH);
	}

	/**
	 * @param architecture
	 * 		Architecture string.
	 *
	 * @return Architecture type.
	 */
	public static ArchitectureType get(String architecture) {
		if (architecture == null)
			return UNKNOWN;
		architecture = architecture.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
		if (architecture.matches("^(x8664|amd64|ia32e|em64t|x64)$"))
			return X86_64;
		if (architecture.matches("^(x8632|x86|i[3-6]86|ia32|x32)$"))
			return X86_32;
		if (architecture.matches("^(arm|arm32)$"))
			return ARM32;
		if ("aarch64".equals(architecture))
			return AARCH64;
		return UNKNOWN;
	}
}
