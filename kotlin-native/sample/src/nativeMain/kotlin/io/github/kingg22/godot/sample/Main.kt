package io.github.kingg22.godot.sample

import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.print
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.ffi.GDExtensionBool
import io.github.kingg22.godot.internal.ffi.GDExtensionClassLibraryPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionInitialization
import io.github.kingg22.godot.internal.ffi.GDExtensionInitializationLevel
import io.github.kingg22.godot.internal.ffi.GDExtensionInterfaceGetProcAddress
import io.github.kingg22.godot.internal.ffi.TRUE
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.pointed
import kotlinx.cinterop.staticCFunction

@Suppress("unused") // Invoked by Godot
@CName("godot_kotlin_init")
fun godotKotlinInit(
    pGetProcAddress: GDExtensionInterfaceGetProcAddress,
    pLibrary: GDExtensionClassLibraryPtr,
    initialization: CPointer<GDExtensionInitialization>,
): GDExtensionBool {
    BindingProcAddressHolder.initialize(pGetProcAddress, pLibrary)

    val initialization = initialization.pointed
    initialization.initialize = staticCFunction(::initialize)
    initialization.deinitialize = staticCFunction(::deinitialize)
    initialization.userdata = null
    initialization.minimum_initialization_level = GDEXTENSION_INITIALIZATION_SCENE

    return GDExtensionBool.TRUE
}

private fun initialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
    when (level) {
        GDEXTENSION_INITIALIZATION_CORE -> println("✓ CORE initialized")

        GDEXTENSION_INITIALIZATION_SERVERS -> println("✓ SERVERS initialized")

        GDEXTENSION_INITIALIZATION_SCENE -> {
            println("✓ SCENE initialized")
            registerCustomSignalClass()
            // Register the signal for CustomSignalClass
            registerSignalsForCustomSignalClass()
        }

        GDEXTENSION_INITIALIZATION_EDITOR -> GD.print("✓ EDITOR initialized. Hello from Kotlin Native")

        // False positive https://youtrack.jetbrains.com/issue/KT-77521
        else -> println("Unexpected $level, userdata: $userdata")
    }
}

private fun deinitialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
    println("✗ DEINITIALIZE LEVEL = $level, userdata: $userdata")
}
