@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, ExperimentalGodotApi::class)

package io.github.kingg22.godot.test.signals

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Color
import io.github.kingg22.godot.api.builtin.RID
import io.github.kingg22.godot.api.builtin.VariantDictionary
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.node.AnimationPlayer
import io.github.kingg22.godot.api.core.node.Area2D
import io.github.kingg22.godot.api.core.node.Button
import io.github.kingg22.godot.api.core.node.ColorPicker
import io.github.kingg22.godot.api.core.node.Control
import io.github.kingg22.godot.api.core.node.HSlider
import io.github.kingg22.godot.api.core.node.ItemList
import io.github.kingg22.godot.api.core.node.LineEdit
import io.github.kingg22.godot.api.core.node.NavigationAgent2D
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.refcounted.InputEventKey
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.signal.connect
import io.github.kingg22.godot.api.signal.emit
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The typed `emit` / `connect` for every argument type and arity found in the API (issue #190). */
class SignalEmitReceiveTest {
    @Test
    fun zeroArgumentsResourceChanged() {
        val resource = Resource()
        var fired = 0
        resource.changed.connect { fired++ }.use {
            assertTrue(resource.changed.emit() == GodotError.OK, "emit did not return OK")
            assertTrue(fired == 1, "fired $fired times")
        }
    }

    @Test
    fun booleanBaseButtonToggled() {
        val button = Button()
        val received = mutableListOf<Boolean>()
        button.toggled.connect { toggledOn -> received += toggledOn }.use {
            button.toggled.emit(true).ok()
            button.toggled.emit(false).ok()
            assertEquals(listOf(true, false), received, "received=$received")
        }
        button.queueFree()
    }

    @Test
    fun doubleRangeValueChanged() {
        val slider = HSlider()
        var received = Double.NaN
        slider.valueChanged.connect { value -> received = value }.use {
            slider.valueChanged.emit(-3.25).ok()
            assertTrue(received == -3.25, "received=$received")
        }
        slider.queueFree()
    }

    @Test
    fun stringLineEditTextSubmitted() {
        val edit = LineEdit()
        var received: String? = null
        edit.textSubmitted.connect { newText -> received = newText }.use {
            edit.textSubmitted.emit("Ñandú 🎮").ok()
            assertTrue(received == "Ñandú 🎮", "received=$received")
        }
        edit.queueFree()
    }

    @Test
    fun longItemListItemSelectedNegativeAndLarge() {
        val list = ItemList()
        val received = mutableListOf<Long>()
        list.itemSelected.connect { index -> received += index }.use {
            list.itemSelected.emit(7L).ok()
            list.itemSelected.emit(-1L).ok()
            list.itemSelected.emit(Long.MAX_VALUE).ok()
            assertEquals(listOf(7L, -1L, Long.MAX_VALUE), received, "received=$received")
        }
        list.queueFree()
    }

    @Test
    fun colorColorPickerColorChanged() {
        val picker = ColorPicker()
        var received: Color? = null
        val sent = Color(0.25, 0.5, 0.75, 1.0)
        picker.colorChanged.connect { color -> received = color }.use {
            picker.colorChanged.emit(sent).ok()
            assertTrue(received == sent, "received=$received, sent $sent")
        }
        picker.queueFree()
    }

    @Test
    fun stringNameAnimationPlayerCurrentAnimationChanged() {
        val player = AnimationPlayer()
        var received: String? = null
        player.currentAnimationChanged.connect { name -> received = name.toString() }.use {
            player.currentAnimationChanged.emit("run".toStringName()).ok()
            assertTrue(received == "run", "received=$received")
        }
        player.queueFree()
    }

    @Test
    fun vector2NavigationAgent2DVelocityComputed() {
        val agent = NavigationAgent2D()
        var received: Vector2? = null
        val sent = Vector2(3.0, -4.5)
        agent.velocityComputed.connect { safeVelocity -> received = safeVelocity }.use {
            agent.velocityComputed.emit(sent).ok()
            assertTrue(received == sent, "received=$received, sent $sent")
        }
        agent.queueFree()
    }

    @Test
    fun twoArgumentsAnimationPlayerAnimationChanged() {
        val player = AnimationPlayer()
        var received: Pair<String, String>? = null
        player.animationChanged.connect { oldName, newName -> received = oldName.toString() to newName.toString() }
            .use {
                player.animationChanged.emit("idle".toStringName(), "walk".toStringName()).ok()
                assertTrue(received == "idle" to "walk", "received=$received")
            }
        player.queueFree()
    }

    @Test
    fun twoArgumentsOfDifferentTypesItemListEmptyClicked() {
        val list = ItemList()
        var position: Vector2? = null
        var button = -1L
        list.emptyClicked.connect { atPosition, mouseButtonIndex ->
            position = atPosition
            button = mouseButtonIndex
        }.use {
            list.emptyClicked.emit(Vector2(10.0, 20.0), 2L).ok()
            assertTrue(position == Vector2(10.0, 20.0) && button == 2L, "position=$position button=$button")
        }
        list.queueFree()
    }

    @Test
    fun threeArgumentsItemListItemClicked() {
        val list = ItemList()
        var received: Triple<Long, Vector2, Long>? = null
        list.itemClicked.connect { index, atPosition, mouseButtonIndex ->
            received = Triple(index, atPosition, mouseButtonIndex)
        }.use {
            list.itemClicked.emit(4L, Vector2(1.5, 2.5), 1L).ok()
            assertTrue(received == Triple(4L, Vector2(1.5, 2.5), 1L), "received=$received")
        }
        list.queueFree()
    }

    @Test
    fun fourArgumentsWithARidAndANode2DAreaBodyShapeEntered() {
        val area = Area2D()
        val body = Node2D()
        var shape = -1L
        var local = -1L
        var gotBody: Node2D? = null
        area.bodyShapeEntered.connect { _, theBody, bodyShapeIndex, localShapeIndex ->
            gotBody = theBody
            shape = bodyShapeIndex
            local = localShapeIndex
        }.use {
            area.bodyShapeEntered.emit(RID(), body, 3L, 5L).ok()
            assertTrue(sameObject(gotBody, body), "body=$gotBody")
            assertTrue(shape == 3L && local == 5L, "shape=$shape local=$local")
        }
        body.queueFree()
        area.queueFree()
    }

    @Test
    fun anEngineObjectArgumentNodeChildEnteredTreeEmittedByHand() {
        val parent = Node()
        val child = Node()
        var received: Node? = null
        parent.childEnteredTree.connect { node -> received = node }.use {
            parent.childEnteredTree.emit(child).ok()
            assertTrue(sameObject(received, child), "received=$received")
        }
        child.queueFree()
        parent.queueFree()
    }

    @Test
    fun aSubclassAsArgumentAreaEnteredReceivesTheOtherArea2D() {
        val area = Area2D()
        val other = Area2D()
        var received: Area2D? = null
        area.areaEntered.connect { theArea -> received = theArea }.use {
            area.areaEntered.emit(other).ok()
            assertTrue(sameObject(received, other), "received=$received")
        }
        other.queueFree()
        area.queueFree()
    }

    @Test
    fun anInputEventArgumentControlGuiInput() {
        val control = Control()
        val event = InputEventKey()
        var received: GodotObject? = null
        control.guiInput.connect { theEvent -> received = theEvent }.use {
            control.guiInput.emit(event).ok()
            assertTrue(sameObject(received, event), "received=$received")
        }
        control.queueFree()
    }

    @Test
    fun aDictionaryArgumentNavigationAgent2DWaypointReached() {
        val agent = NavigationAgent2D()
        var size = -1
        agent.waypointReached.connect { details -> size = details.size }.use {
            VariantDictionary().use { sent ->
                agent.waypointReached.emit(sent).ok()
                assertTrue(size == 0, "size=$size")
            }
        }
        agent.queueFree()
    }

    @Test
    fun anUntypedCallableCanBeConnectedToATypedSignal() {
        val button = Button()
        var received: Boolean? = null
        val callable = Callable { toggledOn: Boolean -> received = toggledOn }
        button.toggled.connect(callable).use {
            button.toggled.emit(true).ok()
            assertTrue(received == true, "received=$received")
        }
        callable.close()
        button.queueFree()
    }

    @Test
    fun gdScriptStyleEmitSignalReachesTheTypedHandler() {
        val button = Button()
        var received: Boolean? = null
        button.toggled.connect { toggledOn -> received = toggledOn }.use {
            button.emitSignal("toggled".toStringName(), true).ok()
            assertTrue(received == true, "received=$received")
        }
        button.queueFree()
    }

    @Test
    fun emitSignalWithKotlinValuesOneArgumentPerOverloadShape() {
        val player = AnimationPlayer()
        var received: Pair<String, String>? = null
        player.animationChanged.connect { oldName, newName -> received = oldName.toString() to newName.toString() }
            .use {
                player.emitSignal("animation_changed".toStringName(), "idle".toStringName(), "walk".toStringName())
                    .ok()
                assertTrue(received == "idle" to "walk", "received=$received")
            }
        player.queueFree()
    }

    @Test
    fun emitSignalWithValuesOfDifferentKotlinTypesItemListItemClicked() {
        val list = ItemList()
        var received: Triple<Long, Vector2, Long>? = null
        list.itemClicked.connect { index, position, button -> received = Triple(index, position, button) }.use {
            list.emitSignal("item_clicked".toStringName(), 4, Vector2(1.5, 2.5), 1L).ok()
            assertTrue(received == Triple(4L, Vector2(1.5, 2.5), 1L), "received=$received")
        }
        list.queueFree()
    }

    @Test
    fun emitSignalWithNoVariadicArgumentStillResolvesToTheVariantOverload() {
        val resource = Resource()
        var fired = 0
        resource.changed.connect { fired++ }.use {
            resource.emitSignal("changed".toStringName()).ok()
            assertTrue(fired == 1, "fired $fired times")
        }
    }
}
