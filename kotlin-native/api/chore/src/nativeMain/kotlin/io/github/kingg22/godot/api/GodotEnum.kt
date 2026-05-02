package io.github.kingg22.godot.api

import org.jetbrains.annotations.ApiStatus
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

/**
 * Marker interface for all generated Godot Enum.
 *
 * @see EnumMask
 */
@ApiStatus.NonExtendable
public interface GodotEnum {
    public val value: Long

    public companion object {
        /** Returns the **first enum entry** with the given [value], or throws an exception if not found. */
        public inline fun <reified T> fromValue(value: Long): T where T : GodotEnum, T : Enum<T> =
            fromValueOrNull(value)
                ?: error("Enum entry with value '$value' not found for type '${T::class.simpleName}'")

        /** Returns the **first** enum entry with the given [value], or null if not found. */
        public inline fun <reified T> fromValueOrNull(value: Long): T? where T : GodotEnum, T : Enum<T> =
            entries<T>().firstOrNull { it.value == value }

        /** Shortcut for [enumEntries] of the enum type. */
        public inline fun <reified T> entries(): EnumEntries<T> where T : GodotEnum, T : Enum<T> = enumEntries<T>()
    }
}
