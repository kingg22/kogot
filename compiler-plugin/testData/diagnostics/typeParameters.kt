// RUN_PIPELINE_TILL: FRONTEND
// White box: type parameters propagate an existing @MustBeVariant contract, otherwise their upper bounds decide.

import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.Node2D

fun <@MustBeVariant T> annotated(value: T) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value)
fun <T> unbounded(value: T) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>value<!>)
fun <T : Any> nonNull(value: T) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>value<!>)
fun <T : Node2D> nodeBound(value: T) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value)
fun <T : <!FINAL_UPPER_BOUND!>Regex<!>> incompatibleBound(value: T) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>value<!>)
fun <T> stringLike(value: T) where T : CharSequence, T : Comparable<T> = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>value<!>)
// All bounds hold at once: one incompatible bound is enough.
fun <T> mixedBounds(value: T) where T : Comparable<T>, T : Sequence<Int> = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>value<!>)
fun <T, U : T> nested(value: U) where T : Node2D = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value)
fun <T> explicitInGeneric() = <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!MUST_BE_VARIANT_UNKNOWN_TYPE!>T<!>>()

fun callers() {
    // Callers of a function with an annotated type parameter are checked too.
    <!MUST_BE_VARIANT_CHECKED!>annotated<!>(1L)
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>annotated<!>(1.toByte())
    <!MUST_BE_VARIANT_CHECKED!>annotated<!><<!NOT_VARIANT_COMPATIBLE!>Regex<!>>(Regex("x"))
    // Unannotated type parameters declare no contract.
    unbounded(1.toByte())
    // A reified type argument inferred from the argument.
    Variant.<!MUST_BE_VARIANT_CHECKED!>from<!>(2.5f)
    Variant.<!MUST_BE_VARIANT_CHECKED!>from<!>(<!NOT_VARIANT_COMPATIBLE!>'c'<!>)
}
