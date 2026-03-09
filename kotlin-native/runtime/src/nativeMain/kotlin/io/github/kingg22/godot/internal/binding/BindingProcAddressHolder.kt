package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionClassLibraryPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionInterfaceGetProcAddress

public class BindingProcAddressHolder private constructor(
    public val getProcAddress: GDExtensionInterfaceGetProcAddress,
    public val library: GDExtensionClassLibraryPtr,
) {
    public companion object {
        public fun initialize(
            getProcAddress: GDExtensionInterfaceGetProcAddress,
            libraryPtr: GDExtensionClassLibraryPtr,
        ) {
            instance = BindingProcAddressHolder(getProcAddress, libraryPtr)
        }
    }
}

private var instance: BindingProcAddressHolder? = null

public val bindingProcAddressHolder: BindingProcAddressHolder
    get() = instance ?: error("GDExtension getProcAddress holder is not initialized. Call initialize() first.")
