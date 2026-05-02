package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.VariantTypeTestData
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.equals.shouldEqual
import kotlinx.cinterop.COpaquePointer
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SignalParameterDescriptorTest {

    private val td = VariantTypeTestData

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
        assertFalse(descriptor.isOptional)
    }

    // ── variantType Tests (data-driven) ────────────────────────────────────────

    @Test
    fun `SignalParameterDescriptor variantType maps types correctly`() {
        assertSoftly {
            for (tc in td.allTypes()) {
                val descriptor = createDescriptor(tc.kotlinType)
                descriptor.variantType shouldEqual tc.expectedVariantType
            }
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
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    // FIXME signal params needs to support subclasses
    @Test
    fun `SignalParameterDescriptor throws for GodotObject subclass`() {
        class GodotObjectSubclass(nativePtr: COpaquePointer) : GodotObject(nativePtr)

        val godotObjectType = typeOf<GodotObjectSubclass>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<Any>("test", godotObjectType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    // -- Compiler error because the type is enforced to be non-null --
    // FIXME signal params needs to support nullable types
    @Test
    fun `SignalParameterDescriptor throws for nullable Int`() {
        val nullableIntType = typeOf<Int?>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<Int>("test", nullableIntType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `SignalParameterDescriptor throws for nullable GodotObject`() {
        val nullableGodotObjectType = typeOf<GodotObject?>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<GodotObject>("test", nullableGodotObjectType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Test
    fun `SignalParameterDescriptor throws for nullable Node`() {
        val nullableNodeType = typeOf<Node?>()
        val ex = assertFailsWith<IllegalStateException> {
            SignalParameterDescriptor<Node>("test", nullableNodeType)
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }
    // -- End compiler error --

    // ── Helper to create descriptor from KType ────────────────────────────────

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
        typeOf<Callable>() -> SignalParameterDescriptor<Callable>("test", kType)
        typeOf<Signal>() -> SignalParameterDescriptor<Signal>("test", kType)
        else -> error("Unsupported type for test: $kType")
    }
}
