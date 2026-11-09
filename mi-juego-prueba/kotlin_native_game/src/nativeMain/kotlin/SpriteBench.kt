@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.annotations.Export
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.annotations.GodotNotification
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.signal.SignalConnection
import io.github.kingg22.godot.api.signal.connect
import io.github.kingg22.godot.api.signal.emit
import io.github.kingg22.godot.api.signal.signal
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

// Parameter names and types survive the alias: KSP registers `moved(position: Vector2, source: Object)`.
// TODO: a Node parameter needs the pending GodotObject downcast; handlers receive a plain GodotObject today.
typealias Moved = (position: Vector2, source: GodotObject?) -> Unit

@Godot class SpriteBench(nativePtr: COpaquePointer) :
    Node2D(nativePtr),
    GodotNotification {

    @Export var health: Int = 100

    @Export var speed: Float = 1.0f

    @Export val spriteName: String = "SpriteBench"

    @RegisterSignal
    private val hint by signal()

    @RegisterSignal
    private val punch by signal<(value: Long) -> Unit>()

    @RegisterSignal
    private val moved by signal<Moved>()

    private val connections = mutableListOf<SignalConnection>()

    /**
     * Godot has no GC: release the native handles this node holds for its whole lifetime when it is about to be
     * freed, or they leak (`Orphan StringName: hint/punch …` at exit).
     */
    override fun _notification(what: Int) {
        if (what == GodotObject.NOTIFICATION_PREDELETE.toInt()) {
            println("[SpriteBench] NOTIFICATION_PREDELETE: releasing native handles")
            connections.forEach(SignalConnection::close)
            hint.close()
            punch.close()
            moved.close()
        }
    }

    override fun _ready() {
        println("[SpriteBench] _ready started")
        try {
            connections += hint.connect { println("hint: received") }
            connections += punch.connect { value -> println("punch: received $value") }
            connections += punch.connect(GodotObject.ConnectFlags.ONE_SHOT) { value -> println("punch one-shot: $value") }

            println("emit hint: ${hint.emit()}")
            println("emit punch 12: ${punch.emit(12L)}") // both handlers
            println("emit punch 15: ${punch.emit(15L)}") // the one-shot is gone

            val temporary = punch.connect { value -> println("punch temporary: $value") }
            println("temporary connected: ${temporary.isConnected}")
            temporary.close()
            println("temporary connected after close: ${temporary.isConnected}")
            println("emit punch 30: ${punch.emit(30L)}") // the temporary handler must not run

            connections += moved.connect { position, source -> println("moved: $position by ${source != null}") }
            println("emit moved: ${moved.emit(Vector2(1.0, 2.0), this)}")

            // Untyped Callable (e.g. a GDScript method): the connection keeps its own copy.
            val raw = Callable { value: Long -> println("punch raw callable: $value") }
            connections += punch.connect(raw)
            raw.close()
            println("emit punch 40: ${punch.emit(40L)}") // raw callable still runs
            println("punch has connections: ${punch.hasConnections}")
            punch.asSignal().use { builtin ->
                // The array (and the Signal inside each connection) is ours too: close it, or it leaks the name.
                builtin.getConnections().use { println("punch connections: ${it.size()}") }
            }
        } catch (e: Exception) {
            println("[SpriteBench] failed: ${e.message}")
            e.printStackTrace()
        } finally {
            println("[SpriteBench] _ready finished")
        }
    }
}
