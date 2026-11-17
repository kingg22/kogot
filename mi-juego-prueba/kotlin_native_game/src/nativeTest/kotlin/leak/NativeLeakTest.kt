@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.leak

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Export
import io.github.kingg22.godot.api.annotations.ExportMethod
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Color
import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.builtin.Transform3D
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.toGodotString
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.singleton.Os
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val TAG = "NativeLeakTest"

/** An `@Export` property and an `@ExportMethod` with an argument, to measure their trampolines (#219). */
@Godot
class LeakTestProbe(nativePtr: COpaquePointer) : Node(nativePtr) {
    @Export var level: Int = 0

    @ExportMethod
    fun echo(value: Int): Int = value
}

/** A `@Godot` class with an `@Export` getter and an `@ExportMethod` return value to measure (#219). */
@Godot
class LeakTestBase(nativePtr: COpaquePointer) : Node(nativePtr) {
    @Export var baseValue: Int = 3

    @ExportMethod
    fun baseGreeting(): Int = 7
}

/**
 * Native leaks of #214 (generated constructors) and #219 (Variant paths), #232 (default arguments) and #216 (value
 * types): each case runs 20,000 times and must not grow the C heap or the engine memory.
 * Ported from the former `NativeLeakCheck` scene.
 */
class NativeLeakTest {
    // region #214 constructors

    /** #214: a constructed Resource holds exactly the reference Kotlin owns, and an engine holder adds one. */
    @Test
    fun constructedResourceHoldsExactlyTheKotlinReference() {
        val resource = Resource()
        assertEquals(1, resource.getReferenceCount(), "refcount after construction=${resource.getReferenceCount()}")
        GodotArray<Resource>().use { holder ->
            holder.add(resource)
            assertEquals(2, resource.getReferenceCount(), "refcount while held=${resource.getReferenceCount()}")
        }
        assertEquals(1, resource.getReferenceCount(), "refcount after the holder=${resource.getReferenceCount()}")
        Variant.from(resource).use {
            assertSame(it.getValue<Resource>(), resource, "read back another wrapper")
        }
    }

    /** #214 */
    @Test
    fun nodeConstructorDoesNotLeak() {
        assertNoGrowth("Node() constructor (#214)", TAG) { ObjectBinding.destroyRaw(Node().rawPtr) }
    }

    // endregion

    // region #219 Variant paths

    /** #219: a Variant passed as `Any?` stays owned by the caller. The `Any?` is deliberate. */
    @Suppress("MUST_BE_VARIANT_UNKNOWN_TYPE")
    @Test
    fun variantPassedAsAnyStaysOwnedByTheCaller() {
        val emitter = Node()
        val signalName = "native_leak_probe".toStringName()
        emitter.addUserSignal("native_leak_probe")
        Variant.from(42L).use { variant ->
            val any: Any = variant
            val _ = emitter.emitSignal(signalName, any)
            Signal(emitter, signalName).use { it.emit(any) }
            assertEquals("42", variant.toString(), "the caller's Variant was closed: $variant")
        }
        signalName.close()
        ObjectBinding.destroyRaw(emitter.rawPtr)
    }

    /** #219 */
    @Suppress("MUST_BE_VARIANT_UNKNOWN_TYPE")
    @Test
    fun emitSignalWithAnyDoesNotLeak() {
        val emitter = Node()
        val signalName = "native_leak_probe".toStringName()
        emitter.addUserSignal("native_leak_probe")
        val text = "an argument long enough to be its own engine allocation (#219)"
        assertNoGrowth("GodotObject.emitSignal(name, Any?) (#219)", TAG) {
            val _ = emitter.emitSignal(signalName, text)
        }
        signalName.close()
        ObjectBinding.destroyRaw(emitter.rawPtr)
    }

    /** #219 */
    @Test
    fun signalEmitWithAnyDoesNotLeak() {
        val emitter = Node()
        val signalName = "native_leak_probe".toStringName()
        emitter.addUserSignal("native_leak_probe")
        val text = "an argument long enough to be its own engine allocation (#219)"
        Signal(emitter, signalName).use { signal ->
            assertNoGrowth("Signal.emit(Any?) (#219)", TAG) { signal.emit(text) }
        }
        signalName.close()
        ObjectBinding.destroyRaw(emitter.rawPtr)
    }

    /** #219 */
    @Test
    fun callableLambdaReturnValueDoesNotLeak() {
        val text = "an argument long enough to be its own engine allocation (#219)"
        Callable { text }.use { callable ->
            assertNoGrowth("a Callable lambda's return value (#219)", TAG) { callable.call().close() }
        }
    }

    /** #219 */
    @Test
    fun exportGetterDoesNotLeak() {
        val user = instantiate<LeakTestBase>()
        "baseValue".toStringName().use { name ->
            assertNoGrowth("an @Export getter (#219)", TAG) { user[name].close() }
        }
        user.queueFree()
    }

    /** #219 */
    @Test
    fun exportMethodReturnValueDoesNotLeak() {
        val user = instantiate<LeakTestBase>()
        "baseGreeting".toStringName().use { name ->
            assertNoGrowth("an @ExportMethod return value (#219)", TAG) { user.call(name).close() }
        }
        user.queueFree()
    }

