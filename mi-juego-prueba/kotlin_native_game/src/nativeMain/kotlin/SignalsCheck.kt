@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, ExperimentalGodotApi::class)

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.annotations.Godot
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
import io.github.kingg22.godot.api.core.node.Timer
import io.github.kingg22.godot.api.core.refcounted.InputEventKey
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.signal.SignalConnection
import io.github.kingg22.godot.api.signal.connect
import io.github.kingg22.godot.api.signal.emit
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Live checks of the signals generated for the engine classes (issue #190), run inside the real engine
 * (`signals_check.tscn`): each one is a property of its class (`button.toggled`), inherited by the subclasses, and
 * typed through the JSON (`TypedSignal<(toggledOn: Boolean) -> Unit>`).
 *
 * Three groups: values the engine itself sends when something happens (`addChild`, `value = ..`, `buttonPressed = ..`),
 * the typed `emit` / `connect` for every argument type and arity found in the API, and the life of a connection.
 * The last one is asynchronous (a `Timer`, `SceneTree.processFrame`, a `DEFERRED` connection) and finishes the scene.
 */
@Godot
class SignalsCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("SignalsCheck")

    // State of the asynchronous checks.
    private var frames = 0
    private var finished = false
    private var timerFired = false
    private var deferredValue: Long? = null
    private var deferredDuringEmit = false

    // Connections of the asynchronous checks: they live until the scene is freed.
    private val liveConnections = mutableListOf<SignalConnection>()

    override fun _ready() {
        declaration()
        engineOriginated()
        emitAndReceive()
        emitSignalOverloads()
        connections()
        asynchronous()
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────────────────────────────

    /** A node that belongs to the scene, so it is freed with it and `ready` / `tree_entered` really happen. */
    private fun <T : Node> T.inScene(): T = also { this@SignalsCheck.addChild(node = it) }

    /** An emission Godot rejected is a failure of the check, not a value to drop. */
    private fun GodotError.ok() = expectOk(this)

    private fun expectOk(error: GodotError) {
        if (error != GodotError.OK) throw AssertionError("emit returned $error")
    }

    private fun sameObject(a: GodotObject?, b: GodotObject?) =
        a != null && b != null && a.getInstanceId() == b.getInstanceId()

    // ── the property itself ───────────────────────────────────────────────────────────────────────────────────────

    private fun declaration() = with(checks) {
        check("a signal is the same TypedSignal on every access") {
            val button = Button()
            expect(button.pressed === button.pressed) { "pressed gave two different TypedSignal" }
            expect(button.toggled === button.toggled) { "toggled gave two different TypedSignal" }
            button.queueFree()
        }
        check("a signal knows its Godot name and its owner") {
            val button = Button()
            expect(button.toggled.name == "toggled") { "name=${button.toggled.name}" }
            expect(button.childEnteredTree.name == "child_entered_tree") { "name=${button.childEnteredTree.name}" }
            expect(button.treeExited.name == "tree_exited") { "snake_case lost: ${button.treeExited.name}" }
            expect(button.toggled.owner === button) { "owner is not the button" }
            button.queueFree()
        }
        check("two instances do not share signals") {
            val a = Button()
            val b = Button()
            expect(a.pressed !== b.pressed) { "same TypedSignal for two buttons" }
            expect(a.pressed.owner === a && b.pressed.owner === b) { "wrong owner" }
            a.queueFree()
            b.queueFree()
        }
        check("signals are inherited from every ancestor") {
            val slider = HSlider()
            // HSlider < Slider < Range < Control < CanvasItem < Node < Object
            expect(slider.valueChanged.name == "value_changed") { "Range: ${slider.valueChanged.name}" }
            expect(slider.focusEntered.name == "focus_entered") { "Control: ${slider.focusEntered.name}" }
            expect(slider.visibilityChanged.name == "visibility_changed") { "CanvasItem" }
            expect(slider.treeEntered.name == "tree_entered") { "Node" }
            expect(slider.scriptChanged.name == "script_changed") { "Object" }
            slider.queueFree()
        }
        check("the parameters keep their JSON names and types in the property type") {
            val list = ItemList()
            // Compiles only if the property is TypedSignal<(index: Long, atPosition: Vector2, mouseButtonIndex: Long) -> Unit>.
            val connection = list.itemClicked.connect { index: Long, atPosition: Vector2, mouseButtonIndex: Long ->
                println("[SignalsCheck] unreachable $index $atPosition $mouseButtonIndex")
            }
            expect(connection.isConnected) { "not connected" }
            connection.close()
            list.queueFree()
        }
    }

    // ── what the engine sends ─────────────────────────────────────────────────────────────────────────────────────

    private fun engineOriginated() = with(checks) {
        check("child_entered_tree and child_exiting_tree carry the child") {
            val parent = Node().inScene()
            val child = Node()
            var entered: Node? = null
            var exiting: Node? = null
            val a = parent.childEnteredTree.connect { node -> entered = node }
            val b = parent.childExitingTree.connect { node -> exiting = node }
            parent.addChild(node = child)
            expect(sameObject(entered, child)) { "child_entered_tree gave $entered" }
            parent.removeChild(child)
            expect(sameObject(exiting, child)) { "child_exiting_tree gave $exiting" }
            a.close()
            b.close()
            child.queueFree()
            parent.queueFree()
        }
        check("tree_entered, ready, tree_exiting and tree_exited happen in order") {
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
            expect(order == expected) { "order=$order, expected $expected" }
            connections.forEach { it.close() }
            node.queueFree()
            parent.queueFree()
        }
        check("renamed carries no arguments and fires inside the tree") {
            val node = Node().inScene()
            var renamed = 0
            val connection = node.renamed.connect { renamed++ }
            node.name = "Renamed".toStringName()
            expect(renamed == 1) { "renamed fired $renamed times" }
            connection.close()
            node.queueFree()
        }
        check("toggled carries the new state of the button") {
            val button = Button().inScene()
            button.toggleMode = true
            val states = mutableListOf<Boolean>()
            val connection = button.toggled.connect { toggledOn -> states += toggledOn }
            button.buttonPressed = true
            button.buttonPressed = false
            expect(states == listOf(true, false)) { "states=$states" }
            connection.close()
            button.queueFree()
        }
        check("value_changed carries the Double the slider got") {
            val slider = HSlider().inScene()
            // The default step is 1.0: the slider rounds what it is given to a multiple of it.
            slider.step = 0.5
            var received = Double.NaN
            val connection = slider.valueChanged.connect { value -> received = value }
            slider.value = 42.5
            expect(received == 42.5) { "received=$received" }
            connection.close()
            slider.queueFree()
        }
        check("text_change_rejected carries the rejected text as a Kotlin String") {
            val edit = LineEdit().inScene()
            edit.maxLength = 3
            var rejected: String? = null
            val connection = edit.textChangeRejected.connect { rejectedSubstring -> rejected = rejectedSubstring }
            edit.insertTextAtCaret("kogot")
            expect(rejected == "ot") { "rejected=$rejected" }
            connection.close()
            edit.queueFree()
        }
        check("property_list_changed (declared by Object) fires for any subclass") {
            val node = Node2D()
            var fired = 0
            val connection = node.propertyListChanged.connect { fired++ }
            node.notifyPropertyListChanged()
            expect(fired == 1) { "fired $fired times" }
            connection.close()
            node.queueFree()
        }
        check("hidden and visibility_changed fire when a Control is hidden") {
            val control = Control().inScene()
            var hidden = 0
            var changed = 0
            val a = control.hidden.connect { hidden++ }
            val b = control.visibilityChanged.connect { changed++ }
            control.visible = false
            expect(hidden == 1) { "hidden fired $hidden times" }
            expect(changed >= 1) { "visibility_changed fired $changed times" }
            a.close()
            b.close()
            control.queueFree()
        }
        check("changed (Resource) is a signal of a RefCounted") {
            val resource = Resource()
            var fired = 0
            val connection = resource.changed.connect { fired++ }
            resource.emitChanged()
            expect(fired == 1) { "fired $fired times" }
            connection.close()
        }
        check("setBlockSignals(true) stops the delivery") {
            val button = Button()
            button.toggleMode = true
            var fired = 0
            val connection = button.toggled.connect { fired++ }
            button.setBlockSignals(true)
            button.buttonPressed = true
            button.setBlockSignals(false)
            expect(fired == 0) { "delivered $fired while blocked" }
            connection.close()
            button.queueFree()
        }
    }

    // ── typed emit and connect, per argument type and arity ───────────────────────────────────────────────────────

    private fun emitAndReceive() = with(checks) {
        check("0 arguments: Resource.changed") {
            val resource = Resource()
            var fired = 0
            resource.changed.connect { fired++ }.use {
                expect(resource.changed.emit() == GodotError.OK) { "emit did not return OK" }
                expect(fired == 1) { "fired $fired times" }
            }
        }
        check("Boolean: BaseButton.toggled") {
            val button = Button()
            val received = mutableListOf<Boolean>()
            button.toggled.connect { toggledOn -> received += toggledOn }.use {
                button.toggled.emit(true).ok()
                button.toggled.emit(false).ok()
                expect(received == listOf(true, false)) { "received=$received" }
            }
            button.queueFree()
        }
        check("Double: Range.value_changed") {
            val slider = HSlider()
            var received = Double.NaN
            slider.valueChanged.connect { value -> received = value }.use {
                slider.valueChanged.emit(-3.25).ok()
                expect(received == -3.25) { "received=$received" }
            }
            slider.queueFree()
        }
        check("String: LineEdit.text_submitted") {
            val edit = LineEdit()
            var received: String? = null
            edit.textSubmitted.connect { newText -> received = newText }.use {
                edit.textSubmitted.emit("Ñandú 🎮").ok()
                expect(received == "Ñandú 🎮") { "received=$received" }
            }
            edit.queueFree()
        }
        check("Long: ItemList.item_selected (negative and large)") {
            val list = ItemList()
            val received = mutableListOf<Long>()
            list.itemSelected.connect { index -> received += index }.use {
                list.itemSelected.emit(7L).ok()
                list.itemSelected.emit(-1L).ok()
                list.itemSelected.emit(Long.MAX_VALUE).ok()
                expect(received == listOf(7L, -1L, Long.MAX_VALUE)) { "received=$received" }
            }
            list.queueFree()
        }
        check("Color: ColorPicker.color_changed") {
            val picker = ColorPicker()
            var received: Color? = null
            val sent = Color(0.25, 0.5, 0.75, 1.0)
            picker.colorChanged.connect { color -> received = color }.use {
                picker.colorChanged.emit(sent).ok()
                expect(received == sent) { "received=$received, sent $sent" }
            }
            picker.queueFree()
        }
        check("StringName: AnimationPlayer.current_animation_changed") {
            val player = AnimationPlayer()
            var received: String? = null
            player.currentAnimationChanged.connect { name -> received = name.toString() }.use {
                player.currentAnimationChanged.emit("run".toStringName()).ok()
                expect(received == "run") { "received=$received" }
            }
            player.queueFree()
        }
        check("Vector2: NavigationAgent2D.velocity_computed") {
            val agent = NavigationAgent2D()
            var received: Vector2? = null
            val sent = Vector2(3.0, -4.5)
            agent.velocityComputed.connect { safeVelocity -> received = safeVelocity }.use {
                agent.velocityComputed.emit(sent).ok()
                expect(received == sent) { "received=$received, sent $sent" }
            }
            agent.queueFree()
        }
        check("two arguments: AnimationPlayer.animation_changed (StringName, StringName)") {
            val player = AnimationPlayer()
            var received: Pair<String, String>? = null
            player.animationChanged.connect { oldName, newName -> received = oldName.toString() to newName.toString() }
                .use {
                    player.animationChanged.emit("idle".toStringName(), "walk".toStringName()).ok()
                    expect(received == "idle" to "walk") { "received=$received" }
                }
            player.queueFree()
        }
        check("two arguments of different types: ItemList.empty_clicked (Vector2, Long)") {
            val list = ItemList()
            var position: Vector2? = null
            var button = -1L
            list.emptyClicked.connect { atPosition, mouseButtonIndex ->
                position = atPosition
                button = mouseButtonIndex
            }.use {
                list.emptyClicked.emit(Vector2(10.0, 20.0), 2L).ok()
                expect(position == Vector2(10.0, 20.0) && button == 2L) { "position=$position button=$button" }
            }
            list.queueFree()
        }
        check("three arguments: ItemList.item_clicked (Long, Vector2, Long)") {
            val list = ItemList()
            var received: Triple<Long, Vector2, Long>? = null
            list.itemClicked.connect { index, atPosition, mouseButtonIndex ->
                received = Triple(index, atPosition, mouseButtonIndex)
            }.use {
                list.itemClicked.emit(4L, Vector2(1.5, 2.5), 1L).ok()
                expect(received == Triple(4L, Vector2(1.5, 2.5), 1L)) { "received=$received" }
            }
            list.queueFree()
        }
        check("four arguments with a RID and a Node2D: Area2D.body_shape_entered") {
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
                expect(sameObject(gotBody, body)) { "body=$gotBody" }
                expect(shape == 3L && local == 5L) { "shape=$shape local=$local" }
            }
            body.queueFree()
            area.queueFree()
        }
        check("an engine object argument: Node.child_entered_tree emitted by hand") {
            val parent = Node()
            val child = Node()
            var received: Node? = null
            parent.childEnteredTree.connect { node -> received = node }.use {
                parent.childEnteredTree.emit(child).ok()
                expect(sameObject(received, child)) { "received=$received" }
            }
            child.queueFree()
            parent.queueFree()
        }
        check("a subclass as argument: Area2D.area_entered receives the other Area2D") {
            val area = Area2D()
            val other = Area2D()
            var received: Area2D? = null
            area.areaEntered.connect { theArea -> received = theArea }.use {
                area.areaEntered.emit(other).ok()
                expect(sameObject(received, other)) { "received=$received" }
            }
            other.queueFree()
            area.queueFree()
        }
        check("an InputEvent argument: Control.gui_input") {
            val control = Control()
            val event = InputEventKey()
            var received: GodotObject? = null
            control.guiInput.connect { theEvent -> received = theEvent }.use {
                control.guiInput.emit(event).ok()
                expect(sameObject(received, event)) { "received=$received" }
            }
            control.queueFree()
        }
        check("a Dictionary argument: NavigationAgent2D.waypoint_reached") {
            val agent = NavigationAgent2D()
            var size = -1
            agent.waypointReached.connect { details -> size = details.size }.use {
                VariantDictionary().use { sent ->
                    agent.waypointReached.emit(sent).ok()
                    expect(size == 0) { "size=$size" }
                }
            }
            agent.queueFree()
        }
        check("an untyped Callable can be connected to a typed signal") {
            val button = Button()
            var received: Boolean? = null
            val callable = Callable { toggledOn: Boolean -> received = toggledOn }
            button.toggled.connect(callable).use {
                button.toggled.emit(true).ok()
                expect(received == true) { "received=$received" }
            }
            callable.close()
            button.queueFree()
        }
        check("GDScript-style emit_signal reaches the typed handler") {
            val button = Button()
            var received: Boolean? = null
            button.toggled.connect { toggledOn -> received = toggledOn }.use {
                button.emitSignal("toggled".toStringName(), true).ok()
                expect(received == true) { "received=$received" }
            }
            button.queueFree()
        }
    }

    private fun emitSignalOverloads() = with(checks) {
        check("emit_signal with Kotlin values: one argument per overload shape (@MustBeVariant Any?)") {
            val player = AnimationPlayer()
            var received: Pair<String, String>? = null
            player.animationChanged.connect { oldName, newName -> received = oldName.toString() to newName.toString() }
                .use {
                    player.emitSignal("animation_changed".toStringName(), "idle".toStringName(), "walk".toStringName())
                        .ok()
                    expect(received == "idle" to "walk") { "received=$received" }
                }
            player.queueFree()
        }
        check("emit_signal with values of different Kotlin types: ItemList.item_clicked (Long, Vector2, Long)") {
            val list = ItemList()
            var received: Triple<Long, Vector2, Long>? = null
            list.itemClicked.connect { index, position, button -> received = Triple(index, position, button) }.use {
                list.emitSignal("item_clicked".toStringName(), 4, Vector2(1.5, 2.5), 1L).ok()
                expect(received == Triple(4L, Vector2(1.5, 2.5), 1L)) { "received=$received" }
            }
            list.queueFree()
        }
        check("emit_signal with no variadic argument still resolves to the Variant overload") {
            val resource = Resource()
            var fired = 0
            resource.changed.connect { fired++ }.use {
                resource.emitSignal("changed".toStringName()).ok()
                expect(fired == 1) { "fired $fired times" }
            }
        }
    }

    // ── the life of a connection ──────────────────────────────────────────────────────────────────────────────────

    private fun connections() = with(checks) {
        check("hasConnections and isConnected follow connect and close") {
            val button = Button()
            expect(!button.toggled.hasConnections) { "connected before connect" }
            val connection = button.toggled.connect { _ -> }
            expect(button.toggled.hasConnections && connection.isConnected) { "not connected after connect" }
            connection.close()
            expect(!button.toggled.hasConnections && !connection.isConnected) { "still connected after close" }
            button.queueFree()
        }
        check("a closed connection receives nothing, and closing twice is harmless") {
            val button = Button()
            var fired = 0
            val connection = button.toggled.connect { _ -> fired++ }
            button.toggled.emit(true).ok()
            connection.close()
            connection.close()
            expect(button.toggled.emit(true) == GodotError.UNAVAILABLE) { "emit to nobody did not say UNAVAILABLE" }
            expect(fired == 1) { "fired $fired times" }
            button.queueFree()
        }
        check("several handlers are all called, in connection order") {
            val button = Button()
            val order = mutableListOf<Int>()
            val a = button.toggled.connect { _ -> order += 1 }
            val b = button.toggled.connect { _ -> order += 2 }
            val c = button.toggled.connect { _ -> order += 3 }
            button.toggled.emit(true).ok()
            expect(order == listOf(1, 2, 3)) { "order=$order" }
            listOf(a, b, c).forEach { it.close() }
            button.queueFree()
        }
        check("disconnecting one handler leaves the others") {
            val button = Button()
            val order = mutableListOf<Int>()
            val a = button.toggled.connect { _ -> order += 1 }
            val b = button.toggled.connect { _ -> order += 2 }
            a.close()
            button.toggled.emit(true).ok()
            expect(order == listOf(2)) { "order=$order" }
            b.close()
            button.queueFree()
        }
        check("ONE_SHOT delivers once and the connection is gone") {
            val button = Button()
            var fired = 0
            val connection = button.toggled.connect(GodotObject.ConnectFlags.ONE_SHOT) { _ -> fired++ }
            button.toggled.emit(true).ok()
            expect(!connection.isConnected) { "ONE_SHOT still connected after firing" }
            expect(button.toggled.emit(true) == GodotError.UNAVAILABLE) { "the second emit found a listener" }
            expect(fired == 1) { "fired $fired times" }
            connection.close()
            button.queueFree()
        }
        check("APPEND_SOURCE_OBJECT is rejected: the handler does not declare that argument") {
            val button = Button()
            expectThrows<IllegalArgumentException>("connect with APPEND_SOURCE_OBJECT") {
                button.toggled.connect(GodotObject.ConnectFlags.APPEND_SOURCE_OBJECT) { _ -> }
            }
            expect(!button.toggled.hasConnections) { "a rejected connect left a connection" }
            button.queueFree()
        }
        check("asSignal gives an independent builtin Signal the caller closes") {
            val button = Button()
            val connection = button.toggled.connect { _ -> }
            button.toggled.asSignal().use { signal ->
                expect(signal.hasConnections()) { "the copy sees no connection" }
            }
            expect(connection.isConnected) { "closing the copy disconnected the handler" }
            connection.close()
            button.queueFree()
        }
        check("a closed TypedSignal refuses to be used, the connections it made stay") {
            val button = Button()
            val signal = button.toggled
            var fired = 0
            val connection = signal.connect { _ -> fired++ }
            signal.close()
            expectThrows<IllegalStateException>("emit on a closed signal") { signal.emit(true) }
            expect(connection.isConnected) { "close() disconnected the handler" }
            button.emitSignal("toggled".toStringName(), true).ok()
            expect(fired == 1) { "fired $fired times" }
            connection.close()
            button.queueFree()
        }
        check("emit with nobody connected returns UNAVAILABLE, which is how Godot answers") {
            val button = Button()
            val result = button.toggled.emit(true)
            expect(result == GodotError.UNAVAILABLE) { "result=$result" }
            button.queueFree()
        }
    }

    // ── asynchronous ──────────────────────────────────────────────────────────────────────────────────────────────

    private fun asynchronous() {
        val button = Button().inScene()
        val timer = Timer().inScene()
        timer.oneShot = true
        timer.waitTime = 0.05
        liveConnections += timer.timeout.connect { timerFired = true }

        // DEFERRED: the handler waits for the end of the frame, so it has not run when emit returns.
        liveConnections += button.toggled.connect(GodotObject.ConnectFlags.DEFERRED) { toggledOn ->
            deferredValue = if (toggledOn) 1L else 0L
        }
        button.toggled.emit(true).ok()
        deferredDuringEmit = deferredValue != null
        timer.start()

        // processFrame: a SceneTree signal, counted until everything has happened or the time is up.
        liveConnections += getTree().processFrame.connect {
            frames++
            val done = timerFired && deferredValue != null && frames >= 3
            if (done || frames >= MAX_FRAMES) finishAsynchronous()
        }
    }

    private fun finishAsynchronous() {
        // The handler runs every frame: stop it from finishing twice.
        if (finished) return
        finished = true
        with(checks) {
            check("DEFERRED waits for the end of the frame") {
                expect(!deferredDuringEmit) { "the handler ran inside emit" }
                expect(deferredValue == 1L) { "deferredValue=$deferredValue after $frames frames" }
            }
            check("Timer.timeout arrives after the wait time") {
                expect(timerFired) { "timeout never fired in $frames frames" }
            }
            check("SceneTree.process_frame is delivered every frame") {
                expect(frames >= 3) { "only $frames frames" }
            }
        }
        getTree().quit(checks.finish())
    }

    private companion object {
        const val MAX_FRAMES = 600
    }
}
