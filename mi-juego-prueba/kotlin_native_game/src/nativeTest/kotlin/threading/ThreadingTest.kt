@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.threading

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.core.refcounted.Resource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.json.Json as KxJson

/**
 * The threading edge cases of a Kotlin/Native + Godot game that do not need the engine off the main thread. Ported
 * from `ThreadingEdgeCases` (`threading_edge_cases.tscn`).
 *
 * Not ported: the scenarios that were probes, with no verdict beyond the log. They call the engine from worker threads
 * on purpose, to see what happens (`Texture2D.getSize()` and `GD.load` of `res://icon.svg`, with and without a Mutex,
 * and Godot's own `Json.parseString`), and the test host project has no assets.
 */
class ThreadingTest {
    /** Baseline: `kotlinx.coroutines.sync.Mutex` works in this Kotlin/Native setup, across real worker threads. */
    @Test
    fun mutexGuardedCounterAcrossWorkerThreadsLosesNoIncrement() {
        val workerCount = 8
        val incrementsPerWorker = 1_000
        val mutex = Mutex()
        var counter = 0
        runBlocking {
            (0 until workerCount).map {
                async(Dispatchers.Default) { repeat(incrementsPerWorker) { mutex.withLock { counter++ } } }
            }.awaitAll()
        }
        assertEquals(workerCount * incrementsPerWorker, counter, "RACE DETECTED: counter=$counter")
    }

    /**
     * `delay()` suspends the coroutine and `Dispatchers.Default` may resume it on another OS thread. A live Godot
     * reference held across that suspension must not care which thread continues: the engine is not called from the
     * worker, only the wrapper travels.
     */
    @Test
    fun godotReferenceHeldAcrossASuspensionSurvivesIt() {
        val resource = Resource()
        val pointer = runBlocking(Dispatchers.Default) {
            val before = resource.rawPtr
            delay(50.milliseconds)
            assertEquals(before, resource.rawPtr, "the wrapper moved to another object")
            resource.rawPtr
        }
        assertEquals(resource.rawPtr, pointer)
        assertEquals(1, resource.getReferenceCount(), "refcount=${resource.getReferenceCount()}")
    }

    /**
     * The recommended pattern for CPU-bound work such as JSON parsing: pure Kotlin on a background thread
     * (`kotlinx.serialization`, no Godot calls at all), and only the resulting plain value comes back.
     */
    @Test
    fun jsonParsedWithPureKotlinOnABackgroundThreadComesBackAsPlainValues() {
        val text = """{"name": "goblin", "health": 42, "tags": ["enemy", "boss"]}"""
        val (health, tags) = runBlocking {
            withContext(Dispatchers.Default) {
                val parsed = KxJson.parseToJsonElement(text).jsonObject
                parsed.getValue("health").jsonPrimitive.int to
                    parsed.getValue("tags").jsonArray.map { it.jsonPrimitive.content }
            }
        }
        assertEquals(42, health, "health=$health")
        assertEquals(listOf("enemy", "boss"), tags, "tags=$tags")
    }
}
