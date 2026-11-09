package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.utils.GD

/**
 * A connection made with a typed `connect`. [close] (or [disconnect]) removes it from Godot and releases the handler.
 *
 * Both are idempotent and safe after the connection is gone: a `ONE_SHOT` connection already fired, or the emitter
 * was freed (Godot drops its connections with it).
 */
public class SignalConnection internal constructor(
    private val ownerId: Long,
    private val signal: Signal,
    private val callable: Callable,
) : AutoCloseable {
    private var closed = false

    /** `true` while the emitter is alive and Godot still holds this connection. */
    public val isConnected: Boolean
        get() = !closed && GD.isInstanceIdValid(ownerId) && signal.isConnected(callable)

    /** Same as [close]. */
    public fun disconnect(): Unit = close()

    override fun close() {
        if (closed) return
        if (isConnected) signal.disconnect(callable)
        closed = true
        callable.close()
        signal.close()
    }
}
