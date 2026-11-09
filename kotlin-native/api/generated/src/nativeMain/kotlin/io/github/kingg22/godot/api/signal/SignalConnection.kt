package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.utils.GD

/**
 * A connection made with a `TypedSignal.connect`. [close] (or [disconnect]) removes it from Godot and releases the
 * handler.
 *
 * Both are idempotent and safe after the connection is gone: a `ONE_SHOT` connection already fired, the emitter was
 * freed (Godot drops its connections with it), or the same callable was connected again through a newer handle,
 * which then owns that connection.
 */
public class SignalConnection internal constructor(
    private val ownerId: Long,
    private val signal: Signal,
    private val callable: Callable,
) : AutoCloseable {
    internal var isClosed: Boolean = false
        private set

    /** `true` while the emitter is alive and Godot still holds this connection. */
    public val isConnected: Boolean
        get() = !isClosed && GD.isInstanceIdValid(ownerId) && signal.isConnected(callable)

    /** Same as [close]. */
    public fun disconnect(): Unit = close()

    override fun close() {
        if (isClosed) return
        if (isConnected) signal.disconnect(callable)
        release()
    }

    /** Whether both handles connect equal callables, which Godot treats as the same connection. */
    internal fun hasSameCallableAs(other: SignalConnection): Boolean = callable == other.callable

    /**
     * Releases the native handles without disconnecting: for a stale handle whose connection is gone and whose
     * callable is now connected through a newer handle.
     */
    internal fun release() {
        if (isClosed) return
        isClosed = true
        callable.close()
        signal.close()
    }
}
