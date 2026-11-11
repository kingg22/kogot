@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.VariantArray
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.toGodotString
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.refcounted.Mesh
import io.github.kingg22.godot.api.singleton.ResourceLoader
import io.github.kingg22.godot.api.singleton.Os
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/** A mesh whose `_surface_get_arrays` returns a fresh [VariantArray] with two elements on every call. */
@Godot class ProbeMesh(nativePtr: COpaquePointer) : Mesh(nativePtr) {
    override fun _surfaceGetArrays(index: Int): VariantArray {
        val arrays = VariantArray()
        Variant.from(7L).use { arrays.add(it) }
        Variant.from("seven").use { arrays.add(it) }
        return arrays
    }
}

/**
 * Live check of the trampoline of a virtual that returns a builtin (`virtual_returns_check.tscn`): the array an
 * override returns reaches the engine intact, and returning it 20,000 times does not grow the engine memory (the
 * wrapper the override returns is released once the engine has copied it).
 */
@Godot class VirtualReturnCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("VirtualReturnCheck")

    override fun _ready() {
        val probe = ResourceLoader.instance.load("res://probe_mesh.tres".toGodotString())
        val method = "surface_get_arrays".toStringName()
        with(checks) {
            check("virtual builtin return reaches the engine intact") {
                Variant.from(0L).use { index ->
                    probe.call(method, index).use { result ->
                        val array = result.toArray()
                        expect(array.size == 2) { "size=${array.size}" }
                        array.close()
                    }
                }
            }
            check("returning a builtin from a virtual does not leak") {
                Variant.from(0L).use { index ->
                    val os = Os.instance
                    probe.call(method, index).close() // warm up
                    val before = os.getStaticMemoryUsage().toLong()
                    repeat(20_000) { probe.call(method, index).close() }
                    val growth = os.getStaticMemoryUsage().toLong() - before
                    println("[VirtualReturnCheck] growth=$growth bytes over 20000 calls")
                    expect(growth < 100_000) { "static memory grew by $growth bytes" }
                }
            }
        }
        scriptListChecks()
        getTree().quit(checks.finish())
    }

    /** The editor-facing lists of a `.kt` script (`Script.get_script_property_list`), without opening the editor. */
    private fun scriptListChecks() = with(checks) {
        val dir = "res://kotlin_native_game/src/nativeMain/kotlin"
        // SpriteBench exports 3 properties, TestOne registers 1 method.
        for ((file, name, expected) in listOf(
            Triple("SpriteBench.kt", "get_script_property_list", 3),
            Triple("TestOne.kt", "get_script_method_list", 1),
        )) {
            check("script lists: $name of $file has $expected entries and does not leak") {
                val script = ResourceLoader.instance.load("$dir/$file".toGodotString())
                val method = name.toStringName()
                script.call(method).use { list -> list.toArray().use { expect(it.size == expected) { "size=${it.size}" } } }
                val os = Os.instance
                val before = os.getStaticMemoryUsage().toLong()
                repeat(5_000) { script.call(method).close() }
                val growth = os.getStaticMemoryUsage().toLong() - before
                println("[VirtualReturnCheck] $name growth=$growth bytes over 5000 calls")
                expect(growth < 100_000) { "$name grew the static memory by $growth bytes" }
            }
        }
    }
}
