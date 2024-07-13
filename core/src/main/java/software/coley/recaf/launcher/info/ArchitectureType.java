package software.coley.recaf.launcher.info;

/**
 * Architecture enumeration for JavaFX.
 *
 * @see JavaFxPlatform
 */
public enum ArchitectureType {
	X86_32,
	X86_64, // linux, mac, mac-monocle, win, win-monocle
	AARCH64, // linux-aarch64, linux-aarch64-monocle, mac-aarch64, mac-aarch64-monocle
	ARM32, // linux-arm32
	ARM64,
	UNKNOWN
}
