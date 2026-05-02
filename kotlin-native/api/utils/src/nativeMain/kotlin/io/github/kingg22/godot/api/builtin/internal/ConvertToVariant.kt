package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import org.jetbrains.annotations.ApiStatus

/**
 * Converts an arbitrary type to a [Variant].
 *
 * Equivalent to call `asVariant()` on the type.
 *
 * **Safety**:
 * - `null` is converted to [Variant.Type.NIL]
 * - [Variant] is returned the same Variant, not a copy
 * - [Unit] is converted to [Variant.Type.NIL]
 * - [Throwable] is not supported, throws [IllegalStateException]
 *
 * @param element The element to convert, [must be a variant type compatible][MustBeVariant].
 * @see Variant
 * @see GodotObject.asVariant
 */
@ExperimentalGodotKotlin
@ApiStatus.Internal
public fun <T> anyToVariant(element: @MustBeVariant T?): Variant {
    // Manejo explícito de null → Variant NIL
    if (element == null) return Variant()

    return when (element) {
        is Variant -> element
        is Unit -> Variant()
        is Boolean -> element.asVariant()
        is Int -> element.asVariant()
        is Long -> element.asVariant()
        is Float -> element.asVariant()
        is Double -> element.asVariant()
        is String -> element.asVariantString()
        is GodotString -> element.asVariant()
        is StringName -> element.asVariant()
        is NodePath -> element.asVariant()
        is Vector2 -> element.asVariant()
        is Vector2i -> element.asVariant()
        is Rect2 -> element.asVariant()
        is Rect2i -> element.asVariant()
        is Vector3 -> element.asVariant()
        is Vector3i -> element.asVariant()
        is Vector4 -> element.asVariant()
        is Vector4i -> element.asVariant()
        is Transform2D -> element.asVariant()
        is Transform3D -> element.asVariant()
        is Plane -> element.asVariant()
        is Quaternion -> element.asVariant()
        is AABB -> element.asVariant()
        is Basis -> element.asVariant()
        is Projection -> element.asVariant()
        is Color -> element.asVariant()
        is Rid -> element.asVariant()
        is Callable -> element.asVariant()
        is Signal -> element.asVariant()
        is GodotObject -> element.asVariant()
        is Dictionary<*, *> -> element.asVariant()
        is GodotArray<*> -> element.asVariant()
        is PackedByteArray -> element.asVariant()
        is PackedInt32Array -> element.asVariant()
        is PackedInt64Array -> element.asVariant()
        is PackedFloat32Array -> element.asVariant()
        is PackedFloat64Array -> element.asVariant()
        is PackedStringArray -> element.asVariant()
        is PackedVector2Array -> element.asVariant()
        is PackedVector3Array -> element.asVariant()
        is PackedVector4Array -> element.asVariant()
        is PackedColorArray -> element.asVariant()
        is Throwable -> throw IllegalStateException("Cannot convert Throwable to Variant", element)
        else -> error("Unsupported type for Variant: $element (${element::class})")
    }
}
