package io.github.kingg22.godot.internal.register

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

// --- Abstracción de Plataforma para Carga de Librerías ---

internal expect val isWindows: Boolean
internal expect val isMacOS: Boolean

internal expect fun loadLibrary(path: String): COpaquePointer?

internal expect fun getSymbol(handle: COpaquePointer, name: String): COpaquePointer?

internal expect fun closeLibrary(handle: COpaquePointer)

// --- Entrypoints de Godot ---

private fun initialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
    if (!ensureMainThread("initialize callback")) return

    when (level) {
        GDEXTENSION_INITIALIZATION_CORE -> logInfo("Init phase CORE")

        GDEXTENSION_INITIALIZATION_SERVERS -> logInfo("Init phase SERVERS")

        GDEXTENSION_INITIALIZATION_SCENE -> {
            logInfo("Init phase SCENE")
            if (ensureJvmStarted() && ensureJavaBridgeInitialized()) {
                callLevelCallback(true, level.value.toShort())
            } else {
                logError("Failed to initialize JVM at SCENE phase")
            }
        }

        GDEXTENSION_INITIALIZATION_EDITOR -> {
            logInfo("Init phase EDITOR")
            if (RuntimeState.jvmStarted) callLevelCallback(true, level.value.toShort())
        }

        else -> {
            logInfo("Unexpected initialization phase: $level, ignoring $userdata")
        }
    }
}

private fun deinitialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
    val _ = userdata
    if (!ensureMainThread("deinitialize callback")) return

    callLevelCallback(false, level.value.toShort())
    if (level == GDEXTENSION_INITIALIZATION_CORE) {
        destroyJvm()
    }
}

@Suppress("unused")
@CName("godot_java_bridge_init")
fun godotJavaBridgeInit(
    pGetProcAddress: GDExtensionInterfaceGetProcAddress,
    pLibrary: GDExtensionClassLibraryPtr,
    rInitialization: CPointer<GDExtensionInitialization>,
): GDExtensionBool {
    RuntimeState.getProcAddress = pGetProcAddress
    RuntimeState.libraryPtr = pLibrary
    captureMainThread()

    logInfo("=== Godot Java Bridge Initialization (Kotlin Native) ===")

    val init = rInitialization.pointed
    init.initialize = staticCFunction(::initialize)
    init.deinitialize = staticCFunction(::deinitialize)
    init.userdata = null
    init.minimum_initialization_level = GDEXTENSION_INITIALIZATION_CORE

    return GDExtensionBool.TRUE
}
