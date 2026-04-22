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
class SignalParameterDescriptorTest {

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

    // ── Constructor Tests ───────────────────────────────────────────────────────

    @Test
    fun `SignalParameterDescriptor stores name correctly`() {
        val descriptor = SignalParameterDescriptor<Int>("test_param", typeOf<Int>())
        assertEquals("test_param", descriptor.name)
    }

    @Test
    fun `SignalParameterDescriptor stores kType correctly`() {
        val descriptor = SignalParameterDescriptor<String>("test_param", typeOf<String>())
        assertEquals(typeOf<String>(), descriptor.kType)
    }

    @Test
    fun `SignalParameterDescriptor isOptional is false for non-nullable types`() {
        val descriptor = SignalParameterDescriptor<Int>("test_param", typeOf<Int>())
        assertEquals(false, descriptor.isOptional)
    }

    // ── variantType Tests (data-driven) ────────────────────────────────────────

    @Test
    fun `SignalParameterDescriptor variantType maps primitive types correctly`() {
        for (tc in primitiveTypes()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps string types correctly`() {
        for (tc in stringTypes()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps 2D vector types correctly`() {
        for (tc in vector2Types()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps 3D vector types correctly`() {
        for (tc in vector3Types()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps 4D vector types correctly`() {
        for (tc in vector4Types()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps math types correctly`() {
        for (tc in mathTypes()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps core types correctly`() {
        for (tc in coreTypes()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps container types correctly`() {
        for (tc in containerTypes()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `SignalParameterDescriptor variantType maps packed array types correctly`() {
        for (tc in packedArrayTypes()) {
            val descriptor = createDescriptor(tc.kotlinType)
            assertEquals(tc.expectedVariantType, descriptor.variantType, "Failed for ${tc.displayName}")
        }
    }

    // ── Error Cases: Constructor ────────────────────────────────────────────────

    @Test
    fun `SignalParameterDescriptor throws for custom class`() {
        class CustomTestClass

        val customType = typeOf<CustomTestClass>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<Any>("test", customType)
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `SignalParameterDescriptor throws for nullable Int`() {
        val nullableIntType = typeOf<Int?>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<Int?>("test", nullableIntType)
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `SignalParameterDescriptor throws for GodotObject subclass`() {
        class GodotObjectSubclass(nativePtr: COpaquePointer) : GodotObject(nativePtr)

        val godotObjectType = typeOf<GodotObjectSubclass>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<Any>("test", godotObjectType)
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `SignalParameterDescriptor throws for nullable GodotObject`() {
        val nullableGodotObjectType = typeOf<GodotObject?>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<GodotObject?>("test", nullableGodotObjectType)
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    @Test
    fun `SignalParameterDescriptor throws for nullable Node`() {
        val nullableNodeType = typeOf<Node?>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<Node?>("test", nullableNodeType)
        }
        assertTrue(ex.message?.contains("Unsupported type for Variant") == true)
    }

    // ── Helper to create descriptor from KType ────────────────────────────────
    // Note: We cannot fully test Callable/Signal here because SignalParameterDescriptor
    // doesn't support them directly (they require native integration).
    // The variantTypeOf function supports them but SignalParameterDescriptor
    // is designed for parameter types, not callable/signal types.

    private fun createDescriptor(kType: KType): SignalParameterDescriptor<*> = when (kType) {
        typeOf<Boolean>() -> SignalParameterDescriptor<Boolean>("test", kType)
        typeOf<Int>() -> SignalParameterDescriptor<Int>("test", kType)
        typeOf<Long>() -> SignalParameterDescriptor<Long>("test", kType)
        typeOf<Float>() -> SignalParameterDescriptor<Float>("test", kType)
        typeOf<Double>() -> SignalParameterDescriptor<Double>("test", kType)
        typeOf<Unit>() -> SignalParameterDescriptor<Unit>("test", kType)
        typeOf<String>() -> SignalParameterDescriptor<String>("test", kType)
        typeOf<GodotString>() -> SignalParameterDescriptor<GodotString>("test", kType)
        typeOf<Vector2>() -> SignalParameterDescriptor<Vector2>("test", kType)
        typeOf<Vector2i>() -> SignalParameterDescriptor<Vector2i>("test", kType)
        typeOf<Rect2>() -> SignalParameterDescriptor<Rect2>("test", kType)
        typeOf<Rect2i>() -> SignalParameterDescriptor<Rect2i>("test", kType)
        typeOf<Transform2D>() -> SignalParameterDescriptor<Transform2D>("test", kType)
        typeOf<Vector3>() -> SignalParameterDescriptor<Vector3>("test", kType)
        typeOf<Vector3i>() -> SignalParameterDescriptor<Vector3i>("test", kType)
        typeOf<Transform3D>() -> SignalParameterDescriptor<Transform3D>("test", kType)
        typeOf<Vector4>() -> SignalParameterDescriptor<Vector4>("test", kType)
        typeOf<Vector4i>() -> SignalParameterDescriptor<Vector4i>("test", kType)
        typeOf<Plane>() -> SignalParameterDescriptor<Plane>("test", kType)
        typeOf<Quaternion>() -> SignalParameterDescriptor<Quaternion>("test", kType)
        typeOf<Aabb>() -> SignalParameterDescriptor<Aabb>("test", kType)
        typeOf<Basis>() -> SignalParameterDescriptor<Basis>("test", kType)
        typeOf<Projection>() -> SignalParameterDescriptor<Projection>("test", kType)
        typeOf<Color>() -> SignalParameterDescriptor<Color>("test", kType)
        typeOf<StringName>() -> SignalParameterDescriptor<StringName>("test", kType)
        typeOf<NodePath>() -> SignalParameterDescriptor<NodePath>("test", kType)
        typeOf<Rid>() -> SignalParameterDescriptor<Rid>("test", kType)
        typeOf<GodotObject>() -> SignalParameterDescriptor<GodotObject>("test", kType)
        typeOf<Dictionary<*, *>>() -> SignalParameterDescriptor<Dictionary<*, *>>("test", kType)
        typeOf<GodotArray<*>>() -> SignalParameterDescriptor<GodotArray<*>>("test", kType)
        typeOf<PackedByteArray>() -> SignalParameterDescriptor<PackedByteArray>("test", kType)
        typeOf<PackedInt32Array>() -> SignalParameterDescriptor<PackedInt32Array>("test", kType)
        typeOf<PackedInt64Array>() -> SignalParameterDescriptor<PackedInt64Array>("test", kType)
        typeOf<PackedFloat32Array>() -> SignalParameterDescriptor<PackedFloat32Array>("test", kType)
        typeOf<PackedFloat64Array>() -> SignalParameterDescriptor<PackedFloat64Array>("test", kType)
        typeOf<PackedStringArray>() -> SignalParameterDescriptor<PackedStringArray>("test", kType)
        typeOf<PackedVector2Array>() -> SignalParameterDescriptor<PackedVector2Array>("test", kType)
        typeOf<PackedVector3Array>() -> SignalParameterDescriptor<PackedVector3Array>("test", kType)
        typeOf<PackedColorArray>() -> SignalParameterDescriptor<PackedColorArray>("test", kType)
        typeOf<PackedVector4Array>() -> SignalParameterDescriptor<PackedVector4Array>("test", kType)
        else -> error("Unsupported type for test: $kType")
    }
}
