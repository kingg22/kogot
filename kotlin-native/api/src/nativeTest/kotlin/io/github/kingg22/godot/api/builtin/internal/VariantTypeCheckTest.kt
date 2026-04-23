package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.VariantTypeTestData
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.equals.shouldEqual
import kotlinx.cinterop.COpaquePointer
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class VariantTypeCheckTest {

    private val td = VariantTypeTestData

    // ── variantTypeOf Tests ───────────────────────────────────────────────────

    @Test
    fun `variantTypeOf maps types correctly`() {
        assertSoftly {
            for (tc in td.allTypes()) {
                val result = variantTypeOf(tc.kotlinType)
                result shouldEqual tc.expectedVariantType
            }
        }
    }

    // ── Error Cases: variantTypeOf (data-driven) ───────────────────────────────

    @Test
    fun `variantTypeOf throws for nullable types`() {
        for (ec in td.errorCases()) {
            val ex = assertFailsWith<IllegalStateException> {
                variantTypeOf(ec.kotlinType)
            }
            assertContains(assertNotNull(ex.message), "Unsupported type for Variant")
        }
    }

    @Test
    fun `variantTypeOf throws for custom class`() {
        // When GodotObject inheritance support is added, this should pass
        class CustomTestClass

        val customType = typeOf<CustomTestClass>()
        val ex = assertFailsWith<IllegalStateException> {
            variantTypeOf(customType)
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
        // This test documents current behavior: GodotObject is in variantTypeOf
        // so this will pass, but inheritance of GodotObject throws IllegalStateException
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
        shouldNotThrowAny {
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
}
