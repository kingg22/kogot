package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.GodotEnum
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import io.github.kingg22.godot.api.toEnumMask
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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

    /**
     * Connects [callable], taking ownership of it. Uses the builtin [Signal.connect] because it passes the flags
     * with the width Godot reads.
     *
     * @throws IllegalArgumentException for [GodotObject.ConnectFlags.APPEND_SOURCE_OBJECT]: it adds an argument the
     * typed handler does not declare.
     * @throws IllegalStateException when Godot rejects the connection (e.g. it already exists without
     * [GodotObject.ConnectFlags.REFERENCE_COUNTED]).
     */
    @PublishedApi
    @UsedFromCodegenGeneratedCode
    internal fun connectCallable(callable: Callable, flags: Array<out GodotObject.ConnectFlags>): SignalConnection {
        try {
            require(GodotObject.ConnectFlags.APPEND_SOURCE_OBJECT !in flags) {
                "APPEND_SOURCE_OBJECT adds an argument the typed handler of '$name' does not declare"
            }
            checkOpen()
            val mask = flags.toEnumMask().value
            val error = GodotEnum.fromValue<GodotError>(signal.connect(callable, mask))
            check(error == GodotError.OK) { "Cannot connect to signal '$name': $error" }
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
        signal.close()
        nameHandle.close()
    }

    private fun checkOpen() = check(!closed) { "Signal '$name' is closed" }

    override fun toString(): String = "TypedSignal($name)"
}

/**
 * Declares a [TypedSignal] named after the property of `() -> Unit`.
 *
 * Register it with `@RegisterSignal` so Godot knows the signal.
 */
public fun signal(): ReadOnlyProperty<GodotObject, TypedSignal<() -> Unit>> = SignalDelegate()

/**
 * Declares a [TypedSignal] named after the property; [F] describes its parameters, e.g. `(value: Long) -> Unit`.
 *
 * Register it with `@RegisterSignal` so Godot knows the signal.
 */
public fun <F : Function<Unit>> signal(): ReadOnlyProperty<GodotObject, TypedSignal<F>> = SignalDelegate()

/** Property delegate behind [signal]: creates the [TypedSignal] on first access. */
private class SignalDelegate<F : Function<Unit>> : ReadOnlyProperty<GodotObject, TypedSignal<F>> {
    private var signal: TypedSignal<F>? = null

    override operator fun getValue(thisRef: GodotObject, property: KProperty<*>): TypedSignal<F> =
        signal ?: TypedSignal<F>(thisRef, property.name).also { signal = it }
}
