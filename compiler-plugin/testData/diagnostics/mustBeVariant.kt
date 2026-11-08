// RUN_PIPELINE_TILL: FRONTEND
// Minimal stand-ins for the generated Godot API: the checker only matches these fully qualified names.

// FILE: api.kt
package io.github.kingg22.godot.api.builtin

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class MustBeVariant

class Variant
class Vector2

fun anyToVariant(element: @MustBeVariant Any?): Variant = Variant()

inline fun <reified T> from(element: @MustBeVariant T): Variant = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(element)

fun <@MustBeVariant T> variantOf(): Variant = Variant()

fun notAnnotated(element: Any?): Variant = Variant()

// FILE: core.kt
package io.github.kingg22.godot.api.core

open class GodotObject
class Node : GodotObject()

// FILE: usage.kt
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.Node

class Player
interface Unrelated
enum class Team { RED }

fun <@MustBeVariant T> propagates(value: T) = <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(value)

fun compatible(number: Number, any: Any?) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(42L)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>("hello kogot")
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(null)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(Vector2())
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(Node())
    <!MUST_BE_VARIANT_CHECKED!>from<!>(3.5)
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Int>()
    // Open types may hold a Variant value at runtime: left to the runtime check.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(number)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(any)
    notAnnotated(Player())
}

fun incompatible(unrelated: Unrelated) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>Player()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>1.toShort()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>Team.RED<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>unrelated<!>)
    <!MUST_BE_VARIANT_CHECKED!>from<!>(<!NOT_VARIANT_COMPATIBLE!>Player()<!>)
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Player<!>>()
}
