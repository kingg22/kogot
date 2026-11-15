@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Color
import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.singleton.Os
import io.github.kingg22.godot.binding.instantiate
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
        variantPaths()
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

    // The Any? below is deliberate: it is the caller-owned Variant the overloads must not close.
    @Suppress("MUST_BE_VARIANT_UNKNOWN_TYPE")
    private fun variantPaths() = with(checks) {
        val emitter = Node()
        val signalName = "native_leak_probe".toStringName()
        emitter.addUserSignal("native_leak_probe")
        val text = "an argument long enough to be its own engine allocation (#219)"
        check("a Variant passed as Any? stays owned by the caller (#219)") {
            Variant.from(42L).use { variant ->
                val any: Any? = variant
                val _ = emitter.emitSignal(signalName, any)
                Signal(emitter, signalName).use { it.emit(any) }
                expect(variant.toString() == "42") { "the caller's Variant was closed: $variant" }
            }
        }
        noGrowth("GodotObject.emitSignal(name, Any?) (#219)") { val _ = emitter.emitSignal(signalName, text) }
        Signal(emitter, signalName).use { signal ->
            noGrowth("Signal.emit(Any?) (#219)") { signal.emit(text) }
        }
        Callable { -> text }.use { callable ->
            noGrowth("a Callable lambda's return value (#219)") { callable.call().close() }
        }
        val user = instantiate<DowncastDerived>()
        "baseValue".toStringName().use { name ->
            noGrowth("an @Export getter (#219)") { user.get(name).close() }
        }
        "baseGreeting".toStringName().use { name ->
            noGrowth("an @ExportMethod return value (#219)") { user.call(name).close() }
        }
        user.queueFree()
        signalName.close()
        ObjectBinding.destroyRaw(emitter.rawPtr)
        // A Color is a value type whose own storage is never freed yet (#216), so only the engine memory is
        // measured here: that is where the leaked GodotString of the String constructor shows.
        noGrowth("Color(code: String) constructor (#219)", cHeap = false) { val _ = Color("#ff8800") }
        noGrowth("Color(code: String, alpha) constructor (#219)", cHeap = false) { val _ = Color("#ff8800", 0.5) }
    }

    /**
     * [call] 20,000 times must grow neither the C heap ([cHeap] `false` skips it) nor the engine memory by more than
     * a small margin.
     */
    private fun Checks.noGrowth(name: String, cHeap: Boolean = true, call: () -> Unit) = check("$name does not leak") {
        val os = Os.instance
        repeat(100) { call() } // warm up: arenas, lazy method binds
        val heapBefore = cHeapInUse()
        val engineBefore = os.getStaticMemoryUsage().toLong()
        repeat(20_000) { call() }
        val heap = cHeapInUse() - heapBefore
        val engine = os.getStaticMemoryUsage().toLong() - engineBefore
        println("[NativeLeakCheck] $name: C heap +$heap bytes, engine +$engine bytes over 20000 calls")
        expect((!cHeap || heap < 100_000) && engine < 100_000) {
            "C heap grew by $heap bytes, engine by $engine bytes"
        }
    }
}
