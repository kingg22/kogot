package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin

/**
 * Returns the value of this [Variant].
 *
 * Prefers explicit converter method over this function.
 *
 * Doesn't return [kotlin.String]!!
 *
 * @throws [IllegalStateException] if the type of variant is [MAX][Variant.Type.MAX]
 */
@ExperimentalGodotKotlin
public fun Variant.getValue(): @MustBeVariant Any? = when (getType()) {
    NIL -> null
    BOOL -> asBool()
    INT -> asInt()
    FLOAT -> asFloat()
    STRING -> asString()
    STRING_NAME -> asStringName()
    NODE_PATH -> asNodePath()
    VECTOR2 -> asVector2()
    VECTOR2I -> asVector2i()
    RECT2 -> asRect2()
    RECT2I -> asRect2i()
    VECTOR3 -> asVector3()
    VECTOR3I -> asVector3i()
    TRANSFORM2D -> asTransform2D()
    VECTOR4 -> asVector4()
    VECTOR4I -> asVector4i()
    PLANE -> asPlane()
    QUATERNION -> asQuaternion()
    Variant.Type.AABB -> asAabb()
    BASIS -> asBasis()
    TRANSFORM3D -> asTransform3D()
    PROJECTION -> asProjection()
    COLOR -> asColor()
    Variant.Type.RID -> asRid()
    OBJECT -> asObject()
    CALLABLE -> asCallable()
    SIGNAL -> asSignal()
    DICTIONARY -> asDictionary()
    ARRAY -> asArray()
    PACKED_BYTE_ARRAY -> asPackedByteArray()
    PACKED_INT32_ARRAY -> asPackedInt32Array()
    PACKED_INT64_ARRAY -> asPackedInt64Array()
    PACKED_FLOAT32_ARRAY -> asPackedFloat32Array()
    PACKED_FLOAT64_ARRAY -> asPackedFloat64Array()
    PACKED_STRING_ARRAY -> asPackedStringArray()
    PACKED_VECTOR2_ARRAY -> asPackedVector2Array()
    PACKED_VECTOR3_ARRAY -> asPackedVector3Array()
    PACKED_COLOR_ARRAY -> asPackedColorArray()
    PACKED_VECTOR4_ARRAY -> asPackedVector4Array()
    MAX -> error("Invalid Variant.Type.MAX")
}
