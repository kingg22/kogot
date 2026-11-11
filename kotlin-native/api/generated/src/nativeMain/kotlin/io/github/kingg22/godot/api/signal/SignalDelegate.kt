package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.core.GodotObject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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
private data class SignalDelegate<F : Function<Unit>>(private var signal: TypedSignal<F>? = null) :
    ReadOnlyProperty<GodotObject, TypedSignal<F>> {

    override operator fun getValue(thisRef: GodotObject, property: KProperty<*>): TypedSignal<F> =
        signal ?: TypedSignal<F>(thisRef, property.name).also { signal = it }
}
