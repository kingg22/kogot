package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.internal.anyToVariant
import io.github.kingg22.godot.api.builtin.internal.anyToVariantOrNull
import io.github.kingg22.godot.api.builtin.internal.getValue
import kotlin.contracts.contract
import kotlin.reflect.KProperty

/** Syntactic sugar for [GodotString] constructor */
public fun String.toGodotString(): GodotString = GodotString(this)

/** Syntactic sugar for [NodePath] constructor */
public fun String.toNodePath(): NodePath = NodePath(this)

/** Syntactic sugar for [StringName] constructor */
public fun String.toStringName(): StringName = StringName(this)

/** Converts [String] to [Variant] using [GodotString] as [Variant.Type.STRING] */
public fun String?.toVariant(): Variant = toVariantString()

/** Converts [String] to [Variant] using [GodotString] as [Variant.Type.STRING] */
public fun String?.toVariantString(): Variant = this?.toGodotString().use { it.toVariant() }

/** Converts [String] to [Variant] using [NodePath] as [Variant.Type.NODE_PATH] */
public fun String?.toVariantNodePath(): Variant = this?.toNodePath().use { it.toVariant() }

/** Converts [String] to [Variant] using [StringName] as [Variant.Type.STRING_NAME] */
public fun String?.toVariantStringName(): Variant = this?.toStringName().use { it.toVariant() }

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
 * **Safety**:
 * - [CPointer][kotlinx.cinterop.COpaquePointer] is not supported, use
 * [Object][io.github.kingg22.godot.api.core.GodotObject] if it's an Object pointer or
 * primary constructor of Variant if it's a Variant pointer instead.
 * - [String] is mapped to [GodotString] and box to [Variant]
 */
@ExperimentalGodotKotlin
public inline fun <reified T> Variant.Companion.from(element: @MustBeVariant T): Variant = anyToVariant(this)

/**
 * Syntactic sugar for [getValue]
 * Must prefer explicit convert methods over this operator.
 */
@ExperimentalGodotKotlin
public inline val Variant.value: @MustBeVariant Any? get() {
    val value: Any? by this
    return value
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
@ExperimentalGodotKotlin
public inline operator fun <@MustBeVariant reified T> Variant.getValue(thisRef: T?, property: KProperty<*>): T {
    // Fast path - Redundant
    if (T::class == Variant::class) return this as T

    val value = getValue()

    // Fast path - Already the right type
    if (value is T) return value

    // String case
    if (T::class == String::class && value is GodotString) return value.toKString() as T

    // Downcast
    if (T::class == Int::class && value is Number) return value.toInt() as T
    if (T::class == Float::class && value is Number) return value.toFloat() as T
    if (T::class == Short::class && value is Number) return value.toShort() as T
    if (T::class == Byte::class && value is Number) return value.toByte() as T

    throw ClassCastException("Expected ${T::class} but got ${value?.let { it::class }}, for variant value: $value")
}
