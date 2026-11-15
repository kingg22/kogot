@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.singleton.Os
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.linux.mallinfo

/**
 * Bytes in use in the C heap (`malloc`): where `nativeHeap` puts the storage of every builtin wrapper (a `Variant` is
 * 24 bytes there) and where the engine allocates too. `OS.get_static_memory_usage()` only sees the engine's own
 * allocations, so a leaked wrapper whose engine value owns nothing (a `Variant` holding a `Node`) shows only here.
 */
private fun cHeapInUse(): Long = mallinfo().useContents { uordblks.toLong() + hblkhd.toLong() }

/**
 * Live checks of the native leaks of #214 (generated constructors) and #219 (Variant paths)
 * (`native_leak_check.tscn`): each case runs 20,000 times and must not grow the C heap or the engine memory.
 */
@Godot class NativeLeakCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("NativeLeakCheck")

    override fun _ready() {
        constructors()
        getTree().quit(checks.finish())
    }

    private fun constructors() = with(checks) {
        check("a constructed Resource holds exactly the reference Kotlin owns, and an engine holder adds one (#214)") {
            val resource = Resource()
            expect(resource.getReferenceCount() == 1) { "refcount after construction=${resource.getReferenceCount()}" }
            GodotArray<Resource>().use { holder ->
                holder.add(resource)
                expect(resource.getReferenceCount() == 2) { "refcount while held=${resource.getReferenceCount()}" }
            }
            expect(resource.getReferenceCount() == 1) { "refcount after the holder=${resource.getReferenceCount()}" }
            Variant.from(resource).use { expect(it.getValue<Resource>() === resource) { "read back another wrapper" } }
        }
        noGrowth("Node() constructor (#214)") { ObjectBinding.destroyRaw(Node().rawPtr) }
    }

    /** [call] 20,000 times must grow neither the C heap nor the engine memory by more than a small margin. */
    private fun Checks.noGrowth(name: String, call: () -> Unit) = check("$name does not leak") {
        val os = Os.instance
        repeat(100) { call() } // warm up: arenas, lazy method binds
        val heapBefore = cHeapInUse()
        val engineBefore = os.getStaticMemoryUsage().toLong()
        repeat(20_000) { call() }
        val heap = cHeapInUse() - heapBefore
        val engine = os.getStaticMemoryUsage().toLong() - engineBefore
        println("[NativeLeakCheck] $name: C heap +$heap bytes, engine +$engine bytes over 20000 calls")
        expect(heap < 100_000 && engine < 100_000) { "C heap grew by $heap bytes, engine by $engine bytes" }
    }
}