    /** #219 */
    @Test
    fun exportSetterDoesNotLeak() {
        val probe = instantiate<LeakTestProbe>()
        "level".toStringName().use { name ->
            Variant.from(5L).use { value ->
                assertNoGrowth("an @Export setter (#219)", TAG) { probe[name] = value }
                assertEquals(5, probe.level, "level=${probe.level}")
            }
        }
        ObjectBinding.destroyRaw(probe.rawPtr)
    }

    /** #219 */
    @Test
    fun exportMethodArgumentDoesNotLeak() {
        val probe = instantiate<LeakTestProbe>()
        "echo".toStringName().use { name ->
            Variant.from(7L).use { value ->
                assertNoGrowth("an @ExportMethod argument (#219)", TAG) { probe.call(name, value).close() }
            }
        }
        ObjectBinding.destroyRaw(probe.rawPtr)
    }

    /** #219: a Color is a value type, its own storage is freed by its Cleaner (#216), so the C heap is measured after a GC. */
    @Test
    fun colorCodeConstructorDoesNotLeak() {
        assertNoGrowth("Color(code: String) constructor (#219)", TAG, collect = true) { val _ = Color("#ff8800") }
    }

    /** #219 */
    @Test
    fun colorCodeAlphaConstructorDoesNotLeak() {
        assertNoGrowth("Color(code: String, alpha) constructor (#219)", TAG, collect = true) {
            val _ = Color("#ff8800", 0.5)
        }
    }

    // endregion

    // region #232 default arguments

    /** #232: `arguments: VariantArray = GodotArray()` built an engine Array per call and never closed it. */
    @Test
    fun addUserSignalDefaultArgumentsDoNotLeak() {
        val signal = "default_probe".toGodotString()
        assertNoGrowth("addUserSignal(name) with its default arguments array (#232)", TAG) {
            val node = Node()
            node.addUserSignal(signal)
            ObjectBinding.destroyRaw(node.rawPtr)
        }
        signal.close()
    }

    /** #232: `context: StringName = StringName("")`. */
    @Test
    fun trDefaultContextDoesNotLeak() {
        val node = Node()
        "default_probe_message".toStringName().use { message ->
            assertNoGrowth("tr(message) with its default context (#232)", TAG) { node.trAsGdStr(message).close() }
        }
        ObjectBinding.destroyRaw(node.rawPtr)
    }

    /** #232: `arguments: PackedStringArray = PackedStringArray()`. */
    @Test
    fun setRestartOnExitDefaultArgumentsDoNotLeak() {
        assertNoGrowth("OS.setRestartOnExit(restart) with its default arguments (#232)", TAG) {
            Os.instance.setRestartOnExit(false)
        }
    }

    // endregion

    // region #216 value types

    // Value-type builtins (Vector2, Color, Transform3D, ...) allocate their storage in nativeHeap, and nothing freed it
    // (#216). A Cleaner frees it once the wrapper is unreachable, so these are measured after a GC.

    /** #216 */
    @Test
    fun vector2ConstructorDoesNotLeak() {
        assertNoGrowth("Vector2(x, y) (#216)", TAG, collect = true) { val _ = Vector2(1.0, 2.0) }
    }

    /** #216 */
    @Test
    fun transform3dConstructorDoesNotLeak() {
        assertNoGrowth("Transform3D() (#216)", TAG, collect = true) { val _ = Transform3D() }
    }

    /** #216 */
    @Test
    fun variantGetValueVector2DoesNotLeak() {
        Variant.from(Vector2(3.0, 4.0)).use { variant ->
            assertNoGrowth("Variant.getValue<Vector2>() (#216)", TAG, collect = true) {
                val _ = variant.getValue<Vector2>()
            }
        }
    }

    /** #216 */
    @Test
    fun vector2PropertyReadDoesNotLeak() {
        val node = Node2D()
        assertNoGrowth("a Vector2 property read, Node2D.position (#216)", TAG, collect = true) {
            val _ = node.position
        }
        ObjectBinding.destroyRaw(node.rawPtr)
    }

    /** #216: cost of the Cleaner per value, reported and not asserted (compare runs with and without #216). */
    @Test
    fun cleanerCostIsReported() {
        val timedNode = Node2D()
        printTiming("Vector2(x, y)", TAG) { val _ = Vector2(1.0, 2.0) }
        printTiming("Node2D.position read", TAG) { val _ = timedNode.position }
        ObjectBinding.destroyRaw(timedNode.rawPtr)
    }

    /** #216: a value type keeps its value until it is unreachable, and a borrowed view is never freed. */
    @Test
    fun valueTypeKeepsItsValueAndBorrowedViewIsNeverFreed() {
        val kept = List(1_000) { Vector2(it.toDouble(), -it.toDouble()) }
        settleCleaners()
        kept.forEachIndexed { i, v ->
            assertTrue(v.x == i.toFloat() && v.y == -i.toFloat(), "element $i is $v")
        }
        val transform = Transform3D()
        val origin = transform.origin
        settleCleaners()
        assertEquals(0.0f, origin.x, "origin=$origin")
    }

    // endregion
}
