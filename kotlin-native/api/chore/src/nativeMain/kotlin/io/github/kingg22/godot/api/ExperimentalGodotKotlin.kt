package io.github.kingg22.godot.api

/** Marker for Experimental APIs of the Godot Kotlin Bindings. It's not related to Godot. */
@RequiresOptIn(
    level = ERROR,
    message = "This API is experimental and may change or removed in the future.",
)
@Target(CLASS, FUNCTION, PROPERTY, TYPEALIAS, CONSTRUCTOR)
@Retention(BINARY)
@MustBeDocumented
public annotation class ExperimentalGodotKotlin
