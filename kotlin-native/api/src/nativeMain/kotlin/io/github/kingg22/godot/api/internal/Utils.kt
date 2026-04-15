package io.github.kingg22.godot.api.internal

import io.github.kingg22.godot.api.global.GodotError

@Suppress("NOTHING_TO_INLINE")
public inline fun checkGodotError(context: String, error: GodotError) {
    check(error == GodotError.OK) {
        "Godot Error: $error (${error.value}) in $context"
    }
}
