// Shared stand-in for the hand-written typed signal API (kotlin-native/api/generated, api.signal).
package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.core.GodotObject
import kotlin.properties.ReadOnlyProperty

class TypedSignal<F : Function<Unit>>

fun signal(): ReadOnlyProperty<GodotObject, TypedSignal<() -> Unit>> = TODO()

// The real API is native only; on the JVM both overloads would erase to the same signature.
@JvmName("typedSignal")
fun <F : Function<Unit>> signal(): ReadOnlyProperty<GodotObject, TypedSignal<F>> = TODO()

// Signals that already exist in Godot: the name is explicit, nothing is registered.
fun signal(name: String): ReadOnlyProperty<GodotObject, TypedSignal<() -> Unit>> = TODO()

@JvmName("typedSignalNamed")
fun <F : Function<Unit>> signal(name: String): ReadOnlyProperty<GodotObject, TypedSignal<F>> = TODO()
