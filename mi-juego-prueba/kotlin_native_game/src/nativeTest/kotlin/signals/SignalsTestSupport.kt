@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, ExperimentalGodotApi::class)

package io.github.kingg22.godot.test.signals

import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.SceneTree
import io.github.kingg22.godot.api.singleton.Engine
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Adds this node under the SceneTree root, so `ready` / `tree_entered` really happen. The harness runs every test in
 * its own Godot process, so the node needs no cleanup beyond the `queueFree` each test already does.
 */
internal fun <T : Node> T.inScene(): T = also {
    val tree = Engine.instance.getMainLoop() as SceneTree
    tree.root.addChild(node = it)
}

/** An emission Godot rejected is a failure of the test, not a value to drop. */
internal fun GodotError.ok() {
    if (this != GodotError.OK) throw AssertionError("emit returned $this")
}

internal fun sameObject(a: GodotObject?, b: GodotObject?) =
    a != null && b != null && a.getInstanceId() == b.getInstanceId()
