package io.github.kingg22.godot.test

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.toGodotString
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.refcounted.GDScript
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.singleton.ClassDB
import io.github.kingg22.godot.api.singleton.Performance
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Variant round trips and ownership: values survive the engine and objects are not leaked. */
class VariantTest {
    @Test
    fun scalarsRoundTrip() {
        Variant.from(42L).use { assertEquals(42L, it.getValue<Long>()) }
        Variant.from(1.5).use { assertEquals(1.5, it.getValue<Double>()) }
        Variant.from(true).use { assertTrue(it.getValue<Boolean>()) }
        Variant.from("héllo").use { assertEquals("héllo", it.getValue<String>()) }
    }

    @Test
    fun resourceHoldsExactlyOneReferenceFromKotlin() {
        val resource = Resource()
        assertEquals(1, resource.getReferenceCount())
        Variant.from(resource).use { assertTrue(it.getValue<Resource>() === resource) }
        assertEquals(1, resource.getReferenceCount())
    }

    @Test
    fun constructingAndDestroyingNodesDoesNotLeakObjects() {
        val before = objectCount()
        repeat(100) { ObjectBinding.destroyRaw(Node().rawPtr) }
        assertEquals(before, objectCount(), "engine objects alive before and after")
    }

    private fun objectCount() = Performance.instance.getMonitor(Performance.Monitor.OBJECT_COUNT)
}

/** Registration of the `@Godot` fixtures, seen through `ClassDB` reflection. */
class RegistrationTest {
    private fun parentOf(name: String): String? {
        if (!name.toStringName().use { ClassDB.instance.classExists(it) }) return null
        return name.toStringName().use { n -> ClassDB.instance.getParentClass(n).use { it.toString() } }
    }

    @Test
    fun registeredClassHasItsParent() {
        assertEquals("Node", parentOf("HarnessProbe"))
    }

    @Test
    fun exportedMethodIsVisibleToTheEngine() {
        val probe = instantiate<HarnessProbe>()
        val has = "triple".toStringName().use { probe.hasMethod(it) }
        assertTrue(has, "triple is not registered as a method")
    }

    @Test
    fun aParentDeclaredAfterItsChildIsStillRegistered() {
        assertEquals("Node", parentOf("OrderParentLast"))
    }

    // Red until the processor registers parent-first (#254): Godot rejects the child declared first.
    @Test
    @kotlin.test.Ignore
    fun aChildDeclaredBeforeItsParentIsRegistered_issue254() {
        assertEquals("OrderParentLast", parentOf("OrderChildFirst"))
    }
}

/** Calls from GDScript into Kotlin-registered classes (dynamic calls) and virtual dispatch from the engine. */
class ScriptInteropTest {
    private val source = """
        extends Node
        func add_one(v):
        ${'\t'}return v + 1
        func triple_via_kotlin(target, v):
        ${'\t'}return target.triple(v)
    """.trimIndent()

    private fun scripted(): Node {
        val script = GDScript()
        script.sourceCode = source.toGodotString()
        assertEquals(0, script.reload().value, "the GDScript did not compile")
        return Node().also { it.setScript(script) }
    }

    @Test
    fun gdscriptRunsInsideTheHarness() {
        val node = scripted()
        Variant.from(41L).use { arg ->
            node.call("add_one".toStringName(), arg).use { assertEquals(42L, it.getValue<Long>()) }
        }
    }

    @Test
    fun gdscriptCallsAKotlinExportedMethod() {
        val node = scripted()
        val probe = instantiate<HarnessProbe>()
        Variant.from(probe).use { target ->
            Variant.from(7L).use { value ->
                node.call("triple_via_kotlin".toStringName(), target, value).use {
                    assertEquals(21L, it.getValue<Long>())
                }
            }
        }
    }

    @Test
    fun readyVirtualIsDispatchedToKotlin() {
        val probe = instantiate<HarnessProbe>()
        probe.notification(Node.NOTIFICATION_READY.toInt())
        assertEquals(1, probe.readyCalls)
    }
}

/** Typed signals and callables. */
class SignalAndCallableTest {
    @Test
    fun userSignalReachesAKotlinCallable() {
        val emitter = Node()
        val name = "harness_probe".toStringName()
        emitter.addUserSignal("harness_probe")
        var received: Long? = null
        Callable { value: Long -> received = value }.use { callable ->
            assertEquals(0L, emitter.connect(name, callable).value)
            Variant.from(7L).use { emitter.emitSignal(name, it) }
        }
        assertEquals(7L, received)
    }

    @Test
    fun kotlinLambdaCallableIsInvokedByTheEngine() {
        var calls = 0
        Callable { calls++ }.use { it.call().close() }
        assertEquals(1, calls)
    }
}
