@file:OptIn(ObsoleteWorkersApi::class)

package io.github.kingg22.godot.internal.binding

import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalBinding::class)
class MainThreadTest {
    /** Runs [block] on a fresh OS thread and returns its result. */
    private fun <T> onOtherThread(block: () -> T): T {
        val worker = Worker.start()
        try {
            return worker.execute(TransferMode.SAFE, { block }) { it() }.result
        } finally {
            worker.requestTermination().result
        }
    }

    @Test
    fun `only the marked thread is the main thread`() {
        MainThread.markCurrent()

        assertTrue(MainThread.isCurrent)
        assertFalse(onOtherThread { MainThread.isCurrent }, "a thread that never marked itself is not the main thread")
        // Marking is idempotent and per thread: the marked thread keeps its answer.
        MainThread.markCurrent()
        assertTrue(MainThread.isCurrent)
    }

    @Test
    fun `check answers without throwing on any thread`() {
        MainThread.markCurrent()

        assertTrue(MainThread.check("test operation"))
        // Off the main thread it reports (no engine here, so through the stderr fallback) and answers false.
        assertFalse(onOtherThread { MainThread.check("test operation") })
    }
}
