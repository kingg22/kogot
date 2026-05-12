package io.github.kingg22.godot.api.internal

import io.github.kingg22.godot.api.GodotError

@UsedFromCodegenGeneratedCode
@Suppress("NOTHING_TO_INLINE")
public inline fun checkGodotError(context: String, error: GodotError) {
    check(error == GodotError.OK) {
        "Godot Error: $error (${error.value}) in $context"
    }
}

@UsedFromCodegenGeneratedCode
@Suppress("NOTHING_TO_INLINE")
public inline fun checkGodotError(context: String, error: Long) {
    check(error == GodotError.OK.value) {
        val errorEntry = GodotError.entries.firstOrNull { it.value == error }
        "Godot Error: ${errorEntry ?: "'unknown enum entry'"} ($error) in $context"
    }
}
