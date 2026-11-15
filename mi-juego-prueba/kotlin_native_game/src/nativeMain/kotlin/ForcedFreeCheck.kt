@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Live check of issue #218 (`forced_free_check.tscn`): the engine may destroy a `RefCounted` with `memdelete`, ignoring
 * its reference count. The editor does it on its first run, when it instantiates every extension class to read the
 * default values of its properties for the docs. Kotlin still owed the reference its wrapper took, and the shutdown
 * drain then called `unreference()` on freed memory: the process crashed at exit.
 *
 * The pass criterion is the exit code (0): the crash happens after the checks, in the extension's deinitialization.
 * The wrappers are kept reachable until exit, so their Cleaners do not run first and hide the problem.
 */
@Godot class ForcedFreeCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("ForcedFreeCheck")
    private val kept = mutableListOf<Any>()

    override fun _ready() {
        with(checks) {
            check("a @Godot RefCounted destroyed by the engine is no longer owed") {
                val resource = instantiate<DowncastResource>()
                kept += resource
                ObjectBinding.destroyRaw(resource.rawPtr)
            }
            check("an engine RefCounted destroyed by the engine is no longer owed") {
                val resource = Resource()
                kept += resource
                ObjectBinding.destroyRaw(resource.rawPtr)
            }
        }
        getTree().quit(checks.finish())
    }
}
