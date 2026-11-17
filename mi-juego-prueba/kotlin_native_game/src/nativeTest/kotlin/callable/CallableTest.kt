@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.callable

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.ExportMethod
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A node with an `@ExportMethod` to call by name through the engine. */
@Godot
class CallableProbe(nativePtr: COpaquePointer) : Node(nativePtr) {
    @ExportMethod
    fun addTwo(a: Int, b: Int): Int = a + b
}

/**
 * The typed `Callable { ... }` factories: Godot passes the arguments as `Variant`s and each one is converted to the
 * type the lambda declares. Ported from `CallableUsage` and the `Callable` part of `TestOne`.
 */
class CallableTest {
    @Test
    fun callableWithoutArgumentsReturnsNilForAUnitLambda() {
        var calls = 0
        Callable { calls += 1 }.use { callable ->
            callable.call().use { assertTrue(it.isNil(), "returned ${it.getType()}") }
        }
        assertEquals(1, calls, "calls=$calls")
    }

    @Test
    fun callableReturnsWhatTheLambdaReturns() {
        Callable { id: Long -> id }.use { callable ->
            callable.call(15L).use { assertEquals(15L, it.toIntOrNull(), "returned $it") }
        }
    }

    /** Godot passes INT as `Long` and STRING as `GodotString`: both are converted for the lambda. */
    @Test
    fun callableConvertsIntAndStringArguments() {
        Callable { count: Int, name: String -> name.repeat(count) }.use { callable ->
            callable.call(3L, "ab").use { assertEquals("ababab", it.toString()) }
        }
    }

    @Test
    fun callableConvertsVectorAndFloatArguments() {
        Callable { position: Vector2, scale: Float -> position * scale.toDouble() }.use { callable ->
            callable.call(Vector2(1.0, 2.0), 2.0).use { result ->
                val scaled = result.getValue<Vector2>()
                assertEquals(2.0f, scaled.x, "scaled=$scaled")
                assertEquals(4.0f, scaled.y, "scaled=$scaled")
            }
        }
    }

    @Test
    fun exportMethodIsCalledByNameThroughTheEngine() {
        val probe = instantiate<CallableProbe>()
        "addTwo".toStringName().use { name ->
            probe.call(name, 3L, 4).use { assertEquals(7L, it.toIntOrNull(), "addTwo returned $it") }
        }
        ObjectBinding.destroyRaw(probe.rawPtr)
    }
}
