package io.github.kingg22.godot.api

import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode

/** Marker for Godot API that is experimental and may change or be removed in the future based on Godot GDExtension API. */
@UsedFromCodegenGeneratedCode
@RequiresOptIn(
    message = "This API is experimental and may change or removed in the future version of Godot.",
    level = ERROR,
)
@Retention(BINARY)
@Target(CLASS, FUNCTION, PROPERTY)
@MustBeDocumented
public annotation class ExperimentalGodotApi @ExperimentalGodotKotlin constructor(val reason: String = "")
