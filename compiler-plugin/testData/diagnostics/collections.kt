// RUN_PIPELINE_TILL: FRONTEND
// White box: an annotated type argument (`List<@MustBeVariant Any?>`) is a contract on every value entering the
// collection: the collection itself (declaration, assignment, argument, return), its writes (`add`, `set`, `put`,
// `+=`) and its factories (`listOf`, `mutableMapOf`). Reads keep the annotation, so they are Variant compatible.

import io.github.kingg22.godot.api.builtin.*

class Box<T>(var content: T)

fun <T> MutableList<T>.addTwice(element: T) {
    add(element)
    add(element)
}

fun takesList(values: List<@MustBeVariant Any?>) {}

// Declarations: the collection's own type arguments and its factory's elements.
val fromFactory: List<@MustBeVariant Any?> = listOf(1L, "two", <!NOT_VARIANT_COMPATIBLE!>Regex("")<!>)
val explicitFactory = <!MUST_BE_VARIANT_CHECKED!>listOf<!><@MustBeVariant Any?>(1L, <!NOT_VARIANT_COMPATIBLE!>Regex("")<!>)
val mapFactory: Map<String, @MustBeVariant Any?> = mapOf("a" to 1L, "b" to <!NOT_VARIANT_COMPATIBLE!>Regex("")<!>)
val existingRegexes: List<Regex> = emptyList()
val fromExisting: List<@MustBeVariant Any?> = <!NOT_VARIANT_COMPATIBLE!>existingRegexes<!>
val nested: List<List<@MustBeVariant Any?>> = listOf(listOf(1L), listOf(<!NOT_VARIANT_COMPATIBLE!>Regex("")<!>))

fun writes(list: MutableList<@MustBeVariant Any?>, map: MutableMap<String, @MustBeVariant Any?>, box: Box<@MustBeVariant Any?>) {
    list.<!MUST_BE_VARIANT_CHECKED!>add<!>(1L)
    list.<!MUST_BE_VARIANT_CHECKED!>add<!>(<!NOT_VARIANT_COMPATIBLE!>Regex("")<!>)
    <!MUST_BE_VARIANT_CHECKED!>list[0]<!> = <!NOT_VARIANT_COMPATIBLE!>Regex("")<!>
    <!MUST_BE_VARIANT_CHECKED!>list += <!NOT_VARIANT_COMPATIBLE!>Regex("")<!><!>
    list.<!MUST_BE_VARIANT_CHECKED!>addAll<!>(listOf(<!NOT_VARIANT_COMPATIBLE!>Regex("")<!>))
    list.<!MUST_BE_VARIANT_CHECKED!>addTwice<!>(<!NOT_VARIANT_COMPATIBLE!>Regex("")<!>)
    <!MUST_BE_VARIANT_CHECKED!>map["key"]<!> = <!NOT_VARIANT_COMPATIBLE!>Regex("")<!>
    map.<!MUST_BE_VARIANT_CHECKED!>put<!>("key", "value")
    box.content = <!NOT_VARIANT_COMPATIBLE!>Regex("")<!>
    <!MUST_BE_VARIANT_CHECKED!>Box<!><@MustBeVariant Any?>(<!NOT_VARIANT_COMPATIBLE!>Regex("")<!>)
}

fun arguments(regexes: ArrayList<Regex>, strings: Set<String>) {
    <!MUST_BE_VARIANT_CHECKED!>takesList<!>(<!NOT_VARIANT_COMPATIBLE!>regexes<!>)
    <!MUST_BE_VARIANT_CHECKED!>takesList<!>(strings.toList())
    <!MUST_BE_VARIANT_CHECKED!>takesList<!>(listOf(1L, <!NOT_VARIANT_COMPATIBLE!>Regex("")<!>))
}

fun returned(regexes: List<Regex>): List<@MustBeVariant Any?> {
    if (regexes.isEmpty()) return <!NOT_VARIANT_COMPATIBLE!>regexes<!>
    return listOf(1L)
}

// Reads keep the annotation: no warning for the open `Any?` element type.
fun reads(list: List<@MustBeVariant Any?>, map: Map<String, @MustBeVariant Any?>) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(list[0])
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(map["key"])
    list.<!MUST_BE_VARIANT_CHECKED!>forEach<!> { <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(it) }
}

// An inferred type is the value's own: checked once, at the call that creates it.
fun inferred() = <!MUST_BE_VARIANT_CHECKED!>listOf<!><@MustBeVariant Any?>(<!NOT_VARIANT_COMPATIBLE!>Regex("")<!>)
