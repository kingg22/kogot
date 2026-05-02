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
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SignalParamFactoryTest {

    private val td = VariantTypeTestData

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
        assertFalse(descriptor.isOptional)
    }

    // ── param() variantType Tests (data-driven) ────────────────────────────────

    @Test
    fun `param variantType maps types correctly`() {
        assertSoftly {
            for (tc in td.allTypes()) {
                val descriptor = createParam(tc.kotlinType)
                descriptor.variantType shouldEqual tc.expectedVariantType
            }
        }
    }

    // ── param() vs SignalParameterDescriptor equivalence Tests ────────────────

    @Test
    fun `param and SignalParameterDescriptor produce equivalent descriptors`() {
        assertSoftly {
            for (tc in td.allTypes()) {
                val name = "test_param"
                val paramDescriptor = paramByType(tc.kotlinType, name)
                val manualDescriptor = SignalParameterDescriptor<Any>(name, tc.kotlinType)

                paramDescriptor.name shouldEqual manualDescriptor.name
                paramDescriptor.kType shouldEqual manualDescriptor.kType
                paramDescriptor.variantType shouldEqual manualDescriptor.variantType
                paramDescriptor.isOptional shouldEqual manualDescriptor.isOptional
            }
        }
    }

    // ── Error Cases: param() ──────────────────────────────────────────────────

    @Test
    fun `param throws for custom class`() {
        class CustomTestClass

        val ex = assertFailsWith<IllegalStateException> {
            param<CustomTestClass>("test_param")
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    // FIXME signal params needs to support subclasses
    @Test
    fun `param throws for GodotObject subclass`() {
        class GodotObjectSubclass(nativePtr: COpaquePointer) : GodotObject(nativePtr)

        val ex = assertFailsWith<IllegalStateException> {
            param<GodotObjectSubclass>("test_param")
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    // -- Compiler error because the type is enforced to be non-null --
    // FIXME signal params needs to support nullable types

    @Ignore
    @Test
    fun `param throws for nullable Int`() {
        val ex = assertFailsWith<IllegalStateException> {
            param<Int>("test_param")
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Ignore
    @Test
    fun `param throws for nullable GodotObject`() {
        val ex = assertFailsWith<IllegalStateException> {
            param<GodotObject>("test_param")
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }

    @Ignore
    @Test
    fun `param throws for nullable Node`() {
        val ex = assertFailsWith<IllegalStateException> {
            param<Node>("test_param")
        }
        assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
    }
    // -- End compiler error --

    // ── Helper to create param from KType ─────────────────────────────────────

    private fun paramByType(kType: KType, name: String): SignalParameterDescriptor<*> = when (kType) {
        typeOf<Boolean>() -> param<Boolean>(name)
        typeOf<Int>() -> param<Int>(name)
        typeOf<Long>() -> param<Long>(name)
        typeOf<Float>() -> param<Float>(name)
        typeOf<Double>() -> param<Double>(name)
        typeOf<Unit>() -> param<Unit>(name)
        typeOf<String>() -> param<String>(name)
        typeOf<GodotString>() -> param<GodotString>(name)
        typeOf<Vector2>() -> param<Vector2>(name)
        typeOf<Vector2i>() -> param<Vector2i>(name)
        typeOf<Rect2>() -> param<Rect2>(name)
        typeOf<Rect2i>() -> param<Rect2i>(name)
        typeOf<Transform2D>() -> param<Transform2D>(name)
        typeOf<Vector3>() -> param<Vector3>(name)
        typeOf<Vector3i>() -> param<Vector3i>(name)
        typeOf<Transform3D>() -> param<Transform3D>(name)
        typeOf<Vector4>() -> param<Vector4>(name)
        typeOf<Vector4i>() -> param<Vector4i>(name)
        typeOf<Plane>() -> param<Plane>(name)
        typeOf<Quaternion>() -> param<Quaternion>(name)
        typeOf<Aabb>() -> param<Aabb>(name)
        typeOf<Basis>() -> param<Basis>(name)
        typeOf<Projection>() -> param<Projection>(name)
        typeOf<Color>() -> param<Color>(name)
        typeOf<StringName>() -> param<StringName>(name)
        typeOf<NodePath>() -> param<NodePath>(name)
        typeOf<Rid>() -> param<Rid>(name)
        typeOf<GodotObject>() -> param<GodotObject>(name)
        typeOf<Dictionary<*, *>>() -> param<Dictionary<*, *>>(name)
        typeOf<GodotArray<*>>() -> param<GodotArray<*>>(name)
        typeOf<PackedByteArray>() -> param<PackedByteArray>(name)
        typeOf<PackedInt32Array>() -> param<PackedInt32Array>(name)
        typeOf<PackedInt64Array>() -> param<PackedInt64Array>(name)
        typeOf<PackedFloat32Array>() -> param<PackedFloat32Array>(name)
        typeOf<PackedFloat64Array>() -> param<PackedFloat64Array>(name)
        typeOf<PackedStringArray>() -> param<PackedStringArray>(name)
        typeOf<PackedVector2Array>() -> param<PackedVector2Array>(name)
        typeOf<PackedVector3Array>() -> param<PackedVector3Array>(name)
        typeOf<PackedColorArray>() -> param<PackedColorArray>(name)
        typeOf<PackedVector4Array>() -> param<PackedVector4Array>(name)
        typeOf<Callable>() -> param<Callable>(name)
        typeOf<Signal>() -> param<Signal>(name)
        else -> error("Unsupported type for test: $kType")
    }

    private fun createParam(kType: KType): SignalParameterDescriptor<*> = paramByType(kType, "test")
}
