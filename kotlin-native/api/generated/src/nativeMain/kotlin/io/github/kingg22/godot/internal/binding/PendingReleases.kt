@file:OptIn(ExperimentalAtomicApi::class)

package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr
import kotlinx.cinterop.toLong
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Ledger of the engine references Kotlin has taken and not yet handed back, per native pointer (issue #114).
 *
 * Godot has no GC and tears itself down on its own schedule, so references that are still owed when the
 * extension shuts down would never be released: a wrapper that is still reachable at exit never fires its
 * [kotlin.native.ref.Cleaner], and one whose cleaner already queued a deferred release may only see it run
 * after the servers it needs are gone. Tracking what is owed lets [drain] settle all of it synchronously,
 * on the main thread, while the engine is still fully alive.
 *
 * One entry unit ("owe") is added for every reference whose release is still pending — the one each live
 * wrapper owns (see [attachRefCountedRelease]) and every redundant one queued by [releaseOwnedReference] —
 * and removed exactly once, by whichever comes first: the deferred release running on the main thread
 * ([settle]) or the shutdown [drain]. That "exactly once" is what makes a deferred release that is still
 * queued when [drain] runs a harmless no-op instead of a double `unreference()`.
 *
 * Touched from the main thread, from Kotlin/Native's cleaner thread and from any thread that calls
 * `GD.load`, so every access goes through a tiny spin lock (a handful of hash-map operations, never held
 * across an engine call).
 */
internal object PendingReleases {
    private class Entry(val ptr: GDExtensionObjectPtr, var count: Int)

    private val lock = AtomicInt(0)
    private val owed = HashMap<Long, Entry>()

    /** 0 while the extension is running; 1 once [drain] started, after which nothing is tracked or queued. */
    private val shutdown = AtomicInt(0)

    /** Cleaner-thread enqueues currently between [beginEnqueue] and [endEnqueue]; [drain] waits for them. */
    private val inFlight = AtomicInt(0)

    private inline fun <R> locked(block: () -> R): R {
        while (!lock.compareAndSet(0, 1)) {
            // Spin: the critical sections below are a few map operations.
        }
        try {
            return block()
        } finally {
            lock.store(0)
        }
    }

    /** Records one owed release for [ptr]. Returns `false` (and records nothing) once shutdown started. */
    fun owe(ptr: GDExtensionObjectPtr): Boolean {
        if (shutdown.load() != 0) return false
        return locked {
            if (shutdown.load() != 0) return@locked false
            val key = ptr.toLong()
            val entry = owed[key]
            if (entry == null) owed[key] = Entry(ptr, 1) else entry.count += 1
            true
        }
    }

    /**
     * Consumes one owed release for [ptr] on behalf of a deferred release that is about to run. Returns
     * `false` when there is nothing left to consume — [drain] already settled it — so the caller must skip
     * the `unreference()`.
     */
    fun settle(ptr: GDExtensionObjectPtr): Boolean = locked {
        val key = ptr.toLong()
        val entry = owed[key] ?: return@locked false
        entry.count -= 1
        if (entry.count <= 0) owed.remove(key)
        true
    }

    /**
     * Brackets a push onto Godot's message queue from a non-main thread. Returns `false` once shutdown
     * started, and the caller must not touch the engine at all. Every `true` must be paired with exactly
     * one [endEnqueue].
     */
    fun beginEnqueue(): Boolean {
        val _ = inFlight.fetchAndAdd(1)
        if (shutdown.load() == 0) return true
        val _ = inFlight.fetchAndAdd(-1)
        return false
    }

    fun endEnqueue() {
        val _ = inFlight.fetchAndAdd(-1)
    }

    /**
     * Stops all tracking and returns every release still owed, as `(pointer, how many)` pairs, for the
     * caller to perform synchronously. Waits for cleaner-thread enqueues that were already past
     * [beginEnqueue] so none of them can touch the engine after this returns.
     */
    fun drain(): List<Pair<GDExtensionObjectPtr, Int>> {
        shutdown.store(1)
        while (inFlight.load() != 0) {
            // Spin: an enqueue is a single callDeferred().
        }
        return locked {
            val pending = owed.values.map { it.ptr to it.count }
            owed.clear()
            pending
        }
    }
}
