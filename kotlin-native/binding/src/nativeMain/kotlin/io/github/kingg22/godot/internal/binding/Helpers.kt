package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.internal.ffi.GDExtensionCallError
import io.github.kingg22.godot.internal.ffi.GDExtensionCallErrorType
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.pointed

/**
 * Runs [block] with a `record` function that turns a [String] into a [StringName] pointer and keeps
 * the [StringName] alive until [block] returns, then destructs every recorded name.
 *
 * ClassDB registration (`registerExtensionClassMethod/Property/Signal`) reads `StringName` fields out
 * of `GDExtension*Info` structs by pointer: the names must outlive struct construction but Godot has
 * copied their contents by the time the `register…` call returns, so they can be freed immediately
 * after. Building them inline as `x.toStringName().rawPtr` never frees them — they surface as
 * `Orphan StringName` / `N unclaimed string names at exit` in a verbose run.
 */
@InternalBinding
public inline fun <R> withTransientStringNames(block: (record: (String) -> COpaquePointer) -> R): R {
    val names = ArrayList<StringName>()
    try {
        return block { value -> value.toStringName().also(names::add).rawPtr }
    } finally {
        for (name in names) name.close()
    }
}

@InternalBinding
public fun CPointer<GDExtensionCallError>?.write(
    error: GDExtensionCallErrorType = GDEXTENSION_CALL_OK,
    argument: Int = 0,
    expected: Int = 0,
) {
    if (this == null) return
    this.pointed.error = error
    this.pointed.argument = argument
    this.pointed.expected = expected
}
