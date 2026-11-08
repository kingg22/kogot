// RUN_PIPELINE_TILL: FRONTEND
// Minimal stand-ins for the generated Callable API: a zero-argument constructor plus typed top-level factories.

// FILE: api.kt
package io.github.kingg22.godot.api.builtin

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class MustBeVariant

class Callable() {
    constructor(lambda: () -> @MustBeVariant Any?) : this()
}

// Same shape as the generated factories, which also annotate the use sites inside the lambda type.
inline fun <reified @MustBeVariant P1, @MustBeVariant R> Callable(
    noinline lambda: (@MustBeVariant P1) -> @MustBeVariant R,
): Callable = Callable()

inline fun <reified @MustBeVariant P1, reified @MustBeVariant P2, @MustBeVariant R> Callable(
    noinline lambda: (@MustBeVariant P1, @MustBeVariant P2) -> @MustBeVariant R,
): Callable = Callable()

// FILE: usage.kt
import io.github.kingg22.godot.api.builtin.*

fun valid() {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { println("zero arguments") }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { 42 }
    // Godot INT arrives as Long: Int and Short parameters are narrowed, and sent back widened.
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { count: Short, name: String -> name.repeat(count.toInt()) }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { count: Int -> count.toShort() }
}

fun invalid() {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>1.toByte()<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { pattern: Regex -> pattern.pattern }
    // Byte is unsupported in both directions (signed in Kotlin, unsigned in Godot's PackedByteArray).
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { level: Byte -> level.toInt() }
}
