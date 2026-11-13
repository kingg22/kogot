package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.core.GodotObject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Declares a [TypedSignal] named after the property of `() -> Unit`.
 *
 * Register it with `@RegisterSignal` so Godot knows the signal.
 */
public fun signal(): ReadOnlyProperty<GodotObject, TypedSignal<() -> Unit>> = SignalDelegate(null)

/**
 * Declares a [TypedSignal] named after the property; [F] describes its parameters, e.g. `(value: Long) -> Unit`.
 *
 * Register it with `@RegisterSignal` so Godot knows the signal.
 */
public fun <F : Function<Unit>> signal(): ReadOnlyProperty<GodotObject, TypedSignal<F>> = SignalDelegate(null)

/**
 * Declares a [TypedSignal] of `() -> Unit` for the signal [name], e.g. `"child_entered_tree"`.
 */
public fun signal(name: String): ReadOnlyProperty<GodotObject, TypedSignal<() -> Unit>> = SignalDelegate(name)

/**
 * Declares a [TypedSignal] for the signal [name] with the parameters [F] describes,
 * e.g. `signal<(node: Node) -> Unit>("child_entered_tree")`
 */
public fun <F : Function<Unit>> signal(name: String): ReadOnlyProperty<GodotObject, TypedSignal<F>> =
    SignalDelegate(name)

/**
 * Property delegate behind [signal]: creates the [TypedSignal] on first access and keeps that one, named [name] or,
 * when it is `null`, after the property.
 */
private data class SignalDelegate<F : Function<Unit>>(
    private val name: String?,
    private var signal: TypedSignal<F>? = null,
) : ReadOnlyProperty<GodotObject, TypedSignal<F>> {

    override operator fun getValue(thisRef: GodotObject, property: KProperty<*>): TypedSignal<F> =
        signal ?: TypedSignal<F>(thisRef, name ?: property.name).also { signal = it }
}
