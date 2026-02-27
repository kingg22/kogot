package io.github.kingg22.jextract.gradle.internal

import java.io.File

/**
 * Represents the OS + architecture combination for jextract binary selection.
 */
internal enum class JextractPlatform(val id: String) {
    LINUX_X64("linux-x64"),
    LINUX_ARM64("linux-aarch64"),
    MAC_X64("macos-x64"),
    MAC_ARM64("macos-aarch64"),
    WINDOWS_X64("windows-x64"),
    ;

    val isWindows: Boolean get() = this == WINDOWS_X64
    val isMac: Boolean get() = this == MAC_X64 || this == MAC_ARM64

    companion object {
        /**
         * Detects the current platform from system properties.
         * This is safe to call at configuration time because it only reads
         * JVM system properties, not project state.
         */
        val current: JextractPlatform by lazy(LazyThreadSafetyMode.NONE) {
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            when {
                os.contains("linux") && (arch == "aarch64" || arch == "arm64") -> LINUX_ARM64
                os.contains("linux") -> LINUX_X64
                os.contains("mac") && (arch == "aarch64" || arch == "arm64") -> MAC_ARM64
                os.contains("mac") -> MAC_X64
                os.contains("windows") -> WINDOWS_X64
                else -> error("Unsupported platform: os=$os arch=$arch")
            }
        }

        @JvmStatic
        fun resolveJextractBinary(home: File): File = if (current.isWindows) {
            home.resolve("bin/jextract.bat").takeIf { it.exists() } ?: home.resolve("bin/jextract.exe")
        } else {
            home.resolve("bin/jextract")
        }
    }
}
