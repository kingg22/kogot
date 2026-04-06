package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.builtin.Callable

// ---------------------------------------------------------------------------
// Internal callable creation (placeholder - needs runtime binding support)
// ---------------------------------------------------------------------------

/**
 * Creates a Godot [Callable] from a Kotlin lambda.
 *
 * TODO: This is a placeholder. The actual implementation requires:
 * - A callback/trampoline mechanism in the native runtime
 * - Memory management for the callback closure
 * - Integration with Godot's callable system
 *
 * One approach is to create a static callback object that dispatches
 * to a closure stored via a key in a global WeakMap.
 */
@PublishedApi
@Suppress("UNUSED_PARAMETER")
internal inline fun <R : Any> createCallable(callback: (Array<Any?>) -> R): Callable {
    // TODO: Implementation requires native callback trampoline
    // For now, return empty callable as placeholder
    return Callable()
}
