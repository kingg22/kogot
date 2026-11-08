// RUN_PIPELINE_TILL: FRONTEND
// Black box: a @MustBeVariant type checks every value stored in it (initializers, assignments, returns, getters), and
// values read from it count as compatible, so the contract travels with the value.

import io.github.kingg22.godot.api.builtin.*

class Holder {
    val member: @MustBeVariant Any? = <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!>
    val getter: @MustBeVariant Any? get() = <!NOT_VARIANT_COMPATIBLE!>'c'<!>
    var mutable: @MustBeVariant Any? = 1
}

val topLevel: @MustBeVariant Any? = <!NOT_VARIANT_COMPATIBLE!>1.toByte()<!>

fun locals(flag: Boolean, holder: Holder) {
    val valid: @MustBeVariant Any? = "text"
    val invalid: @MustBeVariant Any? = <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!>
    val branches: @MustBeVariant Any? = if (flag) 1 else <!NOT_VARIANT_COMPATIBLE!>'c'<!>
    var reassigned: @MustBeVariant Any? = 1
    reassigned = <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!>
    reassigned = Vector2()
    holder.mutable = <!NOT_VARIANT_COMPATIBLE!>1u<!>
    // Values read from a @MustBeVariant type are compatible: no warning for their open static type.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(valid)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(reassigned)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(holder.member)
}

fun blockBody(flag: Boolean): @MustBeVariant Any? {
    if (flag) return <!NOT_VARIANT_COMPATIBLE!>Regex("early")<!>
    return 1L
}

fun expressionBody(): @MustBeVariant Any? = <!NOT_VARIANT_COMPATIBLE!>'c'<!>

// A lambda inside a @MustBeVariant function returns to itself, not to the function.
fun nestedLambda(): @MustBeVariant Any? {
    listOf(1).map { Regex("$it") }
    return 1
}

// Propagation: the contract checks the callers, not the body.
fun forward(value: @MustBeVariant Any?) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value)

fun callers() {
    <!MUST_BE_VARIANT_CHECKED!>forward<!>(1)
    <!MUST_BE_VARIANT_CHECKED!>forward<!>(<!NOT_VARIANT_COMPATIBLE!>Regex("x")<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(expressionBody())
}
