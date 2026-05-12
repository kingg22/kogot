package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.internal.anyToVariant
import io.github.kingg22.godot.api.builtin.internal.anyToVariantOrNull
import kotlin.contracts.contract
import kotlin.reflect.KProperty

/**
 * Converts an element to a [Variant] with type checking.
 *
 * This is unsafe, use caution. Prefers explicit convert methods over this function.
 *
 * @see anyToVariant
 */
@ExperimentalGodotKotlin
public inline fun <reified T> T?.toVariant(): Variant {
    contract { returns() implies (this@toVariant != null) }
    return anyToVariant(this)
}

/**
 * Converts an element to a [Variant] with type checking or null if you can't be converted.
 *
 * This is unsafe, use caution. Prefers explicit convert methods over this function.
 *
 * @see anyToVariantOrNull
 */
@ExperimentalGodotKotlin
public inline fun <reified T> T?.toVariantOrNull(): Variant? {
    contract { returns() implies (this@toVariantOrNull != null) }
    return anyToVariantOrNull(this)
}

/**
 * Converts an element to a [Variant] with type checking.
 *
 * This is unsafe, use caution. Prefers explicit convert methods over this function.
 *
 * @see anyToVariant
 */
@ExperimentalGodotKotlin
public inline fun <reified T> Variant.Companion.from(element: @MustBeVariant T): Variant = anyToVariant(this)

/**
 * Syntactic sugar for [Variant.value] with type checking and inferred return type.
 *
 * Must prefer explicit convert methods over this operator.
 *
 * Supports [String] conversion from [GodotString].
 * [Int], [Float], [Short], [Byte] downcasting.
 */
public inline fun <reified T> Variant.getValueOrNull(): T? {
    // Fast path - Redundant
    if (T::class == Variant::class) return this as T

    val value = this.value

    // Fast path - Already the right type
    if (value is T) return value

    // String case
    if (T::class == String::class && value is GodotString) return value.toKString() as T

    // Downcast
    if (T::class == Int::class && value is Number) return value.toInt() as T
    if (T::class == Float::class && value is Number) return value.toFloat() as T
    if (T::class == Short::class && value is Number) return value.toShort() as T
    if (T::class == Byte::class && value is Number) return value.toByte() as T

    return null
}

/**
 * Syntactic sugar for [Variant.value] with type checking and inferred return type.
 *
 * Must prefer explicit convert methods over this operator.
 *
 * Supports [String] conversion from [GodotString].
 * [Int], [Float], [Short], [Byte] downcasting.
 *
 * @throws [ClassCastException] if the value is not of the expected type.
 */
public inline fun <reified T> Variant.getValue(): T = getValueOrNull()
    ?: run {
        val value = this.value
        throw ClassCastException("Expected ${T::class} but got ${value?.let { it::class }}, for variant value: $value")
    }

/**
 * Operator syntactic sugar for [getValue]
 * @see Variant.value
 * @see Variant.getValue
 * @see Variant.getValueOrNull
 */
@ExperimentalGodotKotlin
public inline operator fun <@MustBeVariant reified T> Variant.getValue(thisRef: T?, property: KProperty<*>): T =
    getValue()
