package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.Variant

/**
 * Returns the value of this [io.github.kingg22.godot.api.builtin.Variant].
 *
 * Prefers explicit converter method over this function.
 *
 * Or use `val value = variant.getValue<T>()`
 *
 * Or use `val value = variant.getValueOrNull<T>()`
 *
 * Or use `val value: T by variant`
 *
 * **Safety**:
 * - Nevers returns [String]
 * - Never downcasting to [Int], [Float], [Short], [Byte]
 * - Can return `null` if the type is [io.github.kingg22.godot.api.builtin.Variant.Type.NIL]
 * - Never returns specific types like [Texture][io.github.kingg22.godot.api.core.refcounted.Texture] always going to be
 * [Object][io.github.kingg22.godot.api.core.GodotObject] if the type is [io.github.kingg22.godot.api.builtin.Variant.Type.OBJECT]
 * - Always returns [io.github.kingg22.godot.api.builtin.VariantArray] for [io.github.kingg22.godot.api.builtin.Variant.Type.ARRAY]
 * - Always returns [io.github.kingg22.godot.api.builtin.VariantDictionary] for [io.github.kingg22.godot.api.builtin.Variant.Type.DICTIONARY]
 * - Throws [IllegalStateException] if the type of variant is [io.github.kingg22.godot.api.builtin.Variant.Type.MAX]
 *
 * @see io.github.kingg22.godot.api.builtin.getValue
 * @see io.github.kingg22.godot.api.builtin.getValueOrNull
 */
@ExperimentalGodotKotlin
@PublishedApi
internal val Variant.value: @MustBeVariant Any? get() = when (getType()) {
    NIL -> null
    BOOL -> toBool()
    INT -> toInt()
    FLOAT -> toFloat()
    STRING -> toGodotString()
    STRING_NAME -> toStringName()
    NODE_PATH -> toNodePath()
    VECTOR2 -> toVector2()
    VECTOR2I -> toVector2i()
    RECT2 -> toRect2()
    RECT2I -> toRect2i()
    VECTOR3 -> toVector3()
    VECTOR3I -> toVector3i()
    TRANSFORM2D -> toTransform2D()
    VECTOR4 -> toVector4()
    VECTOR4I -> toVector4i()
    PLANE -> toPlane()
    QUATERNION -> toQuaternion()
    Variant.Type.AABB -> toAabb()
    BASIS -> toBasis()
    TRANSFORM3D -> toTransform3D()
    PROJECTION -> toProjection()
    COLOR -> toColor()
    Variant.Type.RID -> toRid()
    OBJECT -> toObject()
    CALLABLE -> toCallable()
    SIGNAL -> toSignal()
    DICTIONARY -> toDictionary()
    ARRAY -> toArray()
    PACKED_BYTE_ARRAY -> toPackedByteArray()
    PACKED_INT32_ARRAY -> toPackedInt32Array()
    PACKED_INT64_ARRAY -> toPackedInt64Array()
    PACKED_FLOAT32_ARRAY -> toPackedFloat32Array()
    PACKED_FLOAT64_ARRAY -> toPackedFloat64Array()
    PACKED_STRING_ARRAY -> toPackedStringArray()
    PACKED_VECTOR2_ARRAY -> toPackedVector2Array()
    PACKED_VECTOR3_ARRAY -> toPackedVector3Array()
    PACKED_COLOR_ARRAY -> toPackedColorArray()
    PACKED_VECTOR4_ARRAY -> toPackedVector4Array()
    MAX -> error("Invalid Variant.Type.MAX")
}
