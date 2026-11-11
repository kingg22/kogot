// RUN_PIPELINE_TILL: FRONTEND
// `@MustBeVariant` on the type parameters of the Godot typed containers (`GodotArray<@MustBeVariant T>`,
// `Dictionary<@MustBeVariant K, @MustBeVariant V>`): what is enforced where the container is built or written down.

import io.github.kingg22.godot.api.builtin.*

class UserThing

fun constructorCalls() {
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><Long>()
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><Variant>()
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><<!NOT_VARIANT_COMPATIBLE!>UserThing<!>>()
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><<!NOT_VARIANT_COMPATIBLE!>Regex<!>>()
    <!MUST_BE_VARIANT_CHECKED!>Dictionary<!><String, Long>()
    <!MUST_BE_VARIANT_CHECKED!>Dictionary<!><String, <!NOT_VARIANT_COMPATIBLE!>Char<!>>()
    <!MUST_BE_VARIANT_CHECKED!>Dictionary<!><<!NOT_VARIANT_COMPATIBLE!>Char<!>, String>()
}

fun nestedCollections() {
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><GodotArray<Long>>()
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><GodotArray<<!NOT_VARIANT_COMPATIBLE!>Byte<!>>>()
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><Dictionary<String, GodotArray<Vector2>>>()
    <!MUST_BE_VARIANT_CHECKED!>Dictionary<!><String, GodotArray<Long>>()
    <!MUST_BE_VARIANT_CHECKED!>Dictionary<!><String, GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>>()
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><PackedInt32Array>()
}

fun untypedElements() {
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><<!MUST_BE_VARIANT_UNKNOWN_TYPE!>Any?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><<!MUST_BE_VARIANT_UNKNOWN_TYPE!>Any<!>>()
    <!MUST_BE_VARIANT_CHECKED!>Dictionary<!><<!MUST_BE_VARIANT_UNKNOWN_TYPE!>Any?<!>, <!MUST_BE_VARIANT_UNKNOWN_TYPE!>Any?<!>>()
}

class Holder<@MustBeVariant T> {
    fun make(): GodotArray<T> = <!MUST_BE_VARIANT_CHECKED!>GodotArray<!>()
}

fun <T> unconstrained(): GodotArray<<!MUST_BE_VARIANT_UNKNOWN_TYPE!>T<!>> = <!MUST_BE_VARIANT_CHECKED, MUST_BE_VARIANT_UNKNOWN_TYPE!>GodotArray<!>()

fun <@MustBeVariant T> constrained(): GodotArray<T> = <!MUST_BE_VARIANT_CHECKED!>GodotArray<!>()

// Written types are checked too, not only the calls that build a value: the type is wrong where it is declared.
val property: GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>? = null

lateinit var lateProperty: GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>

fun parameter(array: GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>, map: Dictionary<String, <!NOT_VARIANT_COMPATIBLE!>Char<!>>) {}

fun returned(): GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>> = TODO()

fun nullableAndNested(a: GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>?, b: GodotArray<GodotArray<<!NOT_VARIANT_COMPATIBLE!>Byte<!>>>, c: Dictionary<String, GodotArray<<!NOT_VARIANT_COMPATIBLE!>Char<!>>>) {}

fun valid(a: GodotArray<Long>, b: GodotArray<GodotArray<Vector2>>, c: Dictionary<String, GodotArray<Long>>, d: GodotArray<*>) {}

fun <T> unconstrainedParameter(a: GodotArray<<!MUST_BE_VARIANT_UNKNOWN_TYPE!>T<!>>) {}

fun <@MustBeVariant T> constrainedParameter(a: GodotArray<T>) {}

class Derived : Builtin() {
    val field: Dictionary<String, <!NOT_VARIANT_COMPATIBLE!>Char<!>>? = null
}

typealias RegexArray = GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>

fun usesAlias(a: <!NOT_VARIANT_COMPATIBLE!>RegexArray<!>) {}

fun locals(value: Any) {
    val local: GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>? = null
    val cast = value <!UNCHECKED_CAST!>as? GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>><!>
    if (value is GodotArray<*>) value
}

// The call and the written type are each reported where they are written.
class Scene {
    lateinit var myCollection: GodotArray<<!NOT_VARIANT_COMPATIBLE!>Regex<!>>

    fun ready() {
        myCollection = <!MUST_BE_VARIANT_CHECKED!>GodotArray<!><<!NOT_VARIANT_COMPATIBLE!>Regex<!>>()
    }
}
