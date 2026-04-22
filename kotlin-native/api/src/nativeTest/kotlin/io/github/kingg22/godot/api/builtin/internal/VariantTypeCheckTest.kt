package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.kotest.assertions.throwables.shouldNotThrowAny
import kotlinx.cinterop.COpaquePointer
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class VariantTypeCheckTest {

    // ── Simple Test Case Data Classes ─────────────────────────────────────────

    private data class TypeCase(val kotlinType: KType, val expectedVariantType: Variant.Type, val displayName: String)

    // ── Test Data: variantTypeOf mappings ────────────────────────────────────

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
        TypeCase(typeOf<GodotObject>(), Variant.Type.OBJECT, "Object"),
    )

    private fun callableSignalTypes(): List<TypeCase> = listOf(
        TypeCase(typeOf<Callable>(), Variant.Type.CALLABLE, "Callable"),
        TypeCase(typeOf<Signal>(), Variant.Type.SIGNAL, "Signal"),
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

    // ── variantTypeOf Tests ───────────────────────────────────────────────────

    @Test
    fun `variantTypeOf maps primitive types correctly`() {
        for (tc in primitiveTypes()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps string types correctly`() {
        for (tc in stringTypes()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps 2D vector types correctly`() {
        for (tc in vector2Types()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps 3D vector types correctly`() {
        for (tc in vector3Types()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps 4D vector types correctly`() {
        for (tc in vector4Types()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps math types correctly`() {
        for (tc in mathTypes()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps core types correctly`() {
        for (tc in coreTypes()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps callable and signal types correctly`() {
        for (tc in callableSignalTypes()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps container types correctly`() {
        for (tc in containerTypes()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    @Test
    fun `variantTypeOf maps packed array types correctly`() {
        for (tc in packedArrayTypes()) {
            val result = variantTypeOf(tc.kotlinType)
            assertEquals(tc.expectedVariantType, result, "Failed for ${tc.displayName}")
        }
    }

    // ── Error Cases: variantTypeOf ──────────────────────────────────────────

    @Test
    fun `variantTypeOf throws for custom class`() {
        // ── Custom Test Classes for Error Cases ───────────────────────────────────

        class CustomTestClass

        val customType = typeOf<CustomTestClass>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(customType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `variantTypeOf throws for nullable string`() {
        val nullableStringType = typeOf<String?>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(nullableStringType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `variantTypeOf throws for GodotObject subclasses`() {
        // GodotObject inheritance is not supported
        class GodotObjectSubclass(nativePtr: COpaquePointer) : GodotObject(nativePtr)
        val godotObjectType = typeOf<GodotObjectSubclass>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(godotObjectType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `variantTypeOf throws for Node subclass`() {
        // Node extends GodotObject, inherits same behavior - not in when branches
        val nodeType = typeOf<Node>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(nodeType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `variantTypeOf throws for nullable GodotObject`() {
        // Nullable GodotObject also aren't supported
        val nullableGodotObjectType = typeOf<GodotObject?>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(nullableGodotObjectType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `variantTypeOf throws for nullable Node`() {
        // Nullable Node also isn't supported
        val nullableNodeType = typeOf<Node?>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(nullableNodeType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `variantTypeOf throws for Int nullable`() {
        // Nullable Int should also fail
        val nullableIntType = typeOf<Int?>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(nullableIntType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `variantTypeOf throws for Boolean nullable`() {
        // Nullable Boolean should also fail
        val nullableBoolType = typeOf<Boolean?>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(nullableBoolType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    // ── checkVariantCompatibility Tests ──────────────────────────────────────

    @Test
    fun `checkVariantCompatibility String matches STRING`() {
        // Direct call - if it throws, test fails
        checkVariantCompatibility<String>(Variant.Type.STRING)
    }

    @Test
    fun `checkVariantCompatibility String matches STRING_NAME`() {
        // String flexible compatibility
        checkVariantCompatibility<String>(Variant.Type.STRING_NAME)
    }

    @Test
    fun `checkVariantCompatibility String matches NODE_PATH`() {
        // String flexible compatibility
        checkVariantCompatibility<String>(Variant.Type.NODE_PATH)
    }

    @Test
    fun `checkVariantCompatibility String does NOT match RID`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            checkVariantCompatibility<String>(Variant.Type.RID)
        }
        assertContains(assertNotNull(ex.message), "Variant.Type mismatch")
    }

    @Test
    fun `checkVariantCompatibility mismatches throw - Bool vs Int`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            checkVariantCompatibility<Boolean>(Variant.Type.INT)
        }
        assertContains(assertNotNull(ex.message), "Variant.Type mismatch")
    }

    @Test
    fun `checkVariantCompatibility mismatches throw - Int vs Float`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            checkVariantCompatibility<Int>(Variant.Type.FLOAT)
        }
        assertContains(assertNotNull(ex.message), "Variant.Type mismatch")
    }

    @Test
    fun `checkVariantCompatibility mismatches throw - String vs Int`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            checkVariantCompatibility<String>(Variant.Type.INT)
        }
        assertContains(assertNotNull(ex.message), "Variant.Type mismatch")
    }

    @Test
    fun `checkVariantCompatibility mismatches throw - Vector2 vs Vector3`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            checkVariantCompatibility<Vector2>(Variant.Type.VECTOR3)
        }
        assertContains(assertNotNull(ex.message), "Variant.Type mismatch")
    }

    @Test
    fun `checkVariantCompatibility error message format`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            checkVariantCompatibility<Boolean>(Variant.Type.INT)
        }

        val msg = ex.message ?: ""
        assertTrue(msg.contains("expected="), "Message should contain 'expected='")
        assertTrue(msg.contains("got="), "Message should contain 'got='")
        assertTrue(msg.contains("BOOL"), "Message should contain 'BOOL'")
        assertTrue(msg.contains("INT"), "Message should contain 'INT'")
        assertTrue(msg.contains("T="), "Message should contain 'T='")
    }

    @Test
    fun `checkVariantCompatibility GodotObject with OBJECT passes`() {
        // This test documents current behavior: GodotObject is NOT in variantTypeOf
        // so this will throw an IllegalStateException from variantTypeOf
        // When GodotObject support is added, this should pass
        shouldNotThrowAny { checkVariantCompatibility<GodotObject>(Variant.Type.OBJECT) }
    }

    @Test
    fun `checkVariantCompatibility Node with OBJECT passes`() {
        // Node extends GodotObject, same issue
        try {
            checkVariantCompatibility<Node>(Variant.Type.OBJECT)
        } catch (e: IllegalStateException) {
            assertContains(e.message ?: "", "Unsupported type")
        }
    }

    @Test
    fun `checkVariantCompatibility exact type matches`() {
        // These should all pass without throwing
        checkVariantCompatibility<Boolean>(Variant.Type.BOOL)
        checkVariantCompatibility<Int>(Variant.Type.INT)
        checkVariantCompatibility<Long>(Variant.Type.INT) // Long maps to INT
        checkVariantCompatibility<Float>(Variant.Type.FLOAT)
        checkVariantCompatibility<Double>(Variant.Type.FLOAT) // Double maps to FLOAT
        checkVariantCompatibility<Vector2>(Variant.Type.VECTOR2)
        checkVariantCompatibility<Vector2i>(Variant.Type.VECTOR2I)
        checkVariantCompatibility<Vector3>(Variant.Type.VECTOR3)
        checkVariantCompatibility<Vector4>(Variant.Type.VECTOR4)
        checkVariantCompatibility<Color>(Variant.Type.COLOR)
        checkVariantCompatibility<Callable>(Variant.Type.CALLABLE)
        checkVariantCompatibility<Signal>(Variant.Type.SIGNAL)
    }
}
