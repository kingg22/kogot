package io.github.kingg22.godot.api.signal

// ---------------------------------------------------------------------------
// Factory functions (top-level DSL)
// ---------------------------------------------------------------------------

/**
 * Creates a [Signal0] with the given name.
 *
 * Example:
 * ```kotlin
 * val gameOver = signal("game_over")
 * ```
 */
public fun signal(name: String): Signal0 = Signal0(name)

/**
 * Creates a [Signal1] with the given name and parameter.
 *
 * Example:
 * ```kotlin
 * val vidaCambio = signal("vida_cambio", param<Int>("nueva_vida"))
 * ```
 */
public fun <P1> signal(name: String, param1: SignalParameterDescriptor<P1>): Signal1<P1> = Signal1(name, param1)
