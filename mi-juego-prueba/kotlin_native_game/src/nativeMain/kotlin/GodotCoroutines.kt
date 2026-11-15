@file:OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class, InternalBinding::class)

import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.SceneTree
import io.github.kingg22.godot.api.signal.SignalConnection
import io.github.kingg22.godot.api.signal.TypedSignal
import io.github.kingg22.godot.api.signal.connect
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.MainThread
import io.github.kingg22.godot.internal.binding.RegisterBinding
import io.github.kingg22.godot.internal.binding.guardCallback
import io.github.kingg22.godot.internal.ffi.GDExtensionMainLoopCallbacks
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/*
 * PROTOTYPE (sample project only, not binding API yet): a coroutine dispatcher for Godot's main thread and `suspend`
 * adapters over signals, checked live by coroutines_check.tscn. See docs/coroutines.md.
 *
 * Same model as the Go binding (graphics.gd, internal/ring): any thread may *enqueue* work for the main thread, through
 * a lock-free multi-producer queue, and the main thread drains it once per frame. The drain runs from the
 * GDExtension main-loop `frame_func` (Godot 4.5+), after every `_process`, so no node and no `call_deferred` is needed.
 */

/**
 * Dispatcher of Godot's main thread. `dispatch` is safe from any thread (a lock-free push); the work runs on the main
 * thread at the next frame, in FIFO order. Use [immediate] to run inline when already on the main thread.
 */
object GodotMain : MainCoroutineDispatcher() {
    private class Task(val block: Runnable, val next: Task?)

    /** Treiber stack of pending tasks, newest first; [drain] reverses it to run them in FIFO order. */
    private val pending = AtomicReference<Task?>(null)

    private val drained = AtomicInt(0)

    /** Number of frames that drained the queue (for the check: proves `frame_func` runs). */
    val frames: Int get() = drained.load()

    override val immediate: MainCoroutineDispatcher get() = Immediate

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        while (true) {
            val head = pending.load()
            if (pending.compareAndSet(head, Task(block, head))) return
        }
    }

    /** Runs everything queued so far, oldest first. Main thread only. Work queued while draining waits for next frame. */
    fun drain(): Int {
        val _ = drained.fetchAndAdd(1)
        var reversed: Task? = null
        var task = pending.exchange(null)
        while (task != null) {
            reversed = Task(task.block, reversed)
            task = task.next
        }
        var count = 0
        while (reversed != null) {
            val task = reversed
            // A throwing task must not stop the rest: report it as Godot error and continue.
            guardCallback("GodotMain.task") { task.block.run() }
            count++
            reversed = task.next
        }
        return count
    }

    override fun toString(): String = "Godot.Main"

    /** Runs inline when already on the main thread, otherwise like [GodotMain]. */
    private object Immediate : MainCoroutineDispatcher() {
        override val immediate: MainCoroutineDispatcher get() = this

        override fun isDispatchNeeded(context: CoroutineContext): Boolean = !MainThread.isCurrent

        override fun dispatch(context: CoroutineContext, block: Runnable) = GodotMain.dispatch(context, block)

        override fun toString(): String = "Godot.Main.immediate"
    }
}

/**
 * Scope for game coroutines: [GodotMain], a [SupervisorJob] (one failing coroutine does not cancel the others) and a
 * handler that reports uncaught exceptions instead of crashing. Cancelled by the main loop's `shutdown_func`, while the
 * engine is still fully alive, so suspended coroutines can release what they hold (signal connections).
 */
object GodotScope : CoroutineScope {
    val errors = mutableListOf<Throwable>()

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + GodotMain + CoroutineExceptionHandler { _, error -> errors += error }
}

/**
 * Registers the main-loop callbacks that drive [GodotMain]. The real binding would do it during initialization; here it
 * is called from a node's `_ready`, after the main loop started (so `startup_func` is missed, which is fine).
 */
object GodotMainLoop {
    var registered = false
        private set
    var shutdownCalled = false
        private set

    fun register() {
        if (registered) return
        registered = true
        // Lives as long as the library: Godot keeps the pointer.
        val callbacks = nativeHeap.alloc<GDExtensionMainLoopCallbacks> {
            startup_func = null
            frame_func = staticCFunction<Unit> { guardCallback("GodotMain.frame") { val _ = GodotMain.drain() } }
            shutdown_func = staticCFunction<Unit> {
                guardCallback("GodotMain.shutdown") {
                    shutdownCalled = true
                    GodotScope.cancel("Godot is shutting down")
                    // Run the cancellation handlers (they release signal connections) while the engine is alive.
                    val _ = GodotMain.drain()
                }
            }
        }
        RegisterBinding.mainLoopCallbacksRaw(BindingProcAddressHolder.library, callbacks.ptr)
    }
}

// ── suspend adapters ─────────────────────────────────────────────────────────────────────────────────────────────────

/**
 * Suspends until [this] signal is emitted once. The connection is `ONE_SHOT` and is always released: after the
 * emission, or on the main thread when the coroutine is cancelled first.
 */
suspend fun TypedSignal<() -> Unit>.await() {
    suspendCancellableCoroutine { continuation ->
        val holder = arrayOfNulls<SignalConnection>(1)
        holder[0] = connect(GodotObject.ConnectFlags.ONE_SHOT) {
            holder[0]?.close()
            continuation.resumeIfActive(Unit)
        }
        continuation.releaseOnCancel(holder)
    }
}

/** Suspends until [this] one-argument signal is emitted once, and returns the argument. */
suspend inline fun <reified @MustBeVariant P1> TypedSignal<(P1) -> Unit>.await(): P1 = suspendCancellableCoroutine { continuation ->
    val holder = arrayOfNulls<SignalConnection>(1)
    holder[0] = connect(GodotObject.ConnectFlags.ONE_SHOT) { value: P1 ->
        holder[0]?.close()
        continuation.resumeIfActive(value)
    }
    continuation.releaseOnCancel(holder)
}

fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) resume(value)
}

/** On cancellation, closes the connection on the main thread (cancellation may happen on any thread). */
fun CancellableContinuation<*>.releaseOnCancel(holder: Array<SignalConnection?>) {
    invokeOnCancellation {
        GodotMain.dispatch(context, Runnable { holder[0]?.close() })
    }
}

/** Suspends until the next process frame. */
suspend fun SceneTree.nextFrame(): Unit = processFrame.await()

/** Suspends for [seconds] of game time, like `await get_tree().create_timer(seconds).timeout` in GDScript. */
suspend fun SceneTree.wait(seconds: Double) {
    createTimer(seconds).timeout.await()
}
