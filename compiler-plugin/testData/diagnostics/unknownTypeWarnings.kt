// RUN_PIPELINE_TILL: CODEGEN
// RENDER_DIAGNOSTICS_FULL_TEXT
// Black box: a value whose concrete type is only known at runtime is a warning that says how to make it checkable.

import io.github.kingg22.godot.api.builtin.*

// Hint: annotate the parameter, which moves the check to the callers.
fun fromParameter(value: Any?) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>value<!>)
fun fromParameterFixed(value: @MustBeVariant Any?) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value)

// Hint: annotate the type parameter.
fun <T> fromTypeParameter() = <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!MUST_BE_VARIANT_UNKNOWN_TYPE!>T<!>>()
fun <@MustBeVariant T> fromTypeParameterFixed() = <!MUST_BE_VARIANT_CHECKED!>variantOf<!><T>()

// Hint: use a concrete type or suppress by name.
fun fromExpression(values: Map<String, Any>) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_UNKNOWN_TYPE!>values.getValue("key")<!>)
}

@Suppress("MUST_BE_VARIANT_UNKNOWN_TYPE")
fun suppressed(values: Map<String, Any>) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(values.getValue("key"))
}

fun suppressedReference() {
    @Suppress("MUST_BE_VARIANT_UNCHECKED_REFERENCE")
    val reference = ::anyToVariant
}
