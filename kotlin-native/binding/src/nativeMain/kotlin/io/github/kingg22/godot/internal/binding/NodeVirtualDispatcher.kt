package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.asStringName
import io.github.kingg22.godot.internal.ffi.GDExtensionClassCallVirtual
import io.github.kingg22.godot.internal.ffi.GDExtensionClassGetVirtual2
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction

/** MVP for virtual methods in [io.github.kingg22.godot.api.core.Node] */
@OptIn(ExperimentalForeignApi::class)
@InternalBinding
public object NodeVirtualDispatcher {

    public val getVirtual: GDExtensionClassGetVirtual2 = staticCFunction { _, funcNamePtr, _ ->
        if (funcNamePtr == null) return@staticCFunction null

        val funcName = StringName(funcNamePtr)

        val result: GDExtensionClassCallVirtual? = when (funcName) {
            "_ready".asStringName() -> NodeVirtualCalls.ready
            "_process".asStringName() -> NodeVirtualCalls.process
            else -> null
        }

        funcName.close()
        result
    }
}
