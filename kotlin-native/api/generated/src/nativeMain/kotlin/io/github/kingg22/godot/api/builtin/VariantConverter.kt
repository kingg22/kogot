package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.internal.anyToVariant
import io.github.kingg22.godot.api.builtin.internal.anyToVariantOrNull
import io.github.kingg22.godot.api.builtin.internal.value
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf

/**
 * Converts an element to a [Variant] with type checking.
 *
 * This is unsafe, use caution. Prefers explicit convert methods over this function.
 *
 * @see anyToVariant
 */
@ExperimentalGodotKotlin
public inline fun <@MustBeVariant reified T> T?.toVariant(): Variant {
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
public inline fun <@MustBeVariant reified T> T?.toVariantOrNull(): Variant? {
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
public inline fun <reified T> Variant.Companion.from(element: @MustBeVariant T): Variant = anyToVariant(element)

/**
 * Syntactic sugar for [value] with type checking and inferred return type.
 *
 * Must prefer explicit convert methods over this operator.
 *
 * Supports [String] conversion from [GodotString] and the narrowing conversions described in [MustBeVariant]:
 * [Int] and [Short] from `INT`, [Float] from `FLOAT`. A narrowing never loses the value: a `Long` outside the range of
 * [Int] or [Short], or a finite `Double` outside the range of [Float], is not convertible and gives `null`. Read it as
 * [Long] or [Double] and narrow it explicitly (`toInt()`) if truncating is what you want. A [Float] still rounds to the
 * nearest value, like [Double.toFloat].
 *
 * Returns `null` when the value is not a [T]. A `NIL` variant also reads as `null` for a nullable [T]; use [getValue]
 * to tell the two apart.
 */
public inline fun <@MustBeVariant reified T> Variant.getValueOrNull(): T? {
    // Fast path - Redundant
    if (T::class == Variant::class) return this as T

    // A typed collection keeps its element types: they come from the reified type, `value is T` cannot see them.
    if (T::class == GodotArray::class || T::class == Dictionary::class) return toCollectionOrNull(typeOf<T>()) as T?

    val value = this.value

    // Fast path - Already the right type
    if (value is T) return value
    if (value == null && null is T) return null

    // String and Unit cases: NIL also reads back as Unit, the value sent for it.
    if (T::class == String::class && value is GodotString) return value.use { it.toKString() } as T
    if (T::class == Unit::class && value == null) return Unit as T

    // Narrowing, only from the matching Godot type (see MustBeVariant): INT is a Long, FLOAT a Double. Only when it fits.
    if (T::class == Int::class && value is Long && value in Int.MIN_VALUE..Int.MAX_VALUE) return value.toInt() as T
    if (T::class == Short::class && value is Long && value in Short.MIN_VALUE..Short.MAX_VALUE) {
        return value.toShort() as T
    }
    if (T::class == Float::class && value is Double &&
        (value.isNaN() || value.isInfinite() || abs(value) <= Float.MAX_VALUE)
    ) {
        return value.toFloat() as T
    }

    // Not convertible: a wrapper built by `value` that nobody receives is closed here.
    (value as? AutoCloseable)?.close()
    return null
}

/**
 * Syntactic sugar for [value] with type checking and inferred return type.
 *
 * Must prefer explicit convert methods over this operator.
 *
 * Supports the same conversions as [getValueOrNull]; [T] must be Variant compatible (see [MustBeVariant]).
 *
 * A `NIL` variant reads as `null` when [T] is nullable (`getValue<Long?>()`), and is a mismatch otherwise.
 *
 * @throws [ClassCastException] if the value is not of the expected type.
 * @throws [ArithmeticException] if the value is a number of the right Godot type that does not fit in [T]
 * (`getValue<Int>()` of an `INT` above [Int.MAX_VALUE]). Read it as [Long] or [Double] instead.
 */
public inline fun <@MustBeVariant reified T> Variant.getValue(): T {
    getValueOrNull<T>()?.let { return it }
    // `getValueOrNull` is also null for a legit NIL read as a nullable T, which is not a mismatch.
    if (null is T && getType() == Variant.Type.NIL) return null as T

    // Describe the value before closing it: it is a wrapper built just for this message.
    val value = this.value
    val description = value.toString()
    val actualKClass = value?.let { it::class }

    (value as? AutoCloseable)?.close()

    if ((value is Long && (T::class == Int::class || T::class == Short::class)) ||
        (value is Double && T::class == Float::class)
    ) {
        val wider = if (value is Long) "Long" else "Double"
        throw ArithmeticException("$description does not fit in ${T::class}: read it as $wider and narrow it yourself")
    }

    throw ClassCastException("Expected ${T::class} but got $actualKClass, for variant value: $description")
}

/**
 * Operator syntactic sugar for [getValue]
 * @see value
 * @see Variant.getValue
 * @see Variant.getValueOrNull
 */
@ExperimentalGodotKotlin
public inline operator fun <@MustBeVariant reified T> Variant.getValue(thisRef: Any?, property: KProperty<*>): T =
    getValue()
