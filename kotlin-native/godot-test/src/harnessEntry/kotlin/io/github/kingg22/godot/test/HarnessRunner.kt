package io.github.kingg22.godot.test

import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.core.Node
import kotlinx.cinterop.COpaquePointer
import kotlin.native.internal.test.testLauncherEntryPoint

/**
 * The scene root of the host project. The Kotlin/Native test runner starts from its `_ready`, i.e. with a live
 * `SceneTree` on the main thread, and not from the extension initialization: at that point the engine is not fully up
 * (GDScript cannot compile yet, for one). The run ends the process with the runner's exit code.
 */
@Godot
class HarnessRunner(nativePtr: COpaquePointer) : Node(nativePtr) {
    override fun _ready() {
        val code = testLauncherEntryPoint(arrayOf("--ktest_logger=TEAMCITY"))
        println("$RESULT_MARKER$code")
        getTree().quit(code)
    }

    private companion object {
        /** Read by the `godotTest` Gradle task: its absence means the engine died before the tests finished. */
        const val RESULT_MARKER = "KOGOT_TEST_RESULT exit="
    }
}
