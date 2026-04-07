package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import org.jetbrains.annotations.ApiStatus
import kotlin.contracts.contract
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@ExperimentalGodotKotlin
@ApiStatus.Internal
public fun variantTypeOf(kType: KType): Variant.Type = when (kType) {
    typeOf<Unit>() -> {
        GD.pushError(
            "Variant of Unit is not supported, returning Variant.Type.NIL as fallback, define the type explicitly",
        )
        Variant.Type.NIL
    }

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

    else -> {
        // fallback: GodotObject o script
        when (kType.classifier) {
            GodotObject::class -> Variant.Type.OBJECT
            GodotArray::class -> Variant.Type.ARRAY
            Dictionary::class -> Variant.Type.DICTIONARY
            else -> error("Unsupported type for Variant: $kType")
        }
    }
}

/** Compatible Variant.Type for kotlin String */
@PublishedApi
internal val stringToTypes: Array<Variant.Type> = arrayOf(STRING, STRING_NAME, NODE_PATH)

@ExperimentalGodotKotlin
@ApiStatus.Internal
public inline fun <@MustBeVariant reified T> checkVariantCompatibility(type: Variant.Type) {
    val expected = variantTypeOf(typeOf<T>())
    require(type == expected || (T::class == String::class && type in stringToTypes)) {
        "Variant.Type mismatch: expected=$expected but got=$type for T=${T::class}"
    }
}

/**
 * Convert a [element] to a [Variant] compatible with the given [type]
 * @param lenient If true, null elements are allowed; otherwise they are not allowed
 */
