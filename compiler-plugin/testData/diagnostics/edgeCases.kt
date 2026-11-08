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

fun typedContainers(variant: Variant) {
    // Godot typed containers: their element types must be Variant compatible too.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>GodotArray<Regex>()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>Dictionary<String, Char>()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>GodotArray<GodotArray<Byte>>()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(GodotArray<GodotArray<Vector2>>())
    // Untyped Godot containers hold any Variant.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(GodotArray<Any?>())
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(Dictionary<Any?, Any?>())
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><GodotArray<*>>()
}

fun outsideCalls(variant: Variant) {
    // Reading a @MustBeVariant value declares no new contract: it is already compatible wherever it is passed.
    val read = variant.value
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(variant.value)
    // A reference bypasses the contract of every call made through it.
    val reference = <!MUST_BE_VARIANT_UNCHECKED_REFERENCE!>::anyToVariant<!>
    // APIs without the annotation declare no contract.
    notAnnotated(1.toByte())
}
