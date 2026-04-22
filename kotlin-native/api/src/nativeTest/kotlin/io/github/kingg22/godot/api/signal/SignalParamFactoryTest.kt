package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import kotlinx.cinterop.COpaquePointer
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
@OptIn(ExperimentalGodotKotlin::class)
class SignalParamFactoryTest {

    // ── Simple Test Case Data Classes ─────────────────────────────────────────

    private data class TypeCase(val kotlinType: KType, val expectedVariantType: Variant.Type, val displayName: String)

    // ── Test Data: variantType mappings ────────────────────────────────────────

    private fun primitiveTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Boolean>(), Variant.Type.BOOL, "Boolean"),
        TypeCase(typeOf<Int>(), Variant.Type.INT, "Int"),
        TypeCase(typeOf<Long>(), Variant.Type.INT, "Long"),
        TypeCase(typeOf<Float>(), Variant.Type.FLOAT, "Float"),
        TypeCase(typeOf<Double>(), Variant.Type.FLOAT, "Double"),
        TypeCase(typeOf<Unit>(), Variant.Type.NIL, "Unit"),
    )

    private fun stringTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<String>(), Variant.Type.STRING, "String"),
        TypeCase(typeOf<GodotString>(), Variant.Type.STRING, "GodotString"),
    )

    private fun vector2Types(): List<TypeCase> = listOf(
        TypeCase(typeOf<Vector2>(), Variant.Type.VECTOR2, "Vector2"),
        TypeCase(typeOf<Vector2i>(), Variant.Type.VECTOR2I, "Vector2i"),
        TypeCase(typeOf<Rect2>(), Variant.Type.RECT2, "Rect2"),
        TypeCase(typeOf<Rect2i>(), Variant.Type.RECT2I, "Rect2i"),
        TypeCase(typeOf<Transform2D>(), Variant.Type.TRANSFORM2D, "Transform2D"),
    )

    private fun vector3Types(): List<TypeCase> = listOf(
        TypeCase(typeOf<Vector3>(), Variant.Type.VECTOR3, "Vector3"),
        TypeCase(typeOf<Vector3i>(), Variant.Type.VECTOR3I, "Vector3i"),
        TypeCase(typeOf<Transform3D>(), Variant.Type.TRANSFORM3D, "Transform3D"),
    )

    private fun vector4Types(): List<TypeCase> = listOf(
        TypeCase(typeOf<Vector4>(), Variant.Type.VECTOR4, "Vector4"),
        TypeCase(typeOf<Vector4i>(), Variant.Type.VECTOR4I, "Vector4i"),
    )

    private fun mathTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Plane>(), Variant.Type.PLANE, "Plane"),
        TypeCase(typeOf<Quaternion>(), Variant.Type.QUATERNION, "Quaternion"),
        TypeCase(typeOf<Aabb>(), Variant.Type.AABB, "AABB"),
        TypeCase(typeOf<Basis>(), Variant.Type.BASIS, "Basis"),
        TypeCase(typeOf<Projection>(), Variant.Type.PROJECTION, "Projection"),
        TypeCase(typeOf<Color>(), Variant.Type.COLOR, "Color"),
    )

    private fun coreTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<StringName>(), Variant.Type.STRING_NAME, "StringName"),
        TypeCase(typeOf<NodePath>(), Variant.Type.NODE_PATH, "NodePath"),
        TypeCase(typeOf<Rid>(), Variant.Type.RID, "RID"),
        TypeCase(typeOf<GodotObject>(), Variant.Type.OBJECT, "GodotObject"),
    )

    private fun containerTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Dictionary<*, *>>(), Variant.Type.DICTIONARY, "Dictionary"),
        TypeCase(typeOf<GodotArray<*>>(), Variant.Type.ARRAY, "GodotArray"),
    )

    private fun packedArrayTypes(): List<TypeCase> = listOf(
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

    // ── param() Factory Tests ───────────────────────────────────────────────────

    @Test
    fun `param creates descriptor with correct name`() {
        val descriptor = param<Int>("test_param")
        assertEquals("test_param", descriptor.name)
    }

    @Test
    fun `param creates descriptor with correct kType`() {
        val descriptor = param<String>("test_param")
        assertEquals(typeOf<String>(), descriptor.kType)
    }

    @Test
    fun `param creates descriptor with correct variantType`() {
        val descriptor = param<String>("test_param")
        assertEquals(Variant.Type.STRING, descriptor.variantType)
    }

    @Test
    fun `param creates non-nullable descriptor for non-nullable types`() {
        val descriptor = param<Int>("test_param")
        assertEquals(false, descriptor.isOptional)
    }

    @Test
    fun `param throws for nullable types`() {
        // Note: param() cannot support nullable types because variantTypeOf(kType)
        // doesn't support them - it throws IllegalStateException
        val ex = assertFailsWith<IllegalStateException> {
            param<Int?>("test_param")
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    // ── param() variantType Tests (data-driven) ────────────────────────────────

    @Test
    fun `param variantType maps primitive types correctly`() {
        for (tc in primitiveTypes()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps string types correctly`() {
        for (tc in stringTypes()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps 2D vector types correctly`() {
        for (tc in vector2Types()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps 3D vector types correctly`() {
        for (tc in vector3Types()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps 4D vector types correctly`() {
        for (tc in vector4Types()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps math types correctly`() {
        for (tc in mathTypes()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps core types correctly`() {
        for (tc in coreTypes()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps container types correctly`() {
        for (tc in containerTypes()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `param variantType maps packed array types correctly`() {
        for (tc in packedArrayTypes()) {
            val descriptor = createParam(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    // ── Error Cases: param() ──────────────────────────────────────────────────

    @Test
    fun `param throws for custom class`() {
        class CustomTestClass

        val ex = assertFailsWith<IllegalStateException> {
            param<CustomTestClass>("test_param")
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `param throws for nullable Int`() {
        val ex = assertFailsWith<IllegalStateException> {
            param<Int?>("test_param")
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `param throws for GodotObject subclass`() {
        class GodotObjectSubclass(nativePtr: COpaquePointer) : GodotObject(nativePtr)

        val ex = assertFailsWith<IllegalStateException> {
            param<GodotObjectSubclass>("test_param")
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `param throws for nullable GodotObject`() {
        val ex = assertFailsWith<IllegalStateException> {
            param<GodotObject?>("test_param")
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `param throws for nullable Node`() {
        val ex = assertFailsWith<IllegalStateException> {
            param<Node?>("test_param")
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    // ── Helper to create param from KType ─────────────────────────────────────
    // Note: We cannot fully test Callable/Signal here because they require
    // native integration that isn't available in unit tests.
    // The variantTypeOf function supports them but param() is designed for
    // parameter types, not callable/signal types.

    @Suppress("UNCHECKED_CAST")
    private fun createParam(kType: KType): SignalParameterDescriptor<*> = when (kType) {
        typeOf<Boolean>() -> param<Boolean>("test")
        typeOf<Int>() -> param<Int>("test")
        typeOf<Long>() -> param<Long>("test")
        typeOf<Float>() -> param<Float>("test")
        typeOf<Double>() -> param<Double>("test")
        typeOf<Unit>() -> param<Unit>("test")
        typeOf<String>() -> param<String>("test")
        typeOf<GodotString>() -> param<GodotString>("test")
        typeOf<Vector2>() -> param<Vector2>("test")
        typeOf<Vector2i>() -> param<Vector2i>("test")
        typeOf<Rect2>() -> param<Rect2>("test")
        typeOf<Rect2i>() -> param<Rect2i>("test")
        typeOf<Transform2D>() -> param<Transform2D>("test")
        typeOf<Vector3>() -> param<Vector3>("test")
        typeOf<Vector3i>() -> param<Vector3i>("test")
        typeOf<Transform3D>() -> param<Transform3D>("test")
        typeOf<Vector4>() -> param<Vector4>("test")
        typeOf<Vector4i>() -> param<Vector4i>("test")
        typeOf<Plane>() -> param<Plane>("test")
        typeOf<Quaternion>() -> param<Quaternion>("test")
        typeOf<Aabb>() -> param<Aabb>("test")
        typeOf<Basis>() -> param<Basis>("test")
        typeOf<Projection>() -> param<Projection>("test")
        typeOf<Color>() -> param<Color>("test")
        typeOf<StringName>() -> param<StringName>("test")
        typeOf<NodePath>() -> param<NodePath>("test")
        typeOf<Rid>() -> param<Rid>("test")
        typeOf<GodotObject>() -> param<GodotObject>("test")
        typeOf<Dictionary<*, *>>() -> param<Dictionary<*, *>>("test")
        typeOf<GodotArray<*>>() -> param<GodotArray<*>>("test")
        typeOf<PackedByteArray>() -> param<PackedByteArray>("test")
        typeOf<PackedInt32Array>() -> param<PackedInt32Array>("test")
        typeOf<PackedInt64Array>() -> param<PackedInt64Array>("test")
        typeOf<PackedFloat32Array>() -> param<PackedFloat32Array>("test")
        typeOf<PackedFloat64Array>() -> param<PackedFloat64Array>("test")
        typeOf<PackedStringArray>() -> param<PackedStringArray>("test")
        typeOf<PackedVector2Array>() -> param<PackedVector2Array>("test")
        typeOf<PackedVector3Array>() -> param<PackedVector3Array>("test")
        typeOf<PackedColorArray>() -> param<PackedColorArray>("test")
        typeOf<PackedVector4Array>() -> param<PackedVector4Array>("test")
        else -> error("Unsupported type for test: $kType")
    }
}
