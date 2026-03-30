package io.github.kingg22.godot.api

import org.jetbrains.annotations.ApiStatus
import kotlin.enums.enumEntries

/** Marker interface for all generated Godot Enum. */
@ApiStatus.NonExtendable
public interface GodotEnum {
    public val value: Long
}

internal inline fun <reified T> godotEnumFrom(value: Long): T where T : GodotEnum, T : Enum<T> =
    enumEntries<T>().firstOrNull { it.value == value }
        ?: error("Enum entry with value '$value' not found for type '${T::class.simpleName}'")
