// RUN_PIPELINE_TILL: FRONTEND
// Black box: a function type with @MustBeVariant parts is a contract on the lambdas stored in it, whether it is a
// parameter, a property, a local variable or a function's return type.

import io.github.kingg22.godot.api.builtin.*

fun regexProducer(): Regex = Regex("x")

fun accept(action: () -> @MustBeVariant Any?) {}
fun receive(handler: (@MustBeVariant Any?) -> Unit) {}

class Holder {
    val member: () -> @MustBeVariant Any? = { <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!> }
}

fun parameters() {
    <!MUST_BE_VARIANT_CHECKED!>accept<!> { 1 }
    <!MUST_BE_VARIANT_CHECKED!>accept<!> { <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!> }
    // The lambda's parameter is typed `@MustBeVariant Any?`, so it can be forwarded without a warning.
    <!MUST_BE_VARIANT_CHECKED!>receive<!> { value -> <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value) }
}

fun locals(flag: Boolean) {
    val valid: () -> @MustBeVariant Any? = { if (flag) 1 else "one" }
    val invalid: () -> @MustBeVariant Any? = { <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!> }
    val reference: () -> @MustBeVariant Any? = <!NOT_VARIANT_COMPATIBLE!>::regexProducer<!>
    var reassigned: () -> @MustBeVariant Any? = { 1 }
    reassigned = { <!NOT_VARIANT_COMPATIBLE!>'c'<!> }
    // A function value that already carries the contract is compatible where it is passed.
    <!MUST_BE_VARIANT_CHECKED!>Callable<!>(valid)
    <!MUST_BE_VARIANT_CHECKED!>accept<!>(reassigned)
}

fun factory(): () -> @MustBeVariant Any? = { <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!> }

fun blockFactory(flag: Boolean): () -> @MustBeVariant Any? {
    if (flag) return { 1L }
    return { <!NOT_VARIANT_COMPATIBLE!>1u<!> }
}
