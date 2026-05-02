package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.builtin.Callable

// ---------------------------------------------------------------------------
// Internal callable creation
// ---------------------------------------------------------------------------

/**
 * Creates a Godot [Callable] from a Kotlin lambda.
 *
 * This is a placeholder implementation. The full implementation requires:
 * - A callback/trampoline mechanism in the native runtime (CallableTrampolines)
 * - Integration with Godot's callable_custom_create2
 *
 * Currently returns an empty callable for compilation.
 */
@PublishedApi
@Suppress("UNUSED_PARAMETER")
internal inline fun <R : Any> createCallable(callback: (Array<Any?>) -> R): Callable {
    // TODO: When CallableFactory is implemented, use:
    // return CallableFactory.create(callback)

    // For now, return empty callable (placeholder - signal connections won't work)
    return Callable()
}
