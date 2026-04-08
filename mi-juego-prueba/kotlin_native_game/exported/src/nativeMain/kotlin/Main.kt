@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class) // Usage of C-interop API

import io.github.kingg22.godot.internal.binding.BindingInitializationCallbacks
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.ffi.GDExtensionBool
import io.github.kingg22.godot.internal.ffi.GDExtensionClassLibraryPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionInitialization
import io.github.kingg22.godot.internal.ffi.GDExtensionInitializationLevel
import io.github.kingg22.godot.internal.ffi.GDExtensionInterfaceGetProcAddress
import io.github.kingg22.godot.internal.ffi.TRUE
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.pointed
import kotlinx.cinterop.staticCFunction
import kotlin.experimental.ExperimentalNativeApi

// This is the entry point for kotlin-native, provide a concrete class of BindingInitializationCallbacks
// with the methods onScene overridden to register classes

@OptIn(InternalBinding::class) // Generated code is allowed to register binding
@Suppress("unused") // Invoked by .Godot
@CName("godot_kotlin_init") // DON'T EDIT THIS NAME, must be equals in the gdextension file
fun godotKotlinInit(
    pGetProcAddress: GDExtensionInterfaceGetProcAddress,
    pLibrary: GDExtensionClassLibraryPtr,
    initialization: CPointer<GDExtensionInitialization>,
): GDExtensionBool {
    BindingProcAddressHolder.initialize(pGetProcAddress, pLibrary)
    val callbacks = StableRef.create<BindingInitializationCallbacks>(BindingInitializationCallbacks())

    val initialization = initialization.pointed
    initialization.initialize = staticCFunction(callbacks.get()::initialize)
    initialization.deinitialize = staticCFunction(callbacks.get()::deinitialize)
    initialization.userdata = null
    initialization.minimum_initialization_level = GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SCENE

    return GDExtensionBool.TRUE
}
