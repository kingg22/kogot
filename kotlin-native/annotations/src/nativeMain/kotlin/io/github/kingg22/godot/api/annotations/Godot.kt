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
@Retention(SOURCE)
@MustBeDocumented
public annotation class Godot
