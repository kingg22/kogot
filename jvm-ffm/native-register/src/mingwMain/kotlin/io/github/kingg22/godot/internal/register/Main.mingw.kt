package io.github.kingg22.godot.internal.register

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.reinterpret
import platform.windows.FreeLibrary
import platform.windows.GetProcAddress
import platform.windows.LoadLibraryA

internal actual fun loadLibrary(path: String): COpaquePointer? = LoadLibraryA(path)
internal actual fun getSymbol(handle: COpaquePointer, name: String): COpaquePointer? =
    GetProcAddress(handle.reinterpret(), name)

internal actual fun closeLibrary(handle: COpaquePointer) {
    FreeLibrary(handle.reinterpret())
}

internal actual val isWindows: Boolean inline get() = true
internal actual val isMacOS: Boolean inline get() = false
