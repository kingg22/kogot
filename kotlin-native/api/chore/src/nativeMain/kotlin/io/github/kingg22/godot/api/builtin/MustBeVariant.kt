package io.github.kingg22.godot.api.builtin

/**
 * Marks a type that must be _Variant compatible_: convertible to and from a Godot `Variant`.
 *
 * The same rules apply in both directions, values sent to Godot (arguments, return values) and values received
 * from it (e.g. `Callable` lambda parameters). Enforced at compile time by the kogot compiler plugin.
 *
 * Compatible types are `null`, [Unit] (`NIL`), [Boolean], `Variant`, `GodotObject` subclasses and the Godot builtins
 * (`Vector2`, `GodotString`, `Callable`, `Packed*Array`, ...), plus these Kotlin types stored as a Godot type:
 *
 * | Kotlin              | Godot `Variant.Type` | Sent to Godot | Received from Godot           |
 * |---------------------|----------------------|---------------|-------------------------------|
 * | [Long]              | `INT` (64-bit)       | as is         | as is                         |
 * | [Int], [Short]      | `INT` (64-bit)       | widened       | narrowed, like [Long.toInt]   |
 * | [Double]            | `FLOAT` (64-bit)     | as is         | as is                         |
 * | [Float]             | `FLOAT` (64-bit)     | widened       | narrowed, like [Double.toFloat] |
 * | [String]            | `STRING`             | as `GodotString` | from `GodotString`         |
 *
 * [Byte] is deliberately unsupported: Kotlin bytes are signed while Godot bytes (`PackedByteArray`) are unsigned,
 * so a Byte stored as `INT` would not mean "a Godot byte". Use `Int` or `PackedByteArray` instead.
 *
 * Open types (`Any`, an unconstrained type parameter, `Number`...) are accepted and checked at runtime (slow path).
 */
@Retention(BINARY)
@Target(TYPE, TYPE_PARAMETER)
@MustBeDocumented
public annotation class MustBeVariant
