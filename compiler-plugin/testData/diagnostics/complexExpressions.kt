// RUN_PIPELINE_TILL: FRONTEND
// White box: the value passed is whatever an expression evaluates to. Branching expressions and inline scope functions
// are checked per branch; other calls by their declared return type.

import io.github.kingg22.godot.api.builtin.*

class Box(val long: Long, val pattern: Regex) {
    fun long(): Long = long
    fun pattern(): Regex = pattern
}

fun regexOf(): Regex = Regex("x")
fun <T> identity(value: T): T = value

fun branches(flag: Boolean, option: Int, a: Long, b: String, pattern: Regex) {
    Variant.<!MUST_BE_VARIANT_CHECKED!>from<!>(if (flag) a else a + 1)
    // Kotlin itself rejects this: the reified T of `from` would be the intersection of Long and String.
    Variant.<!MUST_BE_VARIANT_CHECKED, TYPE_INTERSECTION_AS_REIFIED_ERROR!>from<!>(if (flag) a else b)
    Variant.<!MUST_BE_VARIANT_CHECKED!>from<!>(if (flag) a else <!NOT_VARIANT_COMPATIBLE!>pattern<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(
        when (option) {
            0 -> a
            1 -> <!NOT_VARIANT_COMPATIBLE!>'1'<!>
            else -> {
                val local = b.length
                local
            }
        },
    )
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(try { a } catch (e: Exception) { <!NOT_VARIANT_COMPATIBLE!>pattern<!> })
}

fun nullSafety(box: Box?) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(box?.long())
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>box?.pattern()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(box?.long ?: 0L)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(box?.long ?: <!NOT_VARIANT_COMPATIBLE!>'0'<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>box!!.pattern<!>)
}

fun calls(flag: Boolean, box: Box) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>regexOf()<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(identity(1L))
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>identity(box.pattern)<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>("${box.long} as text")
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>((box.long))
    // Inline scope functions returning their lambda's result are checked per branch of that lambda.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(run { if (flag) 1 else <!NOT_VARIANT_COMPATIBLE!>regexOf()<!> })
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(box.let { it.long })
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(with(box) { <!NOT_VARIANT_COMPATIBLE!>pattern<!> })
    // `also` returns its receiver, not the lambda's result.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>box.also { it.long }<!>)
}

fun functionValues() {
    // A lambda or reference is a Kotlin function, never a Variant: use a Callable.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>{ 1 }<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>::regexOf<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!MUST_BE_VARIANT_CHECKED!>Callable<!> { 1 })
}
