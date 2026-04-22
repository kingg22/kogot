package io.github.kingg22.godot.sample

import io.github.kingg22.godot.api.builtin.GodotString
import io.github.kingg22.godot.api.builtin.toKStringFromUtf16
import io.github.kingg22.godot.api.builtin.toKStringFromUtf32
import io.github.kingg22.godot.api.builtin.toKStringFromUtf8
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.print
import io.github.kingg22.godot.api.utils.pushError
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.ffi.*
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

private fun initialize(
    userdata: COpaquePointer?,
    level: GDExtensionInitializationLevel,
) {
    when (level) {
        GDEXTENSION_INITIALIZATION_CORE -> {
            println("✓ CORE initialized")
        }

        GDEXTENSION_INITIALIZATION_SERVERS -> {
            println("✓ SERVERS initialized")
        }

        GDEXTENSION_INITIALIZATION_SCENE -> {
            println("✓ SCENE initialized")
            registerCustomSignalClass()
            // Register the signal for CustomSignalClass
            registerSignalsForCustomSignalClass()
        }

        GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_EDITOR -> {
            GD.print("✓ EDITOR initialized. Hello from Kotlin Native")
            // Test GodotString to Kotlin String conversion
            try {
                GodotString("Hello from Kotlin!").use { godotString ->
                    val kotlinString = godotString.toKStringFromUtf8()
                    val kotlin2String = godotString.toKStringFromUtf16()
                    val kotlin3String = godotString.toKStringFromUtf32()
                    GD.print("GodotString.toKString() result: utf-8= '$kotlinString', utf-16= '$kotlin2String', utf-32= '$kotlin3String'")
                }
            } catch (error: Throwable) {
                GD.pushError("GodotString.toKString() error: $error")
            }
        }

        // False positive https://youtrack.jetbrains.com/issue/KT-77521
        else -> {
            println("Unexpected $level, userdata: $userdata")
        }
    }
}

private fun deinitialize(
    userdata: COpaquePointer?,
    level: GDExtensionInitializationLevel,
) {
    println("✗ DEINITIALIZE LEVEL = $level, userdata: $userdata")
}
