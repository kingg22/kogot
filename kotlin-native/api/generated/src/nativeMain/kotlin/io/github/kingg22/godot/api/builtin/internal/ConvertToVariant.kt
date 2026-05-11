package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import org.jetbrains.annotations.ApiStatus
import kotlin.contracts.contract

/** See [anyToVariant] */
@UsedFromCodegenGeneratedCode
@ExperimentalGodotKotlin
@ApiStatus.Internal
public fun anyToVariantOrNull(element: @MustBeVariant Any?): Variant? {
    contract { returns() implies (element != null) }
    // Manejo explícito de null → Variant NIL
    if (element == null) return Variant()

    return when (element) {
        is Variant -> element
        is Unit -> Variant()
        is Boolean -> element.toVariant()
        is Int -> element.toVariant()
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
}

/**
 * Converts an arbitrary type to a [Variant].
 *
 * Equivalent to call `asVariant()` on the type.
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
 * @see asVariant
 */
@ExperimentalGodotKotlin
@ApiStatus.Internal
public fun anyToVariant(element: @MustBeVariant Any?): Variant {
    contract { returns() implies (element != null) }
    return anyToVariantOrNull(element) ?: error("Unsupported type for Variant: $element (${element::class})")
}
