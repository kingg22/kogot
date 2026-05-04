package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.VariantTypeTestData
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.equals.shouldEqual
import kotlinx.cinterop.COpaquePointer
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

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
}
