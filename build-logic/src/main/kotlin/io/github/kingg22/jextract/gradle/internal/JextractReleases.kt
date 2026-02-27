package io.github.kingg22.jextract.gradle.internal

import io.github.kingg22.jextract.gradle.extension.JextractExtension.JextractVersion

/**
 * Known jextract releases with their download URLs and SHA-256 checksums.
 *
 * URL pattern:
 * `https://download.java.net/java/early_access/jextract/{javaVer}/{build}/openjdk-{javaVer}-jextract+{build}-{patch}_{platform}_bin.tar.gz`
 */
internal class JextractRelease private constructor(val artifacts: Map<JextractPlatform, PlatformArtifact>) {
    init {
        check(artifacts.isNotEmpty()) { "A release must have at least one platform artifact" }
    }

    val javaVersion: Int get() = artifacts.values.first().javaVersion
    val build: Int get() = artifacts.values.first().build
    val patch: Int get() = artifacts.values.first().patch

    /** Directory name produced by extracting the archive, e.g. `"jextract-25"`. */
    val extractedDirName: String get() = "jextract-$javaVersion"

    private constructor(
        javaVersion: Int,
        build: Int,
        patch: Int,
        builder: Builder.() -> Unit,
    ) : this(Builder(javaVersion, build, patch).apply(builder).artifacts)

    /** Convenience constructor — no SHA-256 known. */
    constructor(
        javaVersion: Int,
        build: Int,
        patch: Int,
        vararg platforms: JextractPlatform,
    ) : this(javaVersion, build, patch, builder = { platforms.forEach { artifact(it) } })

    /** Convenience constructor — all platforms share per-platform SHA-256 entries. */
    constructor(
        javaVersion: Int,
        build: Int,
        patch: Int,
        vararg platformsWithSha: Pair<JextractPlatform, String>,
    ) : this(javaVersion, build, patch, builder = { platformsWithSha.forEach { (p, sha) -> artifact(p, sha) } })

    private class Builder(val javaVersion: Int, val build: Int, val patch: Int) {
        val artifacts = LinkedHashMap<JextractPlatform, PlatformArtifact>()
        fun artifact(platform: JextractPlatform, sha256: String? = null) {
            artifacts[platform] = PlatformArtifact(javaVersion, build, patch, platform, sha256)
        }
    }
}

/**
 * Metadata for a single platform's jextract binary archive.
 *
 * @property fileNameStem The filename **without** the `.tar.gz` extension.
 *   This is used as the Ivy `[artifact]` token so Gradle can construct the
 *   correct download URL.
 */
internal class PlatformArtifact(
    val javaVersion: Int,
    val build: Int,
    val patch: Int,
    platform: JextractPlatform,
    /** Kept for potential static verification; unused at runtime since we download the .sha256 file. */
    val sha256: String?,
) {
    val platformId: String = platform.id
    val fileNameStem: String = "openjdk-$javaVersion-jextract+$build-${patch}_${platformId}_bin"
    val fileName: String = "$fileNameStem.tar.gz"
    val sha256FileName: String = "$fileName.sha256"
    val baseUrl: String = "https://download.java.net/java/early_access/jextract/$javaVersion/$build"
    val url: String = "$baseUrl/$fileName"
    val sha256Url: String = "$baseUrl/$sha256FileName"
}

// ── Release catalog (newest first) ────────────────────────────────────────────

private val JEXTRACT_25_2_4 = JextractRelease(
    25,
    2,
    4,
    JextractPlatform.LINUX_X64,
    JextractPlatform.LINUX_ARM64,
    JextractPlatform.MAC_X64,
    JextractPlatform.MAC_ARM64,
    JextractPlatform.WINDOWS_X64,
)

private val JEXTRACT_22_6_47 = JextractRelease(
    22,
    6,
    47,
    JextractPlatform.LINUX_X64,
    JextractPlatform.LINUX_ARM64,
    JextractPlatform.MAC_X64,
    JextractPlatform.MAC_ARM64,
    JextractPlatform.WINDOWS_X64,
)

private val JEXTRACT_21_1_2 = JextractRelease(
    21,
    1,
    2,
    JextractPlatform.LINUX_X64,
    JextractPlatform.MAC_X64,
    JextractPlatform.WINDOWS_X64,
)

private val JEXTRACT_20_1_2 = JextractRelease(
    20,
    1,
    2,
    JextractPlatform.LINUX_X64,
    JextractPlatform.MAC_X64,
    JextractPlatform.WINDOWS_X64,
)

private val JEXTRACT_19_2_3 = JextractRelease(
    19,
    2,
    3,
    JextractPlatform.LINUX_X64,
    JextractPlatform.MAC_X64,
    JextractPlatform.WINDOWS_X64,
)

internal fun JextractVersion.release(): JextractRelease = when (this) {
    JextractVersion.VERSION_25 -> JEXTRACT_25_2_4
    JextractVersion.VERSION_22 -> JEXTRACT_22_6_47
    JextractVersion.VERSION_21 -> JEXTRACT_21_1_2
    JextractVersion.VERSION_20 -> JEXTRACT_20_1_2
    JextractVersion.VERSION_19 -> JEXTRACT_19_2_3
}
