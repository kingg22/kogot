// RUN_PIPELINE_TILL: FRONTEND
// Black box: `signal<F>()` checks every parameter of its function type at the declaration, and a property delegated
// to `signal` must carry @RegisterSignal.

import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.*
import io.github.kingg22.godot.api.signal.signal

typealias Hit = (damage: Long, by: Node?) -> Unit
typealias Broken = (pattern: Regex) -> Unit

class Player : Node2D() {
    // Accepted: primitives, builtins, Variant, Godot objects (engine or user), nullable objects, aliases.
    @RegisterSignal val ready by signal()
    @RegisterSignal val scored by <!MUST_BE_VARIANT_CHECKED!>signal<!><(points: Int, total: Long, ratio: Double, name: String) -> Unit>()
    @RegisterSignal val moved by <!MUST_BE_VARIANT_CHECKED!>signal<!><(position: Vector2, raw: Variant, target: Node2D?, owner: Player) -> Unit>()
    @RegisterSignal val hit by <!MUST_BE_VARIANT_CHECKED!>signal<!><Hit>()

    // Rejected at the declaration, even before any emit or connect.
    @RegisterSignal val matched by <!MUST_BE_VARIANT_CHECKED!>signal<!><<!NOT_VARIANT_COMPATIBLE!>(pattern: Regex) -> Unit<!>>()
    @RegisterSignal val items by <!MUST_BE_VARIANT_CHECKED!>signal<!><<!NOT_VARIANT_COMPATIBLE!>(list: List<Int>) -> Unit<!>>()
    @RegisterSignal val small by <!MUST_BE_VARIANT_CHECKED!>signal<!><<!NOT_VARIANT_COMPATIBLE!>(value: Byte) -> Unit<!>>()
    @RegisterSignal val aliased by <!MUST_BE_VARIANT_CHECKED!>signal<!><<!NOT_VARIANT_COMPATIBLE!>Broken<!>>()
    @RegisterSignal val both by <!MUST_BE_VARIANT_CHECKED!>signal<!><<!NOT_VARIANT_COMPATIBLE, NOT_VARIANT_COMPATIBLE!>(a: Regex, ok: Long, b: Regex) -> Unit<!>>()

    // Open types are only known at runtime.
    @RegisterSignal val anything by <!MUST_BE_VARIANT_CHECKED!>signal<!><<!MUST_BE_VARIANT_UNKNOWN_TYPE!>(value: Any?) -> Unit<!>>()

    // Not registered: Godot would reject every emit and connect.
    val <!SIGNAL_NOT_REGISTERED!>forgotten<!> by signal()
    val <!SIGNAL_NOT_REGISTERED!>forgottenTyped<!> by <!MUST_BE_VARIANT_CHECKED!>signal<!><(value: Long) -> Unit>()
}
