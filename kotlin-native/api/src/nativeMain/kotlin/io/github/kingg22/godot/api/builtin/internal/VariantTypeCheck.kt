package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import org.jetbrains.annotations.ApiStatus
import kotlin.contracts.contract
import kotlin.reflect.KClass

@ExperimentalGodotKotlin
@ApiStatus.Internal
public fun <T : Any> variantTypeOf(kClass: KClass<out T>): Variant.Type = when (kClass) {
    Unit::class -> {
        GD.pushError(
            "Variant of Unit is not supported, returning Variant.Type.NIL as fallback, define the type explicitly",
        )
        Variant.Type.NIL
    }

    Boolean::class -> Variant.Type.BOOL

    Long::class, Int::class -> Variant.Type.INT

    Double::class, Float::class -> Variant.Type.FLOAT

    String::class -> Variant.Type.STRING

    GodotString::class -> Variant.Type.STRING

    Vector2::class -> Variant.Type.VECTOR2

    Vector2i::class -> Variant.Type.VECTOR2I

    Rect2::class -> Variant.Type.RECT2

    Rect2i::class -> Variant.Type.RECT2I

    Vector3::class -> Variant.Type.VECTOR3

    Vector3i::class -> Variant.Type.VECTOR3I

    Transform2D::class -> Variant.Type.TRANSFORM2D

    Vector4::class -> Variant.Type.VECTOR4

    Vector4i::class -> Variant.Type.VECTOR4I

    Plane::class -> Variant.Type.PLANE

    Quaternion::class -> Variant.Type.QUATERNION

    AABB::class -> Variant.Type.AABB

    Basis::class -> Variant.Type.BASIS

    Transform3D::class -> Variant.Type.TRANSFORM3D

    Projection::class -> Variant.Type.PROJECTION

    Color::class -> Variant.Type.COLOR

    StringName::class -> Variant.Type.STRING_NAME

    NodePath::class -> Variant.Type.NODE_PATH

    RID::class -> Variant.Type.RID

    Callable::class -> Variant.Type.CALLABLE

    Signal::class -> Variant.Type.SIGNAL

    GodotArray::class -> Variant.Type.ARRAY

    Dictionary::class -> Variant.Type.DICTIONARY

    PackedByteArray::class -> Variant.Type.PACKED_BYTE_ARRAY

    PackedInt32Array::class -> Variant.Type.PACKED_INT32_ARRAY

    PackedInt64Array::class -> Variant.Type.PACKED_INT64_ARRAY

    PackedFloat32Array::class -> Variant.Type.PACKED_FLOAT32_ARRAY

    PackedFloat64Array::class -> Variant.Type.PACKED_FLOAT64_ARRAY

    PackedStringArray::class -> Variant.Type.PACKED_STRING_ARRAY

    PackedVector2Array::class -> Variant.Type.PACKED_VECTOR2_ARRAY

    PackedVector3Array::class -> Variant.Type.PACKED_VECTOR3_ARRAY

    PackedColorArray::class -> Variant.Type.PACKED_COLOR_ARRAY

    PackedVector4Array::class -> Variant.Type.PACKED_VECTOR4_ARRAY

    else -> {
        // fallback: GodotObject o script
        when {
            GodotObject::class.isInstance(kClass) -> {
                Variant.Type.OBJECT
            }

            GodotArray::class.isInstance(kClass) -> Variant.Type.ARRAY

            Dictionary::class.isInstance(kClass) -> Variant.Type.DICTIONARY

            else -> {
                error("Unsupported type for Variant: ${kClass.simpleName} $kClass")
            }
        }
    }
}

/** Compatible Variant.Type for kotlin String */
@PublishedApi
internal val stringToTypes: Array<Variant.Type> = arrayOf(STRING, STRING_NAME, NODE_PATH)

@ExperimentalGodotKotlin
@ApiStatus.Internal
public inline fun <reified T : Any> checkVariantCompatibility(type: Variant.Type) {
    val expected = variantTypeOf(T::class)
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
public inline fun <reified T> checkVariantTypeAndConvert(
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
        "Expected $expected, got $element of ${T::class.simpleName} for type $type"
    }

    return result ?: if (lenient) Variant() else error("Expected $expected, got null for type $type")
}
