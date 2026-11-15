package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.internal.ffi.GDExtensionVariantPtr

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
