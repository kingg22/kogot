package io.github.kingg22.godot.api.internal

import org.jetbrains.annotations.ApiStatus

/**
 * Marks a generated engine class with its Godot class name, which the Kotlin name does not always keep
 * (`Object` is `GodotObject`, `JSONRPC` is `Jsonrpc`).
 */
@ApiStatus.Internal
@Retention(BINARY)
@Target(CLASS)
public annotation class GodotClass(val godotName: String)
