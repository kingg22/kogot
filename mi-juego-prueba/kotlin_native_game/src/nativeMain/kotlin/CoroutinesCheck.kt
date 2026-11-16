@file:OptIn(
    ExperimentalForeignApi::class,
    ExperimentalGodotKotlin::class,
    ExperimentalGodotApi::class,
    InternalBinding::class,
)

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotNotification
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.node.Timer
import io.github.kingg22.godot.api.signal.emit
import io.github.kingg22.godot.api.signal.signal
import io.github.kingg22.godot.api.singleton.Engine
import io.github.kingg22.godot.api.singleton.Time
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.MainThread
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Live check of the coroutine prototype of `GodotCoroutines.kt` (`coroutines_check.tscn`): a main-thread dispatcher
 * driven by the GDExtension main-loop `frame_func`, and `suspend` adapters over Godot signals and timers.
 *
 * The whole check is itself a coroutine on [GodotScope]: it starts from `_ready` and quits the tree at the end, so the
 * scene only finishes if the dispatcher really runs. One coroutine is left suspended on a signal that never fires, to
 * check that the main loop's `shutdown_func` cancels it and releases its connection before the engine goes away.
 */
@Godot
class CoroutinesCheck(nativePtr: COpaquePointer) :
    Node(nativePtr),
    GodotNotification {
    private val checks = Checks("CoroutinesCheck")

    @RegisterSignal
    val progress by signal<(value: Long) -> Unit>()

    @RegisterSignal
    val never by signal<() -> Unit>()

    private var processFrames = 0

    override fun _notification(what: Int) {
        if (what == NOTIFICATION_PREDELETE.toInt()) {
            progress.close()
            never.close()
        }
    }

    override fun _ready() {
        GodotMainLoop.register()
        val _ = GodotScope.launch { runChecks() }
        // Left suspended until shutdown: shutdown_func must cancel it and release the connection (see the log).
        val _ = GodotScope.launch {
            try {
                never.await()
            } finally {
                println("[CoroutinesCheck] the coroutine suspended at exit was cancelled on shutdown")
            }
        }
    }

    override fun _process(delta: Double) {
        processFrames++
        // Safety net: if frame_func never runs, the check coroutine never starts. Report it and stop.
        if (processFrames == 30 && GodotMain.frames == 0) {
            println("[CoroutinesCheck] FAIL frame_func never ran: the dispatcher is not driven")
            getTree().quit(1)
        }
    }

    private suspend fun runChecks() = with(checks) {
        val tree = getTree()

        check("frame_func registered after startup still runs every frame") {
            expect(GodotMain.frames > 0) { "frames=${GodotMain.frames}" }
        }
        check("kotlinx Dispatchers.Main is not available on Linux native (needs our own Main)") {
            val outcome = runCatching { Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext) }
            println(
                "[CoroutinesCheck] Dispatchers.Main -> ${outcome.exceptionOrNull() ?: "available: ${Dispatchers.Main}"}",
            )
            expect(outcome.isFailure) { "Dispatchers.Main = ${Dispatchers.Main}" }
        }
        check("a GodotScope coroutine runs on the main thread") {
            expect(MainThread.isCurrent) { "not on the main thread" }
        }

        // Background work, then back to the main thread to touch the scene.
        val node = Node2D()
        addChild(node)
        val (computedOff, sum) = withContext(Dispatchers.Default) { !MainThread.isCurrent to (1..1_000_000L).sum() }
        check("withContext(Dispatchers.Default) runs off the main thread and comes back to it") {
            expect(computedOff) { "Default ran on the main thread" }
            expect(MainThread.isCurrent) { "did not come back to the main thread" }
            expect(sum == 500_000_500_000L) { "sum=$sum" }
            node.position = Vector2(sum.toDouble() / 1e12, 2.0) // a node in the tree: main thread only
            expect(node.position.x > 0.0) { "position=${node.position}" }
        }

        val frameBefore = Engine.instance.getProcessFrames()
        tree.nextFrame()
        val frameAfter = Engine.instance.getProcessFrames()
        check("SceneTree.nextFrame() resumes on the next frame") {
            expect(frameAfter == frameBefore + 1uL) { "frames $frameBefore -> $frameAfter" }
            expect(MainThread.isCurrent) { "not on the main thread" }
        }

        val sameFrame = Engine.instance.getProcessFrames()
        val ranInSameFrame = withContext(GodotMain.immediate) { Engine.instance.getProcessFrames() == sameFrame }
        check("GodotMain.immediate runs inline on the main thread (no frame wait)") {
            expect(ranInSameFrame) { "waited a frame" }
        }

        val start = Time.instance.getTicksMsec()
        tree.wait(0.1)
        val waited = Time.instance.getTicksMsec() - start
        check("SceneTree.wait(0.1) (create_timer + await timeout) suspends about 100 ms") {
            expect(waited >= 95uL && waited < 1_000uL) { "waited=$waited ms" }
            expect(MainThread.isCurrent) { "not on the main thread" }
        }

        val timer = Timer().apply {
            waitTime = 0.05
            oneShot = true
        }
        addChild(timer)
        timer.start()
        timer.timeout.await()
        check("awaiting an engine signal (Timer.timeout) resumes when it fires") {
            expect(timer.isStopped()) { "timer still running" }
        }

        val emitter = GodotScope.launch {
            tree.nextFrame()
            val _ = progress.emit(42L)
        }
        val received = progress.await()
        emitter.join()
        check("awaiting a signal with an argument returns the emitted value") {
            expect(received == 42L) { "received=$received" }
        }

        val waiting = GodotScope.launch(start = CoroutineStart.UNDISPATCHED) { timer.timeout.await() }
        check("a pending await holds a connection") { expect(timer.timeout.hasConnections) { "not connected" } }
        waiting.cancel()
        tree.nextFrame()
        check("cancelling a coroutine suspended on a signal disconnects it") {
            expect(waiting.isCancelled) { "not cancelled" }
            expect(!timer.timeout.hasConnections) { "still connected after cancel" }
        }

        val order = mutableListOf<Int>()
        (0 until 1_000).map { i -> GodotScope.launch { order += i } }.forEach { it.join() }
        check("1000 coroutines launched on GodotMain run in FIFO order") {
            expect(order == (0 until 1_000).toList()) { "order starts ${order.take(10)}" }
        }

        // Contention: 8 background coroutines hop to the main thread 1000 times each; main-thread confinement
        // serializes the increments without any lock.
        var counter = 0
        (0 until 8).map {
            GodotScope.async(Dispatchers.Default) {
                repeat(1_000) { withContext(GodotMain) { counter++ } }
            }
        }.awaitAll()
        check("8 threads x 1000 hops to GodotMain: no lost update, no lock") {
            expect(counter == 8_000) { "counter=$counter" }
            expect(MainThread.isCurrent) { "not on the main thread" }
        }

        delay(30)
        check("delay() on GodotMain resumes on the main thread") { expect(MainThread.isCurrent) { "off main" } }

        val failing = GodotScope.launch { error("boom") }
        failing.join()
        yield()
        check("an exception in one coroutine is reported and does not cancel the scope") {
            expect(GodotScope.errors.any { it.message == "boom" }) { "errors=${GodotScope.errors}" }
            expect(GodotScope.coroutineContext[kotlinx.coroutines.Job]!!.isActive) { "scope cancelled" }
        }

        node.queueFree()
        timer.queueFree()
        tree.quit(finish())
    }
}
