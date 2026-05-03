package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionClassInstancePtr
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlin.contracts.contract

/**
 * Inline function to get an instance from a [GDExtensionClassInstancePtr] ([COpaquePointer]) that is assumed to be a [StableRef].
 *
 * @throws [IllegalArgumentException] if it's `null`.
 */
@InternalBinding
public inline fun <reified T : Any> GDExtensionClassInstancePtr?.getInstance(): T {
    contract { returns() implies (this@getInstance != null) }
    return requireNotNull(this).asStableRef<T>().get()
}