@ExperimentalGodotKotlin
@ApiStatus.Internal
public fun <@MustBeVariant T> checkVariantTypeAndConvert(
    element: T?,
    type: Variant.Type,
    lenient: Boolean = true,
): Variant {
    contract {
        returns() implies (
            element == null ||

                element is Boolean ||

                element is Int || element is Long ||

                element is Float || element is Double ||

                element is GodotString || element is String ||

                element is Vector2 ||
                element is Vector2i ||

                element is Rect2 ||
                element is Rect2i ||

                element is Vector3 ||
                element is Vector3i ||

                element is Vector4 ||
                element is Vector4i ||

                element is Transform2D ||
                element is Transform3D ||

                element is Plane ||
                element is Quaternion ||

                element is AABB ||
                element is Basis ||
                element is Projection ||

                element is Color ||

                element is StringName ||
                element is NodePath ||
                element is Rid ||

                element is GodotObject ||

                element is Callable ||
                element is Signal ||

                element is VariantDictionary ||
                element is VariantArray ||

                element is PackedByteArray ||
                element is PackedInt32Array ||
                element is PackedInt64Array ||

                element is PackedFloat32Array ||
                element is PackedFloat64Array ||

                element is PackedStringArray ||

                element is PackedVector2Array ||
                element is PackedVector3Array ||
                element is PackedVector4Array ||

                element is PackedColorArray
            )
    }

    val condition: Boolean
    var result: Variant? = null
    val expected: String

    when (type) {
        Variant.Type.NIL -> {
            condition = element == null
            expected = "null"
            result = Variant()
        }

        Variant.Type.BOOL -> {
            condition = element is Boolean?
            expected = "boolean"
            if (condition) result = element.asVariant()
        }

        Variant.Type.INT -> {
            condition = element is Int? || element is Long?
            expected = "int"
            if (condition) result = element?.toLong().asVariant()
        }

        Variant.Type.FLOAT -> {
            condition = element is Float? || element is Double?
            expected = "float"
            if (condition) result = element?.toDouble().asVariant()
        }

        Variant.Type.STRING -> {
            condition = element is GodotString? || element is String?
            expected = "string"
            if (element is GodotString?) {
                result = element.asVariant()
            } else if (element is String?) {
                result = element.asVariantString()
            }
        }

        Variant.Type.VECTOR2 -> {
            condition = element is Vector2?
            expected = "Vector2"
            if (condition) result = element.asVariant()
        }

        Variant.Type.VECTOR2I -> {
            condition = element is Vector2i
            expected = "Vector2i"
        }

        Variant.Type.RECT2 -> {
            condition = element is Rect2?
            expected = "Rect2"
            if (condition) result = element.asVariant()
        }

        Variant.Type.RECT2I -> {
            condition = element is Rect2i?
            expected = "Rect2i"
            if (condition) result = element.asVariant()
        }

        Variant.Type.VECTOR3 -> {
            condition = element is Vector3?
            expected = "Vector3"
            if (condition) result = element.asVariant()
        }

        Variant.Type.VECTOR3I -> {
            condition = element is Vector3i?
            expected = "Vector3i"
            if (condition) result = element.asVariant()
        }

        Variant.Type.TRANSFORM2D -> {
            condition = element is Transform2D?
            expected = "Transform2D"
            if (condition) result = element.asVariant()
        }

        Variant.Type.VECTOR4 -> {
            condition = element is Vector4?
            expected = "Vector4"
            if (condition) result = element.asVariant()
        }

        Variant.Type.VECTOR4I -> {
            condition = element is Vector4i?
            expected = "Vector4i"
            if (condition) result = element.asVariant()
        }

        Variant.Type.PLANE -> {
            condition = element is Plane?
            expected = "Plane"
            if (condition) result = element.asVariant()
        }

        Variant.Type.QUATERNION -> {
            condition = element is Quaternion?
            expected = "Quaternion"
            if (condition) result = element.asVariant()
        }

        Variant.Type.AABB -> {
            condition = element is AABB?
            expected = "AABB"
            if (condition) result = element.asVariant()
        }

        Variant.Type.BASIS -> {
            condition = element is Basis?
            expected = "Basis"
            if (condition) result = element.asVariant()
        }

        Variant.Type.TRANSFORM3D -> {
            condition = element is Transform3D?
            expected = "Transform3D"
            if (condition) result = element.asVariant()
        }

        Variant.Type.PROJECTION -> {
            condition = element is Projection?
            expected = "Projection"
            if (condition) result = element.asVariant()
        }

        Variant.Type.COLOR -> {
            condition = element is Color?
            expected = "Color"
            if (condition) result = element.asVariant()
        }

        Variant.Type.STRING_NAME -> {
            condition = element is StringName? || element is String?
            expected = "StringName"
            if (element is StringName?) {
                result = element.asVariant()
            } else if (element is String?) {
                result = element.asVariantStringName()
            }
        }

        Variant.Type.NODE_PATH -> {
            condition = element is NodePath? || element is String?
            expected = "NodePath"
            if (element is NodePath?) {
                result = element.asVariant()
            } else if (element is String?) {
                result = element.asVariantNodePath()
            }
        }

        Variant.Type.RID -> {
            condition = element is Rid?
            expected = "Rid"
            if (condition) result = element.asVariant()
        }

        Variant.Type.OBJECT -> {
            condition = element is GodotObject?
            expected = "GodotObject"
            if (condition) result = element.asVariant()
        }

        Variant.Type.CALLABLE -> {
            condition = element is Callable?
            expected = "Callable"
            if (condition) result = element.asVariant()
        }

        Variant.Type.SIGNAL -> {
            condition = element is Signal?
            expected = "Signal"
            if (condition) result = element.asVariant()
        }

        Variant.Type.DICTIONARY -> {
            condition = element is Dictionary<*, *>?
            expected = "Dictionary"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.ARRAY -> {
            condition = element is GodotArray<*>?
            expected = "Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_BYTE_ARRAY -> {
            condition = element is PackedByteArray?
            expected = "PackedByteArray"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_INT32_ARRAY -> {
            condition = element is PackedInt32Array?
            expected = "PackedInt32Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_INT64_ARRAY -> {
            condition = element is PackedInt64Array?
            expected = "PackedInt64Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_FLOAT32_ARRAY -> {
            condition = element is PackedFloat32Array?
            expected = "PackedFloat32Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_FLOAT64_ARRAY -> {
            condition = element is PackedFloat64Array?
            expected = "PackedFloat64Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_STRING_ARRAY -> {
            condition = element is PackedStringArray?
            expected = "PackedStringArray"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_VECTOR2_ARRAY -> {
            condition = element is PackedVector2Array?
            expected = "PackedVector2Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_VECTOR3_ARRAY -> {
            condition = element is PackedVector3Array?
            expected = "PackedVector3Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_COLOR_ARRAY -> {
            condition = element is PackedColorArray?
            expected = "PackedColorArray"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.PACKED_VECTOR4_ARRAY -> {
            condition = element is PackedVector4Array?
            expected = "PackedVector4Array"
            if (condition) result = element?.asVariant()
        }

        Variant.Type.MAX -> error("Invalid type, $type is not a valid to compare and convert to Variant")
    }

    require(condition || (lenient && element == null)) {
        "Expected $expected, got $element for type $type"
    }

    return result ?: if (lenient) Variant() else error("Expected $expected, got null for type $type")
}
