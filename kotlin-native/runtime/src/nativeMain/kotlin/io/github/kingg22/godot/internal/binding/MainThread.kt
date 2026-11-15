package io.github.kingg22.godot.internal.binding

import kotlin.native.concurrent.ThreadLocal

/**
 * `true` only on the thread that ran [MainThread.markCurrent]. Every other thread, including a foreign engine thread
 * that Kotlin/Native attaches on its first callback, starts from the initializer and sees `false`.
 */
@ThreadLocal
private var isMainThreadMarker: Boolean = false

/**
 * Knows which thread is Godot's main thread (issue #113), without asking the engine.
 *
 * The GDExtension interface has no "is main thread" query, and the engine's `Thread.is_main_thread()` is an engine
 * call (not usable before the API is loaded, nor cheap enough for a hot path). Like godot-rust's `is_main_thread`,
 * the extension remembers the thread its initialization runs on: Godot always initializes extensions on the main
 * thread, and [markCurrent] is called there by [BindingInitializationCallbacks.initialize] for every level (so it
 * holds whatever minimum initialization level the extension declares).
 *
 * The check is a thread-local read: free, safe from any thread, and correct even before initialization (where it
 * answers `false` everywhere).
 *
 * Kogot's threading model is documented in `docs/threading-model.md`.
 */
@InternalBinding
public object MainThread {
    /** Whether the calling thread is Godot's main thread. `false` on every thread before the extension initialized. */
    public val isCurrent: Boolean get() = isMainThreadMarker

    /** Marks the calling thread as the main thread. Called by the extension's initialization, idempotent. */
    public fun markCurrent() {
        isMainThreadMarker = true
    }

    /**
     * Reports a Godot error when the calling thread is not the main thread, for an [operation] that must only run
     * there. It never throws: it runs where an exception would cross the C boundary or skip a release (the caller
     * decides what to do with the answer). Returns [isCurrent].
     */
    public fun check(operation: String): Boolean {
        if (isCurrent) return true
        reportCallbackFailure(
            operation,
            IllegalStateException("$operation must run on Godot's main thread, called from another thread"),
        )
        return false
    }
}
