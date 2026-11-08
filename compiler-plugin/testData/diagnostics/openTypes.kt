// RUN_PIPELINE_TILL: FRONTEND
// Black box: open types may hold a Variant-compatible value, so they are accepted and checked at runtime (slow path).
// White box: only types some compatible type can be are open; unrelated open classes and stdlib interfaces are not.

import io.github.kingg22.godot.api.GodotNative
import io.github.kingg22.godot.api.builtin.*

interface UserInterface
fun interface UserFunInterface { fun run() }
sealed interface UserSealedInterface
open class UnrelatedOpen
abstract class UnrelatedAbstract

fun accepted(
    any: Any,
    nullableAny: Any?,
    number: Number,
    comparable: Comparable<Int>,
    charSequence: CharSequence,
    autoCloseable: AutoCloseable,
    godotNative: GodotNative,
    userInterface: UserInterface,
    userFunInterface: UserFunInterface,
    userSealedInterface: UserSealedInterface,
) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>any<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>nullableAny<!>)
    // Number may hold an Int/Long/Short/Float/Double, Comparable and CharSequence a String...
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>number<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>comparable<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>charSequence<!>)
    // ...and every builtin is an AutoCloseable GodotNative.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>autoCloseable<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>godotNative<!>)
    // User interfaces may be implemented by a GodotObject subclass.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>userInterface<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>userFunInterface<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>userSealedInterface<!>)
    <!MUST_BE_VARIANT_CHECKED, MUST_BE_VARIANT_UNKNOWN_TYPE, MUST_BE_VARIANT_UNKNOWN_TYPE!>Callable<!> { value: Number -> value }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!MUST_BE_VARIANT_UNKNOWN_TYPE!>any<!> }
}

fun rejected(unrelatedOpen: UnrelatedOpen, unrelatedAbstract: UnrelatedAbstract) {
    // Single class inheritance: a GodotObject can never also be an unrelated class.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>unrelatedOpen<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>unrelatedAbstract<!>)
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { value: UnrelatedOpen -> }
}
