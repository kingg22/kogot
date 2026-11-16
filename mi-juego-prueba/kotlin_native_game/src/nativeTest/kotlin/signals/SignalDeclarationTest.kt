@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, ExperimentalGodotApi::class)

package io.github.kingg22.godot.test.signals

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.core.node.Button
import io.github.kingg22.godot.api.core.node.HSlider
import io.github.kingg22.godot.api.core.node.ItemList
import io.github.kingg22.godot.api.signal.connect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertTrue

/** The signal property itself (issue #190): identity, name, owner, inheritance and the typed parameters. */
class SignalDeclarationTest {
    @Test
    fun aSignalIsTheSameTypedSignalOnEveryAccess() {
        val button = Button()
        assertTrue(button.pressed === button.pressed, "pressed gave two different TypedSignal")
        assertTrue(button.toggled === button.toggled, "toggled gave two different TypedSignal")
        button.queueFree()
    }

    @Test
    fun aSignalKnowsItsGodotNameAndItsOwner() {
        val button = Button()
        assertTrue(button.toggled.name == "toggled", "name=${button.toggled.name}")
        assertTrue(button.childEnteredTree.name == "child_entered_tree", "name=${button.childEnteredTree.name}")
        assertTrue(button.treeExited.name == "tree_exited", "snake_case lost: ${button.treeExited.name}")
        assertTrue(button.toggled.owner === button, "owner is not the button")
        button.queueFree()
    }

    @Test
    fun twoInstancesDoNotShareSignals() {
        val a = Button()
        val b = Button()
        assertTrue(a.pressed !== b.pressed, "same TypedSignal for two buttons")
        assertTrue(a.pressed.owner === a && b.pressed.owner === b, "wrong owner")
        a.queueFree()
        b.queueFree()
    }

    @Test
    fun signalsAreInheritedFromEveryAncestor() {
        val slider = HSlider()
        // HSlider < Slider < Range < Control < CanvasItem < Node < Object
        assertTrue(slider.valueChanged.name == "value_changed", "Range: ${slider.valueChanged.name}")
        assertTrue(slider.focusEntered.name == "focus_entered", "Control: ${slider.focusEntered.name}")
        assertTrue(slider.visibilityChanged.name == "visibility_changed", "CanvasItem")
        assertTrue(slider.treeEntered.name == "tree_entered", "Node")
        assertTrue(slider.scriptChanged.name == "script_changed", "Object")
        slider.queueFree()
    }

    @Test
    fun theParametersKeepTheirJsonNamesAndTypesInThePropertyType() {
        val list = ItemList()
        // Compiles only if the property is TypedSignal<(index: Long, atPosition: Vector2, mouseButtonIndex: Long) -> Unit>.
        val connection = list.itemClicked.connect { index: Long, atPosition: Vector2, mouseButtonIndex: Long ->
            println("[SignalsTest] unreachable $index $atPosition $mouseButtonIndex")
        }
        assertTrue(connection.isConnected, "not connected")
        connection.close()
        list.queueFree()
    }
}
