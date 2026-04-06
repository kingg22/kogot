package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.builtin.GodotString
import io.github.kingg22.godot.api.core.GodotObject

/**
 * Marker interface for all Godot signals.
 *
 * Signals in Godot are the primary mechanism for communication between objects.
 * They allow an object to notify other objects when something happens without
 * direct coupling.
 *
 * Example usage:
 * ```kotlin
 * val vidaCambio: Signal1<Int> = signal("vida_cambio", param<Int>("nueva_vida"))
 * ```
 */
public interface Signal {
    /** The name of the signal in Godot */
    public val name: GodotString

    /**
     * Registers this signal with a [GodotObject].
     * This should be called during class registration/initialization.
     */
    public fun register(owner: GodotObject)
}
