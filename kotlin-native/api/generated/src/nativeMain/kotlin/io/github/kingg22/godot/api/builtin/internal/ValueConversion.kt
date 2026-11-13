package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.builtin.GodotString
import kotlin.math.abs
import kotlin.reflect.KClass

/*
 * The conversions and checks that read a Godot value as a Kotlin type, shared by `getValueOrNull<T>()` (the type is
 * reified) and the runtime codecs (the type is a [KClass] taken from a `KType`), so both accept and reject the same values.
 */

/** Result of [convertValue] when the value cannot be read as the requested type. */
@PublishedApi
internal object NoConversion

/**
 * Reads [value] (what `Variant.value` returned) as [kClass], or [NoConversion].
 *
 * Accepts the value itself, `NIL` for a [nullable] type or [Unit], a [GodotString] as [String], and the narrowing
 * conversions of `MustBeVariant`: [Int] and [Short] from an `INT` (a [Long]), [Float] from a `FLOAT` (a [Double]), only
 * when the value fits. It never closes [value]: the caller owns it.
 */
@PublishedApi
internal fun convertValue(value: Any?, kClass: KClass<*>, nullable: Boolean): Any? {
    if (kClass.isInstance(value)) return value
    if (value == null && nullable) return null

    // String and Unit cases: NIL also reads back as Unit, the value sent for it.
    if (kClass == String::class && value is GodotString) return value.use { it.toKString() }
    if (kClass == Unit::class && value == null) return Unit

    // Narrowing, only from the matching Godot type: INT is a Long, FLOAT a Double. Only when it fits.
    if (kClass == Int::class && value is Long && value in Int.MIN_VALUE..Int.MAX_VALUE) return value.toInt()
    if (kClass == Short::class && value is Long && value in Short.MIN_VALUE..Short.MAX_VALUE) return value.toShort()
    if (kClass == Float::class && value is Double &&
        (value.isNaN() || value.isInfinite() || abs(value) <= Float.MAX_VALUE)
    ) {
        return value.toFloat()
    }
    return NoConversion
}

/**
 * The error for a [value] that [convertValue] could not read as [kClass]: an [ArithmeticException] when it is a number of
 * the right Godot type that does not fit, a [ClassCastException] otherwise. [description] is the value as text, taken
 * before the wrapper was closed.
 */
@PublishedApi
internal fun valueMismatch(kClass: KClass<*>, value: Any?, description: String): Throwable {
    if ((value is Long && (kClass == Int::class || kClass == Short::class)) ||
        (value is Double && kClass == Float::class)
    ) {
        val wider = if (value is Long) "Long" else "Double"
        return ArithmeticException("$description does not fit in $kClass: read it as $wider and narrow it yourself")
    }
    return ClassCastException("Expected $kClass but got ${value?.let { it::class }}, for variant value: $description")
}
