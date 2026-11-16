@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, ExperimentalGodotApi::class)

package io.github.kingg22.godot.test.signals

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.node.Button
import io.github.kingg22.godot.api.core.node.Control
import io.github.kingg22.godot.api.core.node.HSlider
import io.github.kingg22.godot.api.core.node.LineEdit
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.signal.connect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Values the engine itself sends when something happens (issue #190). Nodes that must be inside the tree are added
 * under the SceneTree root through [inScene].
 */
class EngineOriginatedSignalsTest {
    @Test
    fun childEnteredTreeAndChildExitingTreeCarryTheChild() {
        val parent = Node().inScene()
        val child = Node()
        var entered: Node? = null
        var exiting: Node? = null
        val a = parent.childEnteredTree.connect { node -> entered = node }
        val b = parent.childExitingTree.connect { node -> exiting = node }
        parent.addChild(node = child)
        assertTrue(sameObject(entered, child), "child_entered_tree gave $entered")
        parent.removeChild(child)
        assertTrue(sameObject(exiting, child), "child_exiting_tree gave $exiting")
        a.close()
        b.close()
        child.queueFree()
        parent.queueFree()
    }

    @Test
    fun treeEnteredReadyTreeExitingAndTreeExitedHappenInOrder() {
        val parent = Node().inScene()
        val node = Node()
        val order = mutableListOf<String>()
        val connections = listOf(
            node.treeEntered.connect { order += "tree_entered" },
            node.ready.connect { order += "ready" },
            node.treeExiting.connect { order += "tree_exiting" },
            node.treeExited.connect { order += "tree_exited" },
        )
        parent.addChild(node = node)
        parent.removeChild(node)
        val expected = listOf("tree_entered", "ready", "tree_exiting", "tree_exited")
        assertEquals(expected, order, "order=$order, expected $expected")
        connections.forEach { it.close() }
        node.queueFree()
        parent.queueFree()
    }

    @Test
    fun renamedCarriesNoArgumentsAndFiresInsideTheTree() {
        val node = Node().inScene()
        var renamed = 0
        val connection = node.renamed.connect { renamed++ }
        node.name = "Renamed".toStringName()
        assertTrue(renamed == 1, "renamed fired $renamed times")
        connection.close()
        node.queueFree()
    }

    @Test
    fun toggledCarriesTheNewStateOfTheButton() {
        val button = Button().inScene()
        button.toggleMode = true
        val states = mutableListOf<Boolean>()
        val connection = button.toggled.connect { toggledOn -> states += toggledOn }
        button.buttonPressed = true
        button.buttonPressed = false
        assertEquals(listOf(true, false), states, "states=$states")
        connection.close()
        button.queueFree()
    }

    @Test
    fun valueChangedCarriesTheDoubleTheSliderGot() {
        val slider = HSlider().inScene()
        // The default step is 1.0: the slider rounds what it is given to a multiple of it.
        slider.step = 0.5
        var received = Double.NaN
        val connection = slider.valueChanged.connect { value -> received = value }
        slider.value = 42.5
        assertTrue(received == 42.5, "received=$received")
        connection.close()
        slider.queueFree()
    }

    @Test
    fun textChangeRejectedCarriesTheRejectedTextAsAKotlinString() {
        val edit = LineEdit().inScene()
        edit.maxLength = 3
        var rejected: String? = null
        val connection = edit.textChangeRejected.connect { rejectedSubstring -> rejected = rejectedSubstring }
        edit.insertTextAtCaret("kogot")
        assertTrue(rejected == "ot", "rejected=$rejected")
        connection.close()
        edit.queueFree()
    }

    @Test
    fun propertyListChangedDeclaredByObjectFiresForAnySubclass() {
        val node = Node2D()
        var fired = 0
        val connection = node.propertyListChanged.connect { fired++ }
        node.notifyPropertyListChanged()
        assertTrue(fired == 1, "fired $fired times")
        connection.close()
        node.queueFree()
    }

    @Test
    fun hiddenAndVisibilityChangedFireWhenAControlIsHidden() {
        val control = Control().inScene()
        var hidden = 0
        var changed = 0
        val a = control.hidden.connect { hidden++ }
        val b = control.visibilityChanged.connect { changed++ }
        control.visible = false
        assertTrue(hidden == 1, "hidden fired $hidden times")
        assertTrue(changed >= 1, "visibility_changed fired $changed times")
        a.close()
        b.close()
        control.queueFree()
    }

    @Test
    fun changedOfResourceIsASignalOfARefCounted() {
        val resource = Resource()
        var fired = 0
        val connection = resource.changed.connect { fired++ }
        resource.emitChanged()
        assertTrue(fired == 1, "fired $fired times")
        connection.close()
    }

    @Test
    fun setBlockSignalsTrueStopsTheDelivery() {
        val button = Button()
        button.toggleMode = true
        var fired = 0
        val connection = button.toggled.connect { fired++ }
        button.setBlockSignals(true)
        button.buttonPressed = true
        button.setBlockSignals(false)
        assertTrue(fired == 0, "delivered $fired while blocked")
        connection.close()
        button.queueFree()
    }
}
