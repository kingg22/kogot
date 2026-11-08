// RUN_PIPELINE_TILL: FRONTEND
// Black box: reading a value out of a Variant follows the same rules as sending one (see MustBeVariant). The target
// type is the extractor's type argument, explicit or inferred from the expected type, including `by variant`.

import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.Node2D

open class PlainNode

fun explicit(variant: Variant) {
    variant.<!MUST_BE_VARIANT_CHECKED!>getValue<!><Long>()
    variant.<!MUST_BE_VARIANT_CHECKED!>getValue<!><Int>()
    variant.<!MUST_BE_VARIANT_CHECKED!>getValueOrNull<!><String>()
    variant.<!MUST_BE_VARIANT_CHECKED!>getValueOrNull<!><Vector2>()
    variant.<!MUST_BE_VARIANT_CHECKED!>getValue<!><<!NOT_VARIANT_COMPATIBLE!>Regex<!>>()
    variant.<!MUST_BE_VARIANT_CHECKED!>getValueOrNull<!><<!NOT_VARIANT_COMPATIBLE!>Byte<!>>()
    variant.<!MUST_BE_VARIANT_CHECKED!>getValue<!><<!MUST_BE_VARIANT_UNKNOWN_TYPE!>Any?<!>>()
}

fun inferred(variant: Variant) {
    val count: Int = variant.<!MUST_BE_VARIANT_CHECKED!>getValue<!>()
    val name: String? = variant.<!MUST_BE_VARIANT_CHECKED!>getValueOrNull<!>()
    val pattern: Regex = variant.<!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>getValue<!>()
}

fun senders(regex: Regex) {
    42L.<!MUST_BE_VARIANT_CHECKED!>toVariant<!>()
    "hello".<!MUST_BE_VARIANT_CHECKED!>toVariant<!>()
    regex.<!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>toVariant<!>()
}

// `by variant`: member, top-level and local properties.
val topLevel: Long by <!MUST_BE_VARIANT_CHECKED!>Variant()<!>

class Stats(variant: Variant) {
    val health: Int by <!MUST_BE_VARIANT_CHECKED!>variant<!>
    val position: Vector2 by <!MUST_BE_VARIANT_CHECKED!>variant<!>
    val pattern: Regex by <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>variant<!>
}

fun local(variant: Variant) {
    val speed: Double by <!MUST_BE_VARIANT_CHECKED!>variant<!>
    val node: Node2D by <!MUST_BE_VARIANT_CHECKED!>variant<!>
    val plain: PlainNode by <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>variant<!>
    val char: Char by <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>variant<!>
}
