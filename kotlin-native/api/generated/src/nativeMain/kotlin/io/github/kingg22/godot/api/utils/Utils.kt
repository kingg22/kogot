package io.github.kingg22.godot.api.utils

import io.github.kingg22.godot.api.builtin.GodotString
import io.github.kingg22.godot.api.builtin.asVariant

@Suppress("NOTHING_TO_INLINE")
public inline fun GD.pushError(arg1: String, vararg args: String) {
    pushError(
        GodotString(arg1).use { it.asVariant() },
        *args.map { GodotString(it).use { str -> str.asVariant() } }.toTypedArray(),
    )
}
