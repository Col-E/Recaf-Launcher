package software.coley.recaf.launch.info;

/**
 * JavaFX supported platforms.
 */
public enum JavaFxPlatform {
    LINUX("linux",  PlatformType.LINUX, ArchitectureType.X86_64),
    LINUX_AARCH64("linux-aarch64",  PlatformType.LINUX, ArchitectureType.ARM64),
    WINDOWS("win",  PlatformType.WINDOWS, ArchitectureType.X86_64),
    MAC("mac", PlatformType.MAC, ArchitectureType.X86_64),
    MAC_AARCH64("mac-aarch64",PlatformType.MAC, ArchitectureType.ARM64),
    UNSUPPORTED("?",  null, null);

    private final String classifier;
    private final PlatformType platform;
    private final ArchitectureType architecture;

    JavaFxPlatform(String classifier, PlatformType platform, ArchitectureType architecture) {
        this.classifier = classifier;
        this.platform = platform;
        this.architecture = architecture;
    }

    /**
     * @return Maven artifact classifier for this platform.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * @return Associated platform.
     */
    public PlatformType getPlatform() {
        return platform;
    }
    /**
     * @return Associated platform.
     */
    public ArchitectureType getArchitecture() {
        return architecture;
    }

    /**
     * @return Detected supported platform for the current system.
     */
    public static JavaFxPlatform detect() {
        switch (PlatformType.get()) {
            case WINDOWS:
                return WINDOWS;
            case MAC:
                if (ArchitectureType.get() == ArchitectureType.AARCH64)
                    return MAC_AARCH64;
                return MAC;
            case LINUX:
                if (ArchitectureType.get() == ArchitectureType.AARCH64)
                    return LINUX_AARCH64;
                return LINUX;
            default:
                return UNSUPPORTED;
        }
    }
}