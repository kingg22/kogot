package io.github.kingg22.godot.api.annotations

/**
 * Marks a class as a Godot class.
 *
 * ```kotlin
 * @Godot class MyGodotClass(rawPtr: ObjectPtr) : Node(rawPtr) {
 *     // ...
 * }
 * ```
 */
@Target(CLASS)
// BINARY, not SOURCE: KSP reads it from compiled dependencies too, e.g. to name a signal argument's registered class.
@Retention(BINARY)
@MustBeDocumented
public annotation class Godot
