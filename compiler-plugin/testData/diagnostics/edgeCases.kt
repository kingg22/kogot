// RUN_PIPELINE_TILL: FRONTEND
// Edge cases: type aliases, smart casts, casts, nullability, named arguments and the documented limits of the checker.

import io.github.kingg22.godot.api.builtin.*

typealias Position = Vector2
typealias Pattern = Regex
typealias Positions = GodotArray<Position>

fun typeAliases(position: Position, pattern: Pattern, aabb: AABB, rid: RID, positions: Positions) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(position)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>pattern<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(aabb)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(rid)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(positions)
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Pattern<!>>()
}

fun smartCastsAndCasts(value: Any, other: Any) {
    // A smart cast types the value as `Any & Byte`: the incompatible part wins.
    if (value is Byte) <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>value<!>)
    if (value is Long) <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>value as Char<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(other as? Int)
}

fun literals() {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(null)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(Unit)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(TODO())
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(element = <!NOT_VARIANT_COMPATIBLE!>1.toByte()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>1u<!>)
}

fun knownLimits(variant: Variant) {
    // Type arguments of builtins are not checked: GodotArray<Regex> is still an ARRAY.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(GodotArray<Regex>())
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(Dictionary<Regex, Char>())
    // Reading a @MustBeVariant value and referencing (not calling) an annotated function declare no contract.
    val read = variant.value
    val reference = ::anyToVariant
    // APIs without the annotation are not checked.
    notAnnotated(1.toByte())
}
