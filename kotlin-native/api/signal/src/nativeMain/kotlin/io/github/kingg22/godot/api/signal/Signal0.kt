package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.EnumMask
import io.github.kingg22.godot.api.builtin.GodotString
import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.asGodotString
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.internal.checkGodotError

/**
 * A signal with no parameters.
 *
 * Example:
 * ```kotlin
 * val gameOver: Signal0 = signal("game_over")
 * ```
 */
public class Signal0(public val nameKString: String) : Function0<Unit> {
    public val name: GodotString by lazy { nameKString.asGodotString() }

    @PublishedApi
    internal lateinit var owner: GodotObject
        private set

    public fun register(owner: GodotObject) {
        this.owner = owner
        owner.addUserSignal(name)
    }

    /**
     * Emits this signal with no arguments.
     */
    public fun emit() {
        StringName(name).use { signalName ->
            checkGodotError("emit of signal: $nameKString", owner.emitSignal(signalName))
        }
    }

    /**
     * Connects a callback to this signal.
     *
     * @param callback The callback to invoke when the signal is emitted
     * @param flags Connection flags (see [GodotObject.connect])
     */
    public inline fun connect(flags: EnumMask<GodotObject.ConnectFlags> = EnumMask(0), callback: () -> Unit) {
        val callable = createCallable { callback() }
        StringName(name).use { signalName ->
            checkGodotError("connect signal of $nameKString", owner.connect(signalName, callable, flags.value.toUInt()))
        }
    }

    /**
     * Disconnects a callback from this signal.
     */
    public inline fun disconnect(callback: () -> Unit) {
        val callable = createCallable { callback() }
        val signalName = StringName(name)
        owner.disconnect(signalName, callable)
        signalName.close()
    }

    override operator fun invoke() {
        emit()
    }
}
