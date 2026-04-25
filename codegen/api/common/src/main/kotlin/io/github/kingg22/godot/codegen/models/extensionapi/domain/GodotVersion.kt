package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.extensionapi.Header

/**
 * @property hex Full version encoded as hexadecimal with one byte (2 hex digits) per number
 * (e.g., for "3.1.12" it would be 0x03010C)
 * @property status e.g. "stable", "beta", "rc1", "rc2"
 * @property build e.g. "custom_build"
 * @property timestamp Git commit date UNIX timestamp in seconds, or 0 if unavailable.
 * @property string e.g. "Godot v3.1.4.stable.official.mono"
 */
@JvmRecord
data class GodotVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val hex: Int,
    val status: String,
    val build: String,
    val timestamp: Long,
    val string: String,
) : Comparable<GodotVersion> {
    constructor(versionHeader: Header) : this(
        versionHeader.versionMajor,
        versionHeader.versionMinor,
        versionHeader.versionPatch,
        "${versionHeader.versionMajor}${versionHeader.versionMinor}${versionHeader.versionPatch}".hexToInt(),
        versionHeader.versionStatus,
        versionHeader.versionBuild,
        0,
        versionHeader.versionFullName,
    )

    // region: KotlinVersion

    /*
     * Took from kotlin.KotlinVersion
     *
     * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
     * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
     */

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

    // endregion
    override fun toString(): String = string

    override fun compareTo(other: GodotVersion): Int = compareTo(other.major, other.minor, other.patch)

    /** Verifica si esta versión es estrictamente anterior a la versión especificada. */
    fun isBefore(major: Int, minor: Int, patch: Int = 0): Boolean = compareTo(major, minor, patch) < 0

    /** Verifica si esta versión es estrictamente posterior a la versión especificada. */
    fun isAfter(major: Int, minor: Int, patch: Int = 0): Boolean = compareTo(major, minor, patch) > 0

    /** Compara esta versión con componentes específicos sin necesidad de instanciar otro [GodotVersion]. */
    fun compareTo(major: Int, minor: Int, patch: Int = 0): Int {
        if (this.major != major) return this.major - major
        if (this.minor != minor) return this.minor - minor
        return this.patch - patch
    }

    /** Devuelve true si la versión es considerada estable (no es beta, rc, alpha, dev, etc.). */
    fun isStable(): Boolean = status.equals("stable", ignoreCase = true)

    /** Determina si esta versión pertenece a una línea de lanzamiento específica (ej. 4.x). */
    fun isMajor(major: Int): Boolean = this.major == major

    /**
     * Crea una copia simplificada de la versión, útil para comparaciones de compatibilidad
     * donde el hash o el timestamp no son relevantes.
     */
    fun toShortString(): String = "$major.$minor.$patch-$status"
}
