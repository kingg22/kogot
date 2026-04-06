package io.github.kingg22.godot.api.signal

import kotlin.reflect.typeOf

/**
 * Creates a parameter descriptor for signal registration.
 *
 * Example:
 * ```kotlin
 * signal("vida_cambio", param<Int>("nueva_vida"))
 * ```
 */
public inline fun <reified P> param(name: String): SignalParameterDescriptor<P> =
    SignalParameterDescriptor(name, typeOf<P>())
