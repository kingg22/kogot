@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.virtualreturn

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.VariantArray
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.refcounted.Mesh
import io.github.kingg22.godot.api.singleton.Os
import io.github.kingg22.godot.binding.instantiate
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A mesh whose `_surface_get_arrays` returns a fresh [VariantArray] with two elements on every call. */
@Godot
class VirtualReturnProbeMesh(nativePtr: COpaquePointer) : Mesh(nativePtr) {
    override fun _surfaceGetArrays(index: Int): VariantArray {
        val arrays = VariantArray()
        Variant.from(7L).use { arrays.add(it) }
        Variant.from("seven").use { arrays.add(it) }
        return arrays
    }
}

/**
 * Trampoline of a virtual that returns a builtin: the array an override returns reaches the engine intact, and
 * returning it 20,000 times does not grow the engine memory (the wrapper the override returns is released once the
 * engine has copied it).
 *
 * The live check loads `res://probe_mesh.tres`; the test host has no assets, so the probe is instantiated directly.
 *
 * Not ported: the "script lists" checks (`Script.get_script_property_list` / `get_script_method_list` of the
 * `SpriteBench.kt` and former `TestOne.kt` scripts), which load `.kt` scripts from the game project through `res://`.
 */
class VirtualReturnTest {
    @Test
    fun virtualBuiltinReturnReachesTheEngineIntact() {
        val probe = instantiate<VirtualReturnProbeMesh>()
        val method = "surface_get_arrays".toStringName()
        Variant.from(0L).use { index ->
            probe.call(method, index).use { result ->
                val array = result.toArray()
                assertEquals(2, array.size, "size=${array.size}")
                array.close()
            }
        }
    }

    @Test
    fun returningABuiltinFromAVirtualDoesNotLeak() {
        val probe = instantiate<VirtualReturnProbeMesh>()
        val method = "surface_get_arrays".toStringName()
        Variant.from(0L).use { index ->
            val os = Os.instance
            probe.call(method, index).close() // warm up
            val before = os.getStaticMemoryUsage().toLong()
            repeat(20_000) { probe.call(method, index).close() }
            val growth = os.getStaticMemoryUsage().toLong() - before
            println("[VirtualReturnTest] growth=$growth bytes over 20000 calls")
            assertTrue(growth < 100_000, "static memory grew by $growth bytes")
        }
    }
}
