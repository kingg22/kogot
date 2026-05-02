package io.github.kingg22.godot.api.builtin

/**
 * Marks a type must be [variant type][Variant.Type] compatible.
 *
 * This annotation is used to enforce type safety in the API.
 * In the future, it can be enforced by a compiler plugin.
 *
 * APIs marked with this annotation must be used with [Variant]s,
 * so no explicit Variant, it entails a slow path to determinate the effective type.
 * [Kotlin String][String] is not a variant type, can be or not converted to [Variant] of [GodotString] with [Variant.Type.STRING].
 *
 * @see Variant
 * @see Variant.Type
 */
@Retention(BINARY)
@Target(TYPE, TYPE_PARAMETER)
@MustBeDocumented
public annotation class MustBeVariant
