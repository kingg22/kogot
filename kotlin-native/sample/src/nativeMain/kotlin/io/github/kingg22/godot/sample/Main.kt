package io.github.kingg22.godot.sample

import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.asVariantString
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.PrintBinding
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

fun initialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
    when (level) {
        GDEXTENSION_INITIALIZATION_CORE -> {
            println("✓ CORE initialized")
        }

        GDEXTENSION_INITIALIZATION_SERVERS -> {
            println("✓ SERVERS initialized")
        }

        GDEXTENSION_INITIALIZATION_SCENE -> {
            println("✓ SCENE initialized")
        }

        GDEXTENSION_INITIALIZATION_EDITOR -> {
            println("✓ EDITOR initialized - printing to Godot now")
            testGodotPrinting()
        }

        GDExtensionInitializationLevel.GDEXTENSION_MAX_INITIALIZATION_LEVEL -> {}
    }
}

fun deinitialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
    println("✗ DEINITIALIZE LEVEL = $level")
}

private fun testGodotPrinting() {
    try {
        // Load print function
        PrintBinding.instance.error(
            description = "This is a warning message",
            function = "testGodotPrinting",
            file = "MainKt",
            pLine = 42,
            editorNotify = true,
        )
        val result = GD.pingpong(-3.0, 3.0)
        GD.print("Result of ping pong utility: $result.".asVariantString(), Variant.NIL())
        println("Finished printing to Godot, the ping pong utility returned $result")
        testBuiltinLayouts()
        println("Finished testing builtin layouts")
    } catch (e: Throwable) {
        println("Exception captured: ${e.message}")
        e.printStackTrace()
    }
}
