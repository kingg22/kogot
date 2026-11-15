package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.internal.ffi.GDExtensionClassCallVirtual
import io.github.kingg22.godot.internal.ffi.GDExtensionStringNamePtr

/**
 * Resolves the [GDExtensionClassCallVirtual] trampoline for a `getVirtual` query, or `null` if unhandled.
 *
 * Every `get_virtual` callback (generated per `@Godot` class and hand-written for the script classes) goes through
 * here. The engine resolves a virtual lazily, on its first call, so this can run on a worker thread; a failure is
 * reported and answered as "not overridden" instead of aborting the process (issue #113).
 */
@InternalBinding
public inline fun resolveVirtualCall(
    funcNamePtr: GDExtensionStringNamePtr?,
    resolve: (StringName) -> GDExtensionClassCallVirtual?,
): GDExtensionClassCallVirtual? {
    if (funcNamePtr == null) return null
    return guardCallback("kogot.get_virtual", null) { StringName(funcNamePtr).use(resolve) }
}
