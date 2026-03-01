package io.github.kingg22.godot.internal.wrapper

import io.github.kingg22.godot.internal.ffm.GDExtensionGodotVersion2
import java.lang.foreign.MemorySegment

private const val MAX_COMPONENT_VALUE = 255

/**
 * ```c++
 * struct {
 *     uint32_t major;
 *     uint32_t minor;
 *     uint32_t patch;
 *     uint32_t hex;
 *     const char *status;
 *     const char *build;
 *     const char *hash;
 *     uint64_t timestamp;
 *     const char *string;
 * }
 * ```
 *
 * @property hex Full version encoded as hexadecimal with one byte (2 hex digits) per number
 * (e.g., for "3.1.12" it would be 0x03010C)
 * @property status e.g. "stable", "beta", "rc1", "rc2"
 * @property build e.g. "custom_build"
 * @property hash Full Git commit hash.
 * @property timestamp Git commit date UNIX timestamp in seconds, or 0 if unavailable.
 * @property string e.g. "Godot v3.1.4.stable.official.mono"
 * @see io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceGetGodotVersion2
 */
@JvmRecord
@ConsistentCopyVisibility
data class GodotVersion2 private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val hex: Int,
    val status: String,
    val build: String,
    val hash: String,
    val timestamp: Long,
    val string: String,
) : Comparable<GodotVersion2> {
    override fun toString(): String = string
    private val version get() = versionOf(major, minor, patch)

    constructor(struct: MemorySegment) : this(
        major = GDExtensionGodotVersion2.major(struct),
        minor = GDExtensionGodotVersion2.minor(struct),
        patch = GDExtensionGodotVersion2.patch(struct),
        hex = GDExtensionGodotVersion2.hex(struct),
        status = GDExtensionGodotVersion2.status(struct).getString(0),
        build = GDExtensionGodotVersion2.build(struct).getString(0),
        hash = GDExtensionGodotVersion2.hash(struct).getString(0),
        timestamp = GDExtensionGodotVersion2.timestamp(struct),
        string = GDExtensionGodotVersion2.string(struct).getString(0),
    )

    /*
     * Took from kotlin.KotlinVersion
     *
     * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
     * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
     */
    override fun compareTo(other: GodotVersion2): Int = version - other.version

    private fun versionOf(major: Int, minor: Int, patch: Int): Int {
        require(
            major in 0..MAX_COMPONENT_VALUE && minor in 0..MAX_COMPONENT_VALUE && patch in 0..MAX_COMPONENT_VALUE,
        ) {
            "Version components are out of range: $major.$minor.$patch"
        }
        return major.shl(16) + minor.shl(8) + patch
    }

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major] and [minor] components.
     */
    fun isAtLeast(major: Int, minor: Int): Boolean = // this.version >= versionOf(major, minor, 0)
        this.major > major || (this.major == major && this.minor >= minor)

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major], [minor] and [patch] components.
     */
    fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean = // this.version >= versionOf(major, minor, patch)
        this.major > major ||
            (this.major == major && (this.minor > minor || (this.minor == minor && this.patch >= patch)))
}
