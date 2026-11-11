package io.github.kingg22.godot.api.builtin

import kotlin.reflect.KClass

/**
 * How the elements of a [GodotArray] or [Dictionary] cross between a [Variant] and the Kotlin type [T].
 *
 * A codec is fixed when the collection is built and never changes, so every wrapper derived from it reads and writes
 * the same way. It reuses [getValue] and [toVariant], so it is as strict as they are: an element that is not a [T]
 * throws instead of being dropped, and `NIL` is only valid for a nullable [T].
 *
 * It is internal on purpose: a codec that does not match the element type would let a wrong element through, so the
 * collections are only built by the factory functions, which pick the codec from the reified type.
 */
@PublishedApi
internal class ElementCodec<@MustBeVariant T> @PublishedApi internal constructor(
    private val reader: (Variant) -> T,
    private val writer: (T) -> Variant,
    /** The Godot element type the engine side of the collection is fixed to; `NIL` leaves it untyped. */
    val godotType: Variant.Type = Variant.Type.NIL,
) {
    /**
     * Reads [variant] as a [T], taking ownership of it: it is closed unless [T] is [Variant] itself, which returns it.
     *
     * @throws ClassCastException if the element is not a [T].
     */
    fun read(variant: Variant): T = reader(variant)

    /** Writes [value] as a new [Variant] that the caller owns and must close. */
    fun write(value: T): Variant = writer(value)

    @PublishedApi
    internal companion object {
        /** The codec of the untyped containers: elements stay [Variant]s. */
        val VARIANT: ElementCodec<Variant> = ElementCodec({ it }, { Variant(it.rawPtr) })

        /**
         * The codec for [T]. Collections nested in [T] are read as `GodotArray<Variant>` or
         * `Dictionary<Variant, Variant>`: the engine keeps no inner element type, so it is not checked here.
         */
        @PublishedApi
        @Suppress("UNCHECKED_CAST")
        internal inline fun <@MustBeVariant reified T> of(): ElementCodec<T> {
            if (T::class == Variant::class) return VARIANT as ElementCodec<T>

            return ElementCodec(
                reader = { variant -> variant.use { it.getValue<T>() } },
                writer = { it.toVariant() },
                godotType = godotTypeOf(T::class, null is T),
            )
        }
    }
}

/**
 * The Godot type a collection of [kClass] is typed with, or `NIL` when it stays untyped: [Variant] and `Any`, a
 * nullable element (typed arrays of values cannot hold `NIL`), and objects (their class name is not known here).
 * Nested arrays and dictionaries are typed as `ARRAY` and `DICTIONARY` only: the engine keeps no inner type.
 */
@PublishedApi
internal fun godotTypeOf(kClass: KClass<*>, nullable: Boolean): Variant.Type = if (nullable) {
    Variant.Type.NIL
} else {
    when (kClass) {
        Boolean::class -> Variant.Type.BOOL
        Long::class, Int::class, Short::class -> Variant.Type.INT
        Double::class, Float::class -> Variant.Type.FLOAT
        String::class -> Variant.Type.STRING
        StringName::class -> Variant.Type.STRING_NAME
        NodePath::class -> Variant.Type.NODE_PATH
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
        Aabb::class -> Variant.Type.AABB
        Basis::class -> Variant.Type.BASIS
        Transform3D::class -> Variant.Type.TRANSFORM3D
        Projection::class -> Variant.Type.PROJECTION
        Color::class -> Variant.Type.COLOR
        Rid::class -> Variant.Type.RID
        Callable::class -> Variant.Type.CALLABLE
        Signal::class -> Variant.Type.SIGNAL
        Dictionary::class -> Variant.Type.DICTIONARY
        GodotArray::class -> Variant.Type.ARRAY
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
        else -> Variant.Type.NIL
    }
}
