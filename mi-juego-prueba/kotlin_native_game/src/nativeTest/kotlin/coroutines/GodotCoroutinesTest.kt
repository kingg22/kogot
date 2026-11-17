@file:OptIn(
    ExperimentalForeignApi::class,
    ExperimentalGodotKotlin::class,
    ExperimentalGodotApi::class,
    InternalBinding::class,
)

package io.github.kingg22.godot.test.coroutines

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotNotification
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.signal.emit
import io.github.kingg22.godot.api.signal.signal
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.MainThread
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import platform.posix.usleep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** A node with the two signals the `await` adapters are tested against. */
@Godot
class CoroutineProbe(nativePtr: COpaquePointer) :
    Node(nativePtr),
    GodotNotification {
    @RegisterSignal
    val progress by signal<(value: Long) -> Unit>()

    @RegisterSignal
    val finished by signal<() -> Unit>()

    override fun _notification(what: Int) {
        if (what == NOTIFICATION_PREDELETE.toInt()) {
            progress.close()
            finished.close()
        }
    }
}

/**
 * Tests of the coroutine prototype of `GodotCoroutines.kt`: a main-thread dispatcher and `suspend` adapters over
 * signals. Ported from the former `CoroutinesCheck` scene.
 *
 * The test runner is synchronous on the main thread and the main loop does not run while a test executes, so the
 * dispatcher is not driven by the GDExtension `frame_func`: each test pumps it by hand with [pump], which is what the
 * frame does.
 *
 * Not ported (they need real frames or the end of the process): `frame_func` and `shutdown_func` registration,
 * `SceneTree.nextFrame()` / `SceneTree.wait()`, awaiting `Timer.timeout`, and the cancellation of a coroutine still
 * suspended at exit.
 */
class GodotCoroutinesTest {
    /** Stands in for the frame: drains [GodotMain] until [job] completes, then once more for what it queued last. */
    private fun pump(job: Job) {
        val deadline = TimeSource.Monotonic.markNow() + 10.seconds
        while (!job.isCompleted) {
            val _ = GodotMain.drain()
            assertTrue(deadline.hasNotPassedNow(), "the coroutine did not complete: $job")
            usleep(200u)
        }
        val _ = GodotMain.drain()
    }

    @Test
    fun aGodotScopeCoroutineRunsOnTheMainThread() {
        var onMain = false
        pump(GodotScope.launch { onMain = MainThread.isCurrent })
        assertTrue(onMain, "not on the main thread")
    }

    @Test
    fun theDispatcherOnlyRunsWhenItIsDrained() {
        var ran = false
        val job = GodotScope.launch { ran = true }
        assertFalse(ran, "ran before the queue was drained")
        pump(job)
        assertTrue(ran, "never ran")
    }

    @Test
    fun workQueuedWhileDrainingWaitsForTheNextDrain() {
        var inner = false
        var innerJob: Job? = null
        val outer = GodotScope.launch { innerJob = GodotScope.launch { inner = true } }
        val _ = GodotMain.drain()
        assertTrue(outer.isCompleted, "the first task did not run")
        assertFalse(inner, "work queued during the drain ran in the same drain")
        pump(innerJob ?: error("the first task did not launch the second"))
        assertTrue(inner, "the second task never ran")
    }

    @Test
    fun defaultDispatcherRunsOffTheMainThreadAndComesBackToIt() {
        var computedOff = false
        var backOnMain = false
        var sum = 0L
        pump(
            GodotScope.launch {
                val (off, total) = withContext(Dispatchers.Default) { !MainThread.isCurrent to (1..1_000_000L).sum() }
                computedOff = off
                sum = total
                backOnMain = MainThread.isCurrent
            },
        )
        assertTrue(computedOff, "Default ran on the main thread")
        assertTrue(backOnMain, "did not come back to the main thread")
        assertEquals(500_000_500_000L, sum, "sum=$sum")
    }

    @Test
    fun immediateDispatcherRunsInlineOnTheMainThread() {
        var ran = false
        val job = GodotScope.launch(GodotMain.immediate) { ran = true }
        assertTrue(ran, "it waited for a drain")
        pump(job)
    }

    @Test
    fun thousandCoroutinesLaunchedOnGodotMainRunInFifoOrder() {
        val order = mutableListOf<Int>()
        val jobs = (0 until 1_000).map { i -> GodotScope.launch { order += i } }
        pump(jobs.last())
        assertEquals((0 until 1_000).toList(), order, "order starts ${order.take(10)}")
    }

    /** 8 background coroutines hop to the main thread 1000 times each; the confinement serializes the increments. */
    @Test
    fun hopsToGodotMainFromManyThreadsLoseNoUpdate() {
        var counter = 0
        var backOnMain = false
        pump(
            GodotScope.launch {
                (0 until 8).map {
                    GodotScope.async(Dispatchers.Default) {
                        repeat(1_000) { withContext(GodotMain) { counter++ } }
                    }
                }.awaitAll()
                backOnMain = MainThread.isCurrent
            },
        )
        assertEquals(8_000, counter, "counter=$counter")
        assertTrue(backOnMain, "not on the main thread")
    }

    @Test
    fun delayResumesOnTheMainThread() {
        var onMain = false
        pump(
            GodotScope.launch {
                delay(30)
                onMain = MainThread.isCurrent
            },
        )
        assertTrue(onMain, "off the main thread")
    }

    @Test
    fun anExceptionInOneCoroutineIsReportedAndDoesNotCancelTheScope() {
        val failing = GodotScope.launch { error("boom") }
        pump(failing)
        pump(GodotScope.launch { yield() })
        assertTrue(GodotScope.errors.any { it.message == "boom" }, "errors=${GodotScope.errors}")
        assertTrue(GodotScope.coroutineContext[Job]?.isActive == true, "the scope was cancelled")
    }

    @Test
    fun awaitingASignalWithAnArgumentReturnsTheEmittedValue() {
        val probe = instantiate<CoroutineProbe>()
        var received = 0L
        val waiting = GodotScope.launch(start = CoroutineStart.UNDISPATCHED) { received = probe.progress.await() }
        assertTrue(probe.progress.hasConnections, "a pending await holds no connection")
        val _ = probe.progress.emit(42L)
        pump(waiting)
        assertEquals(42L, received, "received=$received")
        assertFalse(probe.progress.hasConnections, "the one-shot connection was not released")
        ObjectBinding.destroyRaw(probe.rawPtr)
    }

    @Test
    fun awaitingASignalWithoutArgumentsResumesWhenItIsEmitted() {
        val probe = instantiate<CoroutineProbe>()
        var resumed = false
        val waiting = GodotScope.launch(start = CoroutineStart.UNDISPATCHED) {
            probe.finished.await()
            resumed = true
        }
        assertFalse(resumed, "resumed before the signal")
        val _ = probe.finished.emit()
        pump(waiting)
        assertTrue(resumed, "never resumed")
        ObjectBinding.destroyRaw(probe.rawPtr)
    }

    @Test
    fun cancellingACoroutineSuspendedOnASignalDisconnectsIt() {
        val probe = instantiate<CoroutineProbe>()
        val waiting = GodotScope.launch(start = CoroutineStart.UNDISPATCHED) { probe.finished.await() }
        assertTrue(probe.finished.hasConnections, "a pending await holds no connection")
        waiting.cancel()
        pump(waiting)
        assertTrue(waiting.isCancelled, "not cancelled")
        assertFalse(probe.finished.hasConnections, "still connected after cancel")
        ObjectBinding.destroyRaw(probe.rawPtr)
    }
}
