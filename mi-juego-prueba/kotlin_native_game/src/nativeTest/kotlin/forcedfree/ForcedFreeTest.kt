@file:OptIn(ExperimentalForeignApi::class)

package io.github.kingg22.godot.test.forcedfree

import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.internal.binding.ObjectBinding
import io.github.kingg22.godot.test.downcast.DowncastResource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test

/**
 * Issue #218: the engine may destroy a `RefCounted` with `memdelete`, ignoring its reference count. The editor does it
 * on its first run, when it instantiates every extension class to read the default values of its properties for the
 * docs. Kotlin still owed the reference its wrapper took, and the shutdown drain then called `unreference()` on freed
 * memory: the process crashed at exit.
 *
 * The pass criterion is the exit code of the Godot process: the crash happens after the test body, in the extension's
 * deinitialization. The wrappers are kept reachable until exit, so their Cleaners do not run first and hide the problem.
 */
class ForcedFreeTest {
    private val kept = mutableListOf<Any>()

    @Test
    fun godotRefCountedDestroyedByTheEngineIsNoLongerOwed() {
        val resource = instantiate<DowncastResource>()
        kept += resource
        ObjectBinding.destroyRaw(resource.rawPtr)
    }

    @Test
    fun engineRefCountedDestroyedByTheEngineIsNoLongerOwed() {
        val resource = Resource()
        kept += resource
        ObjectBinding.destroyRaw(resource.rawPtr)
    }
}
