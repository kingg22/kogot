package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import io.github.kingg22.godot.api.internal.checkGodotError
import io.github.kingg22.godot.api.toEnumMask

/**
 * A signal of [owner] whose parameters are described by the function type [F], e.g. `(value: Long) -> Unit`.
 *
 * Declare it with the [signal] delegate; the signal name is the property name:
 * ```
 * @RegisterSignal
 * val punch by signal<(value: Long) -> Unit>()
 *
 * punch.emit(15L)
 * val connection = punch.connect { value -> println(value) }
 * connection.close() // disconnects
 * ```
 *
 * The typed `emit` and `connect` overloads (zero to 23 parameters) are generated extensions; their parameter types
 * are `@MustBeVariant`, so the kogot compiler plugin checks every argument and handler at the call site.
 *
 * A [TypedSignal] owns native handles (the name and the builtin [Signal]): [close] it when [owner] is deleted, e.g.
 * on `NOTIFICATION_PREDELETE`. Closing it does not disconnect anything; connections live in Godot until they are
 * closed (see [SignalConnection]) or [owner] is freed.
 */
@UsedFromCodegenGeneratedCode
public class TypedSignal<F : Function<Unit>> @PublishedApi internal constructor(
    /** The object that declares and emits this signal. */
    public val owner: GodotObject,
    /** The signal name, as registered in Godot. */
    public val name: String,
) : AutoCloseable {
    private val nameHandle = StringName(name)
    private val signal = Signal(owner, nameHandle)
    private var closed = false

    /**
     * Live handles from [connect] with an untyped `Callable`. Typed handlers never need tracking: each `connect`
     * builds its own custom callable, so no two handles share one.
     */
    private val untypedConnections = mutableListOf<SignalConnection>()

    /**
     * Emits through [GodotObject.emitSignal], the only path that reports errors (the builtin [Signal.emit] drops
     * them). [variants] are the converted [values]: the ones created for this call are closed, a value that already
     * was a [Variant] stays owned by the caller.
     */
    @PublishedApi
    @UsedFromCodegenGeneratedCode
    internal fun emitArguments(values: Array<out Any?>, variants: Array<Variant>): GodotError {
        try {
            checkOpen()
            return owner.emitSignal(nameHandle, *variants)
        } finally {
            variants.forEachIndexed { i, variant -> if (variant !== values[i]) variant.close() }
        }
    }

    /** `true` while anything is connected to this signal: Kotlin handlers, GDScript methods or editor connections. */
    public val hasConnections: Boolean
        get() {
            checkOpen()
            return signal.hasConnections()
        }

    /**
     * Connects any [callable], e.g. a GDScript method (`Callable(node, "on_hit".toStringName())`). Unlike the typed
     * `connect`, its arguments are not checked at compile time: Godot reports a mismatch when the signal is emitted.
     *
     * The connection keeps its own copy, so [callable] stays owned by the caller.
     *
     * Godot identifies a connection by its callable, so an earlier handle for an equal callable whose connection is
     * gone (a `ONE_SHOT` that fired, a disconnect from GDScript) is released here: closing it later must not
     * disconnect this new connection.
     *
     * @throws IllegalStateException when Godot rejects the connection (e.g. it already exists without
     * [GodotObject.ConnectFlags.REFERENCE_COUNTED]).
     */
    public fun connect(callable: Callable, vararg flags: GodotObject.ConnectFlags): SignalConnection {
        val connection = connectOwned(Callable(callable), flags)
        // Without REFERENCE_COUNTED, Godot only accepts an equal callable once its previous connection is gone, so
        // every earlier handle for it is stale. With it, an earlier handle may still own one reference: keep it.
        val referenceCounted = GodotObject.ConnectFlags.REFERENCE_COUNTED in flags
        untypedConnections.removeAll { earlier ->
            // A closed handle's callable is already freed: never compare it.
            if (!earlier.isClosed && !referenceCounted && earlier.hasSameCallableAs(connection)) earlier.release()
            earlier.isClosed
        }
        untypedConnections += connection
        return connection
    }

    /**
     * A copy of the builtin [Signal], for what [TypedSignal] does not wrap (e.g. [Signal.getConnections]) or to pass
     * it where a Variant is expected. The caller owns and closes it.
     */
    public fun asSignal(): Signal {
        checkOpen()
        return Signal(signal)
    }

    /**
     * Connects a typed handler's [callable], taking ownership of it.
     *
     * @throws IllegalArgumentException for [GodotObject.ConnectFlags.APPEND_SOURCE_OBJECT]: it adds an argument the
     * typed handler does not declare.
     * @throws IllegalStateException when Godot rejects the connection.
     */
    @PublishedApi
    @UsedFromCodegenGeneratedCode
    internal fun connectCallable(callable: Callable, flags: Array<out GodotObject.ConnectFlags>): SignalConnection {
        require(GodotObject.ConnectFlags.APPEND_SOURCE_OBJECT !in flags) {
            callable.close()
            "APPEND_SOURCE_OBJECT adds an argument the typed handler of '$name' does not declare"
        }
        return connectOwned(callable, flags)
    }

    /**
     * Connects [callable], taking ownership of it (closed when the connection is, or right away on failure). Uses the
     * builtin [Signal.connect] because it passes the flags with the width Godot reads.
     */
    private fun connectOwned(callable: Callable, flags: Array<out GodotObject.ConnectFlags>): SignalConnection {
        try {
            checkOpen()
            val mask = flags.toEnumMask().value
            val error = signal.connect(callable, mask)
            checkGodotError("connect to signal '$name'", error)
        } catch (e: Throwable) {
            callable.close()
            throw e
        }
        return SignalConnection(owner.getInstanceId().toLong(), Signal(signal), callable)
    }

    /** Releases the native handles. Idempotent; existing connections are left untouched. */
    override fun close() {
        if (closed) return
        closed = true
        // The handles stay valid on their own; this signal just stops tracking them.
        untypedConnections.clear()
        signal.close()
        nameHandle.close()
    }

    private fun checkOpen() = check(!closed) { "Signal '$name' is closed" }

    override fun toString(): String = "TypedSignal($name)"
}
