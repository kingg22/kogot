package io.github.kingg22.godot.internal.register

import io.github.kingg22.godot.internal.ffi.GDExtensionClassLibraryPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionInterfaceGetProcAddress
import io.github.kingg22.godot.internal.ffi.GDExtensionInterfacePrintError
import io.github.kingg22.godot.internal.register.jni.ffi.JavaVMVar
import io.github.kingg22.godot.internal.register.jni.ffi.jclass
import io.github.kingg22.godot.internal.register.jni.ffi.jmethodID
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

// --- Estado Global ---
@OptIn(ExperimentalForeignApi::class)
object RuntimeState {
    var jvm: CPointer<JavaVMVar>? = null
    var jvmHandle: COpaquePointer? = null
    var jvmStarted = false
    var javaInitialized = false

    var bridgeClass: jclass? = null
    var midInitialize: jmethodID? = null
    var midShutdown: jmethodID? = null
    var midOnLevelInit: jmethodID? = null
    var midOnLevelDeinit: jmethodID? = null

    var getProcAddress: GDExtensionInterfaceGetProcAddress? = null
    var libraryPtr: GDExtensionClassLibraryPtr? = null
    var printError: GDExtensionInterfacePrintError? = null

    var mainThreadId: ULong = 0u
    var mainThreadSet = false
}
