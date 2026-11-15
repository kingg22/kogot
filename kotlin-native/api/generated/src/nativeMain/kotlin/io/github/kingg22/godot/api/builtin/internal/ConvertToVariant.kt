package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode

/** See [anyToVariant] */
@UsedFromCodegenGeneratedCode // only in this module until internals published api when shared internals are beta/stable
// https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0451-shared-internals.md
@ExperimentalGodotKotlin
@PublishedApi
internal fun anyToVariantOrNull(element: @MustBeVariant Any?): Variant? = when (element) {
    is Variant -> element
    null, is Unit -> Variant()
    is Boolean -> element.toVariant()
    is Int -> element.toVariant()
    is Short -> element.toLong().toVariant()
    is Long -> element.toVariant()
    is Float -> element.toVariant()
    is Double -> element.toVariant()
    is String -> GodotString(element).use { it.toVariant() }
    is GodotString -> element.toVariant()
    is StringName -> element.toVariant()
    is NodePath -> element.toVariant()
    is Vector2 -> element.toVariant()
    is Vector2i -> element.toVariant()
    is Rect2 -> element.toVariant()
    is Rect2i -> element.toVariant()
    is Vector3 -> element.toVariant()
    is Vector3i -> element.toVariant()
    is Vector4 -> element.toVariant()
    is Vector4i -> element.toVariant()
    is Transform2D -> element.toVariant()
    is Transform3D -> element.toVariant()
    is Plane -> element.toVariant()
    is Quaternion -> element.toVariant()
    is AABB -> element.toVariant()
    is Basis -> element.toVariant()
    is Projection -> element.toVariant()
    is Color -> element.toVariant()
    is Rid -> element.toVariant()
    is Callable -> element.toVariant()
    is Signal -> element.toVariant()
    is GodotObject -> element.toVariant()
    is Dictionary<*, *> -> element.toVariant()
    is GodotArray<*> -> element.toVariant()
    is PackedByteArray -> element.toVariant()
    is PackedInt32Array -> element.toVariant()
    is PackedInt64Array -> element.toVariant()
    is PackedFloat32Array -> element.toVariant()
    is PackedFloat64Array -> element.toVariant()
    is PackedStringArray -> element.toVariant()
    is PackedVector2Array -> element.toVariant()
    is PackedVector3Array -> element.toVariant()
    is PackedVector4Array -> element.toVariant()
    is PackedColorArray -> element.toVariant()
    is Throwable -> throw IllegalStateException("Cannot convert Throwable to Variant", element)
    else -> null
}

/**
 * Converts an arbitrary type to a [Variant].
 *
 * Equivalent to call `toVariant()` on the type.
 *
 * **Safety**:
 * - `null` is converted to [Variant.Type.NIL]
 * - [Variant] is returned the same Variant, not a copy
 * - [Unit] is converted to [Variant.Type.NIL]
 * - [Throwable] is not supported, throws [IllegalStateException] with it as cause
 * - [CPointer][kotlinx.cinterop.COpaquePointer] is not supported
 *
 * @param element The element to convert, [must be a variant type compatible][MustBeVariant].
 * @throws [IllegalStateException] if the type of variant is [MAX][Variant.Type.MAX] or [Throwable] or unsupported type.
 * @see Variant
 * @see toVariant
 */
@ExperimentalGodotKotlin
@PublishedApi
internal fun anyToVariant(element: @MustBeVariant Any?): Variant =
    anyToVariantOrNull(element) ?: error("Unsupported type for Variant: $element (${element?.let { it::class }})")

/**
 * Runs [block] with [element] as a [Variant] and frees that `Variant` afterwards, unless it is [element] itself: a
 * `Variant` passed in is the caller's and stays open. Used by the generated `Any?` overloads of the `Variant`
 * parameters, which used to leak the `Variant` of every argument they converted (#219).
 */
@UsedFromCodegenGeneratedCode
@ExperimentalGodotKotlin
@PublishedApi
internal inline fun <R> withVariant(element: @MustBeVariant Any?, block: (Variant) -> R): R {
    val variant = anyToVariant(element)
    try {
        return block(variant)
    } finally {
        if (variant !== element) variant.close()
    }
}

/**
 * [withVariant] for a variadic call: [block] receives [first] and every element of [rest] as one array of
 * `Variant`s, and the ones converted here are freed afterwards. If a conversion throws, the ones already made are
 * freed too.
 */
@UsedFromCodegenGeneratedCode
@ExperimentalGodotKotlin
@PublishedApi
internal inline fun <R> withVariants(
    first: @MustBeVariant Any?,
    rest: Array<out @MustBeVariant Any?>,
    block: (Array<Variant>) -> R,
): R {
    val inputs = arrayOf(first, *rest)
    val variants = arrayOfNulls<Variant>(inputs.size)
    try {
        for (i in inputs.indices) variants[i] = anyToVariant(inputs[i])
        @Suppress("UNCHECKED_CAST")
        return block(variants as Array<Variant>)
    } finally {
        for (i in inputs.indices) {
            val variant = variants[i] ?: break
            if (variant !== inputs[i]) variant.close()
        }
    }
}
