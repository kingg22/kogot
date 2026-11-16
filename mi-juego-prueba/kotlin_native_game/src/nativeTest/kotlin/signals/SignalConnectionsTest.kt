@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, ExperimentalGodotApi::class)

package io.github.kingg22.godot.test.signals

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.node.Button
import io.github.kingg22.godot.api.signal.connect
import io.github.kingg22.godot.api.signal.emit
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The life of a connection (issue #190). The asynchronous part of the original check (DEFERRED delivery, Timer.timeout,
 * SceneTree.process_frame) needs engine frames and is not covered here.
 */
class SignalConnectionsTest {
    @Test
    fun hasConnectionsAndIsConnectedFollowConnectAndClose() {
        val button = Button()
        assertTrue(!button.toggled.hasConnections, "connected before connect")
        val connection = button.toggled.connect { _ -> }
        assertTrue(button.toggled.hasConnections && connection.isConnected, "not connected after connect")
        connection.close()
        assertTrue(!button.toggled.hasConnections && !connection.isConnected, "still connected after close")
        button.queueFree()
    }

    @Test
    fun aClosedConnectionReceivesNothingAndClosingTwiceIsHarmless() {
        val button = Button()
        var fired = 0
        val connection = button.toggled.connect { _ -> fired++ }
        button.toggled.emit(true).ok()
        connection.close()
        connection.close()
        assertTrue(button.toggled.emit(true) == GodotError.UNAVAILABLE, "emit to nobody did not say UNAVAILABLE")
        assertTrue(fired == 1, "fired $fired times")
        button.queueFree()
    }

    @Test
    fun severalHandlersAreAllCalledInConnectionOrder() {
        val button = Button()
        val order = mutableListOf<Int>()
        val a = button.toggled.connect { _ -> order += 1 }
        val b = button.toggled.connect { _ -> order += 2 }
        val c = button.toggled.connect { _ -> order += 3 }
        button.toggled.emit(true).ok()
        assertEquals(listOf(1, 2, 3), order, "order=$order")
        listOf(a, b, c).forEach { it.close() }
        button.queueFree()
    }

    @Test
    fun disconnectingOneHandlerLeavesTheOthers() {
        val button = Button()
        val order = mutableListOf<Int>()
        val a = button.toggled.connect { _ -> order += 1 }
        val b = button.toggled.connect { _ -> order += 2 }
        a.close()
        button.toggled.emit(true).ok()
        assertEquals(listOf(2), order, "order=$order")
        b.close()
        button.queueFree()
    }

    @Test
    fun oneShotDeliversOnceAndTheConnectionIsGone() {
        val button = Button()
        var fired = 0
        val connection = button.toggled.connect(GodotObject.ConnectFlags.ONE_SHOT) { _ -> fired++ }
        button.toggled.emit(true).ok()
        assertTrue(!connection.isConnected, "ONE_SHOT still connected after firing")
        assertTrue(button.toggled.emit(true) == GodotError.UNAVAILABLE, "the second emit found a listener")
        assertTrue(fired == 1, "fired $fired times")
        connection.close()
        button.queueFree()
    }

    @Test
    fun appendSourceObjectIsRejectedTheHandlerDoesNotDeclareThatArgument() {
        val button = Button()
        assertFailsWith<IllegalArgumentException>("connect with APPEND_SOURCE_OBJECT") {
            button.toggled.connect(GodotObject.ConnectFlags.APPEND_SOURCE_OBJECT) { _ -> }
        }
        assertTrue(!button.toggled.hasConnections, "a rejected connect left a connection")
        button.queueFree()
    }

    @Test
    fun asSignalGivesAnIndependentBuiltinSignalTheCallerCloses() {
        val button = Button()
        val connection = button.toggled.connect { _ -> }
        button.toggled.asSignal().use { signal ->
            assertTrue(signal.hasConnections(), "the copy sees no connection")
        }
        assertTrue(connection.isConnected, "closing the copy disconnected the handler")
        connection.close()
        button.queueFree()
    }

    @Test
    fun aClosedTypedSignalRefusesToBeUsedTheConnectionsItMadeStay() {
        val button = Button()
        val signal = button.toggled
        var fired = 0
        val connection = signal.connect { _ -> fired++ }
        signal.close()
        assertFailsWith<IllegalStateException>("emit on a closed signal") { signal.emit(true) }
        assertTrue(connection.isConnected, "close() disconnected the handler")
        button.emitSignal("toggled".toStringName(), true).ok()
        assertTrue(fired == 1, "fired $fired times")
        connection.close()
        button.queueFree()
    }

    @Test
    fun emitWithNobodyConnectedReturnsUnavailableWhichIsHowGodotAnswers() {
        val button = Button()
        val result = button.toggled.emit(true)
        assertTrue(result == GodotError.UNAVAILABLE, "result=$result")
        button.queueFree()
    }
}
