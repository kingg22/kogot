package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.EnumMask
import io.github.kingg22.godot.api.builtin.GodotString
import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.asGodotString
import io.github.kingg22.godot.api.builtin.internal.checkVariantTypeAndConvert
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.internal.checkGodotError

// TODO: Signal2, Signal3, ... up to Signal22 following the same pattern

/**
 * A signal with 1 typed parameter.
 *
 * Example:
 * ```kotlin
 * val vidaCambio: Signal1<Int> = signal("vida_cambio", param<Int>("nueva_vida"))
 *
 * // Connect
 * vidaCambio.connect { nuevaVida -> println("Vida changed to $nuevaVida") }
 *
 * // Emit
 * vidaCambio.emit(100)
 * ```
 */
public class Signal1<@MustBeVariant P1>(
    public val nameKString: String,
    private val param1: SignalParameterDescriptor<P1>,
) : Function1<P1, Unit> {
    public val name: GodotString by lazy(PUBLICATION) { nameKString.asGodotString() }

    @PublishedApi
    internal lateinit var owner: GodotObject

    public fun register(owner: GodotObject) {
        this.owner = owner
        owner.addUserSignal(name)
    }

    /**
     * Emits this signal with the given argument.
     *
     * @param p1 The first parameter value
     */
    public fun emit(p1: P1) {
        val arg1 = checkVariantTypeAndConvert(p1, param1.variantType)
        checkGodotError(
            "emit signal: $nameKString",
            StringName(name).use { signalName ->
                owner.emitSignal(signalName, arg1)
            },
        )
    }

    /**
     * Connects a callback to this signal.
     *
     * @param callback The callback to invoke when the signal is emitted
     * @param flags Connection flags (see [GodotObject.connect])
     */
    public inline fun connect(flags: EnumMask<GodotObject.ConnectFlags> = EnumMask(0), callback: (P1) -> Unit) {
        val callable = createCallable { args ->
            @Suppress("UNCHECKED_CAST")
            callback(args[0] as P1)
        }
        checkGodotError(
            "connect signal: $nameKString",
            StringName(name).use { signalName ->
                owner.connect(signalName, callable, flags.value.toUInt())
            },
        )
    }

    /**
     * Disconnects a callback from this signal.
     */
    public inline fun disconnect(callback: (P1) -> Unit) {
        val callable = createCallable { args ->
            @Suppress("UNCHECKED_CAST")
            callback(args[0] as P1)
        }
        StringName(name).use { signalName ->
            owner.disconnect(signalName, callable)
        }
    }

    override operator fun invoke(p1: P1) {
        emit(p1)
    }
}
