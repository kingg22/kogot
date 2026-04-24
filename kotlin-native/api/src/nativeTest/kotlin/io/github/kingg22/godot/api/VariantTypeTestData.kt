package io.github.kingg22.godot.api

import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Shared test data for variantType mappings across all signal and builtin tests.
 * Centralizing this prevents duplication and ensures consistency.
 */
object VariantTypeTestData {

    /**
     * Test case for type mapping verification.
     * @param kotlinType The Kotlin type to test
     * @param expectedVariantType The expected Variant.Type mapping
     * @param displayName Human-readable name for test output
     */
    data class TypeCase(val kotlinType: KType, val expectedVariantType: Variant.Type, val displayName: String)

    // ── Primitive Types ─────────────────────────────────────────────────────────

    fun primitiveTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Boolean>(), Variant.Type.BOOL, "Boolean"),
        TypeCase(typeOf<Int>(), Variant.Type.INT, "Int"),
        TypeCase(typeOf<Long>(), Variant.Type.INT, "Long"),
        TypeCase(typeOf<Float>(), Variant.Type.FLOAT, "Float"),
        TypeCase(typeOf<Double>(), Variant.Type.FLOAT, "Double"),
        TypeCase(typeOf<Unit>(), Variant.Type.NIL, "Unit"),
    )

    // ── String Types ────────────────────────────────────────────────────────────

    fun stringTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<String>(), Variant.Type.STRING, "String"),
        TypeCase(typeOf<GodotString>(), Variant.Type.STRING, "GodotString"),
    )

    // ── 2D Vector Types ─────────────────────────────────────────────────────────

    fun vector2Types(): List<TypeCase> = listOf(
        TypeCase(typeOf<Vector2>(), Variant.Type.VECTOR2, "Vector2"),
        TypeCase(typeOf<Vector2i>(), Variant.Type.VECTOR2I, "Vector2i"),
        TypeCase(typeOf<Rect2>(), Variant.Type.RECT2, "Rect2"),
        TypeCase(typeOf<Rect2i>(), Variant.Type.RECT2I, "Rect2i"),
        TypeCase(typeOf<Transform2D>(), Variant.Type.TRANSFORM2D, "Transform2D"),
    )

    // ── 3D Vector Types ─────────────────────────────────────────────────────────

    fun vector3Types(): List<TypeCase> = listOf(
        TypeCase(typeOf<Vector3>(), Variant.Type.VECTOR3, "Vector3"),
        TypeCase(typeOf<Vector3i>(), Variant.Type.VECTOR3I, "Vector3i"),
        TypeCase(typeOf<Transform3D>(), Variant.Type.TRANSFORM3D, "Transform3D"),
    )

    // ── 4D Vector Types ─────────────────────────────────────────────────────────

    fun vector4Types(): List<TypeCase> = listOf(
        TypeCase(typeOf<Vector4>(), Variant.Type.VECTOR4, "Vector4"),
        TypeCase(typeOf<Vector4i>(), Variant.Type.VECTOR4I, "Vector4i"),
    )

    // ── Math Types ──────────────────────────────────────────────────────────────

    fun mathTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Plane>(), Variant.Type.PLANE, "Plane"),
        TypeCase(typeOf<Quaternion>(), Variant.Type.QUATERNION, "Quaternion"),
        TypeCase(typeOf<Aabb>(), Variant.Type.AABB, "AABB"),
        TypeCase(typeOf<Basis>(), Variant.Type.BASIS, "Basis"),
        TypeCase(typeOf<Projection>(), Variant.Type.PROJECTION, "Projection"),
        TypeCase(typeOf<Color>(), Variant.Type.COLOR, "Color"),
    )

    // ── Core Types ───────────────────────────────────────────────────────────────

    fun coreTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<StringName>(), Variant.Type.STRING_NAME, "StringName"),
        TypeCase(typeOf<NodePath>(), Variant.Type.NODE_PATH, "NodePath"),
        TypeCase(typeOf<Rid>(), Variant.Type.RID, "RID"),
        TypeCase(typeOf<GodotObject>(), Variant.Type.OBJECT, "GodotObject"),
    )

    // ── Callable/Signal Types ────────────────────────────────────────────────────

    fun callableSignalTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Callable>(), Variant.Type.CALLABLE, "Callable"),
        TypeCase(typeOf<Signal>(), Variant.Type.SIGNAL, "Signal"),
    )

    // ── Container Types ──────────────────────────────────────────────────────────

    fun containerTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Dictionary<*, *>>(), Variant.Type.DICTIONARY, "Dictionary"),
        TypeCase(typeOf<GodotArray<*>>(), Variant.Type.ARRAY, "GodotArray"),
    )

    // ── Packed Array Types ───────────────────────────────────────────────────────

    fun packedArrayTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<PackedByteArray>(), Variant.Type.PACKED_BYTE_ARRAY, "PackedByteArray"),
        TypeCase(typeOf<PackedInt32Array>(), Variant.Type.PACKED_INT32_ARRAY, "PackedInt32Array"),
        TypeCase(typeOf<PackedInt64Array>(), Variant.Type.PACKED_INT64_ARRAY, "PackedInt64Array"),
        TypeCase(typeOf<PackedFloat32Array>(), Variant.Type.PACKED_FLOAT32_ARRAY, "PackedFloat32Array"),
        TypeCase(typeOf<PackedFloat64Array>(), Variant.Type.PACKED_FLOAT64_ARRAY, "PackedFloat64Array"),
        TypeCase(typeOf<PackedStringArray>(), Variant.Type.PACKED_STRING_ARRAY, "PackedStringArray"),
        TypeCase(typeOf<PackedVector2Array>(), Variant.Type.PACKED_VECTOR2_ARRAY, "PackedVector2Array"),
        TypeCase(typeOf<PackedVector3Array>(), Variant.Type.PACKED_VECTOR3_ARRAY, "PackedVector3Array"),
        TypeCase(typeOf<PackedColorArray>(), Variant.Type.PACKED_COLOR_ARRAY, "PackedColorArray"),
        TypeCase(typeOf<PackedVector4Array>(), Variant.Type.PACKED_VECTOR4_ARRAY, "PackedVector4Array"),
    )

    /**
     * All type categories combined for comprehensive testing.
     */
    fun allTypes(): List<TypeCase> = primitiveTypes() + stringTypes() + vector2Types() +
        vector3Types() + vector4Types() + mathTypes() + coreTypes() +
        callableSignalTypes() + containerTypes() + packedArrayTypes()

    /**
     * Error case test data: types that should throw for variantTypeOf.
     */
    data class ErrorCase(val kotlinType: KType, val displayName: String)

    fun errorCases(): List<ErrorCase> = listOf(
        ErrorCase(typeOf<Boolean?>(), "nullable Boolean"),
        ErrorCase(typeOf<Int?>(), "nullable Int"),
        ErrorCase(typeOf<String?>(), "nullable String"),
        ErrorCase(typeOf<GodotObject?>(), "nullable GodotObject"),
        ErrorCase(typeOf<Node?>(), "nullable Node"),
    )
}
