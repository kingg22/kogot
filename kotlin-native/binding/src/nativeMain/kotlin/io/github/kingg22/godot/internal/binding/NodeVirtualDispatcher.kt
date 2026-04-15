package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.asStringName
import io.github.kingg22.godot.internal.ffi.GDExtensionClassGetVirtual2
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction

/** MVP for virtual methods in [io.github.kingg22.godot.api.core.Node] */
@OptIn(ExperimentalForeignApi::class)
@InternalBinding
public object NodeVirtualDispatcher {

    public val getVirtual: GDExtensionClassGetVirtual2 = staticCFunction { _, funcNamePtr, _ ->
        if (funcNamePtr == null) return@staticCFunction null

        StringName(funcNamePtr).use { funcName ->
            "_ready".asStringName().use { readyName ->
                if (funcName == readyName) return@staticCFunction NodeVirtualCalls.ready
            }
            "_process".asStringName().use { processName ->
                if (funcName == processName) return@staticCFunction NodeVirtualCalls.process
            }
            null
        }
    }
}
