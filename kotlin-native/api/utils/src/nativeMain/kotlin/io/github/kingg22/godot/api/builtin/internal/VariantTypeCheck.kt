package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Convert a [KType] to a [Variant.Type]
 *
 * **Safety**:
 * - [Unit] is not supported, returns [Variant.Type.NIL] as fallback
 * - Inheritance of [GodotObject] is not supported
 * - [Variant] is not supported
 */
@Deprecated("This function doesn't support inheritance of GodotObject, prefers others methods", level = WARNING)
@ExperimentalGodotKotlin
@ApiStatus.Internal
public fun variantTypeOf(kType: KType): Variant.Type = when (kType) {
    typeOf<Unit>() -> Variant.Type.NIL
    typeOf<Boolean>() -> Variant.Type.BOOL
    typeOf<Long>(), typeOf<Int>() -> Variant.Type.INT
    typeOf<Double>(), typeOf<Float>() -> Variant.Type.FLOAT
    typeOf<String>() -> Variant.Type.STRING
    typeOf<GodotString>() -> Variant.Type.STRING
    typeOf<Vector2>() -> Variant.Type.VECTOR2
    typeOf<Vector2i>() -> Variant.Type.VECTOR2I
    typeOf<Rect2>() -> Variant.Type.RECT2
    typeOf<Rect2i>() -> Variant.Type.RECT2I
    typeOf<Vector3>() -> Variant.Type.VECTOR3
    typeOf<Vector3i>() -> Variant.Type.VECTOR3I
    typeOf<Transform2D>() -> Variant.Type.TRANSFORM2D
    typeOf<Vector4>() -> Variant.Type.VECTOR4
    typeOf<Vector4i>() -> Variant.Type.VECTOR4I
    typeOf<Plane>() -> Variant.Type.PLANE
    typeOf<Quaternion>() -> Variant.Type.QUATERNION
    typeOf<AABB>() -> Variant.Type.AABB
    typeOf<Basis>() -> Variant.Type.BASIS
    typeOf<Transform3D>() -> Variant.Type.TRANSFORM3D
    typeOf<Projection>() -> Variant.Type.PROJECTION
    typeOf<Color>() -> Variant.Type.COLOR
    typeOf<StringName>() -> Variant.Type.STRING_NAME
    typeOf<NodePath>() -> Variant.Type.NODE_PATH
    typeOf<RID>() -> Variant.Type.RID
    typeOf<GodotObject>() -> Variant.Type.OBJECT
    typeOf<Callable>() -> Variant.Type.CALLABLE
    typeOf<Signal>() -> Variant.Type.SIGNAL
    typeOf<GodotArray<*>>() -> Variant.Type.ARRAY
    typeOf<Dictionary<*, *>>() -> Variant.Type.DICTIONARY
    typeOf<PackedByteArray>() -> Variant.Type.PACKED_BYTE_ARRAY
    typeOf<PackedInt32Array>() -> Variant.Type.PACKED_INT32_ARRAY
    typeOf<PackedInt64Array>() -> Variant.Type.PACKED_INT64_ARRAY
    typeOf<PackedFloat32Array>() -> Variant.Type.PACKED_FLOAT32_ARRAY
    typeOf<PackedFloat64Array>() -> Variant.Type.PACKED_FLOAT64_ARRAY
    typeOf<PackedStringArray>() -> Variant.Type.PACKED_STRING_ARRAY
    typeOf<PackedVector2Array>() -> Variant.Type.PACKED_VECTOR2_ARRAY
    typeOf<PackedVector3Array>() -> Variant.Type.PACKED_VECTOR3_ARRAY
    typeOf<PackedColorArray>() -> Variant.Type.PACKED_COLOR_ARRAY
    typeOf<PackedVector4Array>() -> Variant.Type.PACKED_VECTOR4_ARRAY
    else -> error("Unsupported type for Variant: $kType, must be explicitly type defined")
}
