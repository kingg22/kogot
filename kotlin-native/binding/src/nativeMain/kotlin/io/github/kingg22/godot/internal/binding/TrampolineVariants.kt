package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.getValueOrNull
import io.github.kingg22.godot.internal.ffi.GDExtensionVariantPtr
import kotlinx.cinterop.COpaquePointer

/**
 * Writes [variant], the conversion of [source], into the engine's [returnValue] slot and frees it afterwards: the
 * engine keeps its own copy. A [variant] that *is* [source] (the method or property already was a `Variant`) belongs
 * to the user and stays open. Used by the trampolines the processor generates for `@Export` getters and
 * `@ExportMethod` returns, which leaked the `Variant` of every call (#219).
 */
@InternalBinding
public fun copyReturnVariant(returnValue: GDExtensionVariantPtr, variant: Variant, source: Any?) {
    try {
        VariantBinding.newCopyRaw(returnValue, variant.rawPtr)
    } finally {
        if (variant !== source) variant.close()
    }
}

/**
 * Reads the engine argument at [ptr] as a [T] for an `@Export` setter or an `@ExportMethod` parameter, or `null` when
 * it is not one. The argument is copied into a `Variant` to convert it, and that copy is freed afterwards, unless the
 * value *is* the copy (a `Variant` parameter), which the user then owns like any other closeable argument. Before, the
 * copy of every argument of every call leaked (#219).
 */
@InternalBinding
public inline fun <@MustBeVariant reified T> readArgumentOrNull(ptr: COpaquePointer): T? {
    val variant = Variant(ptr)
    val value = try {
        variant.getValueOrNull<T>()
    } catch (t: Throwable) {
        variant.close()
        throw t
    }
    if (value !== variant) variant.close()
    return value
}
