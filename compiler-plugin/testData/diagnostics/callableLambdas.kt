// RUN_PIPELINE_TILL: FRONTEND
// White box: a lambda's return type follows the expected one (`Any?` for the zero-argument Callable), so the checker
// inspects what it actually returns: implicit and labeled returns, every branch, but not nested lambdas.

import io.github.kingg22.godot.api.builtin.*

fun regexProducer(): Regex = Regex("x")
fun intProducer(): Int = 1

fun arities() {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { count: Int -> count }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { count: Int, name: String -> name.repeat(count) }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { x: Double, y: Double, z: Double -> Vector3() }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { x: Double, y: Double, z: Char -> }
}

fun returnedValues(flag: Boolean, option: Int, nullable: Regex?) {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>regexProducer()<!> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> {
        if (flag) return@Callable <!NOT_VARIANT_COMPATIBLE!>Regex("early")<!>
        1L
    }
    // `if`, `when`, `try` and `?:` are typed as their branches' common supertype (Any): each branch is checked.
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { if (flag) 1 else <!NOT_VARIANT_COMPATIBLE!>Regex("x")<!> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> {
        when (option) {
            0 -> "zero"
            1 -> <!NOT_VARIANT_COMPATIBLE!>'1'<!>
            else -> Vector2()
        }
    }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { try { 1 } catch (e: Exception) { <!NOT_VARIANT_COMPATIBLE!>e<!> } }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>nullable<!> ?: "default" }
    // Throwing is fine: Nothing never produces a value.
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { if (flag) error("unreachable") else 1 }
}

fun nestedLambdas() {
    // The inner lambda returns a Regex to `map`, the outer one returns an Int to the Callable.
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> {
        listOf(1).map { Regex("$it") }
        1
    }
}

fun nonLambdaFunctions(regexes: () -> Regex, ints: () -> Int) {
    // References and function values are checked through their type instead.
    <!MUST_BE_VARIANT_CHECKED!>Callable<!>(<!NOT_VARIANT_COMPATIBLE!>::regexProducer<!>)
    <!MUST_BE_VARIANT_CHECKED!>Callable<!>(::intProducer)
    <!MUST_BE_VARIANT_CHECKED!>Callable<!>(<!NOT_VARIANT_COMPATIBLE!>regexes<!>)
    <!MUST_BE_VARIANT_CHECKED!>Callable<!>(ints)
}

fun explicitTypeArguments() {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!><Int, String> { count -> "$count" }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!><<!NOT_VARIANT_COMPATIBLE!>Byte<!>, Unit> { }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!><Int, <!NOT_VARIANT_COMPATIBLE!>Char<!>> { 'c' }
}
