package io.github.kingg22.godot.test

import io.github.kingg22.godot.api.annotations.Export
import io.github.kingg22.godot.api.annotations.ExportMethod
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.core.Node
import kotlinx.cinterop.COpaquePointer

/*
 * `@Godot` fixtures of the test source set. The kogot processor runs on it, so these are registered in the engine
 * before the tests start, exactly like the classes of a game.
 */

/** A registered class with an exported property, an exported method and a `_ready` virtual. */
@Godot class HarnessProbe(nativePtr: COpaquePointer) : Node(nativePtr) {
    @Export
    var level: Int = 0

    var readyCalls: Int = 0
        private set

    @ExportMethod
    fun triple(value: Int): Int = value * 3

    override fun _ready() {
        readyCalls++
    }
}

/*
 * Registration order (#254): the child below is declared BEFORE its parent. The processor registers in source order, so
 * Godot refuses `OrderChildFirst` ("non-existing parent class"). Keep this declaration order: it is the reproduction.
 */
@Godot class OrderChildFirst(nativePtr: COpaquePointer) : OrderParentLast(nativePtr)

@Godot open class OrderParentLast(nativePtr: COpaquePointer) : Node(nativePtr)
