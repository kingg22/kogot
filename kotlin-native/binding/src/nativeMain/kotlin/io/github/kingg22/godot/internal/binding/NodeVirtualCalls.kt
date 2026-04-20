package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.internal.ffi.GDExtensionClassCallVirtual
import io.github.kingg22.godot.internal.ffi.GDExtensionConstTypePtrVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value

/** MVP for [Node] virtual calls */
@OptIn(ExperimentalForeignApi::class)
@InternalBinding
public object NodeVirtualCalls {

    /** Binding virtual call for [Node._ready] */
    public val ready: GDExtensionClassCallVirtual = staticCFunction { instancePtr, _, _ ->
        val instance = instancePtr.getInstance<Node>()
        instance._ready()
    }

    /** Binding virtual call for [Node._process] */
    public val process: GDExtensionClassCallVirtual = staticCFunction {
            instancePtr,
            args: CArrayPointer<GDExtensionConstTypePtrVar>?,
            _,
        ->
        val instance = instancePtr.getInstance<Node>()

        val delta = args
            ?.get(0)
            ?.reinterpret<DoubleVar>()
            ?.pointed
            ?.value ?: 0.0

        instance._process(delta)
    }
}
