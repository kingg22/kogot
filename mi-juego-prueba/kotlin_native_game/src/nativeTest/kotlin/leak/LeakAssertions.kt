@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.leak

import cHeapInUse
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.singleton.Os
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.usleep
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlin.time.measureTime

private const val WARM_UP_CALLS = 100
private const val MEASURED_CALLS = 20_000
private const val GROWTH_LIMIT_BYTES = 100_000L

/**
 * Runs [call] 20,000 times and fails when it grows the C heap ([cHeap] `false` skips it) or the engine memory by more
 * than a small margin. With [collect], the garbage collector runs and the Cleaners settle before each measurement:
 * memory a Cleaner frees is only given back once the wrapper is collected.
 *
 * [cHeapInUse] is `null` where the platform has no cheap query for it (everywhere but Linux): the C heap is then not
 * measured and only the engine memory is asserted.
 */
fun assertNoGrowth(
    name: String,
    tag: String,
    cHeap: Boolean = true,
    collect: Boolean = false,
    call: () -> Unit,
) {
    val os = Os.instance
    repeat(WARM_UP_CALLS) { call() } // warm up: arenas, lazy method binds
    if (collect) settleCleaners()
    val heapBefore = cHeapInUse()
    val engineBefore = os.getStaticMemoryUsage().toLong()
    repeat(MEASURED_CALLS) { call() }
    if (collect) settleCleaners()
    val heapAfter = cHeapInUse()
    val heap = if (heapBefore != null && heapAfter != null) heapAfter - heapBefore else null
    val engine = os.getStaticMemoryUsage().toLong() - engineBefore
    val heapText = heap?.let { "+$it bytes" } ?: "not measured on this platform"
    println("[$tag] $name: C heap $heapText, engine +$engine bytes over $MEASURED_CALLS calls")
    assertTrue(
        (!cHeap || heap == null || heap < GROWTH_LIMIT_BYTES) && engine < GROWTH_LIMIT_BYTES,
        "$name leaks: C heap grew by $heap bytes, engine by $engine bytes",
    )
}

/** Prints the mean time of [call] over 200,000 runs, after a warm-up, including the collections they cause. */
fun printTiming(name: String, tag: String, call: () -> Unit) {
    repeat(10_000) { call() }
    settleCleaners()
    val runs = 200_000
    val elapsed = TimeSource.Monotonic.measureTime {
        repeat(runs) { call() }
        settleCleaners()
    }
    println("[$tag] timing $name: ${elapsed.inWholeNanoseconds / runs} ns/op over $runs (with GC settle)")
}

/**
 * Collects garbage until the Cleaners it schedules have run: they run on Kotlin/Native's own cleaner thread, a little
 * after the collection, so this collects and waits a few times.
 */
@OptIn(NativeRuntimeApi::class)
fun settleCleaners() {
    repeat(5) {
        GC.collect()
        usleep(20_000u)
    }
}
