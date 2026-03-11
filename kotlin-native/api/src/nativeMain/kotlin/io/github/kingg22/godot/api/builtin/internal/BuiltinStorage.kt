package io.github.kingg22.godot.api.builtin.internal

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.reinterpret

internal typealias BuiltinStorage = CPointer<ByteVar>

// Si usas nativeHeap o similar, necesitas alinear:
// nativeHeap.alloc usa el alignment del tipo
// Necesitas alloc con alignment explícito de 8
// LongVar tiene align(8) en Kotlin Native en x64
internal fun allocateBuiltinStorage(size: Int): CPointer<ByteVar> {
    val longs = (size + 7) / 8 // ceiling division
    return nativeHeap.allocArray<LongVar>(longs).reinterpret()
}

internal fun freeBuiltinStorage(storage: BuiltinStorage) {
    nativeHeap.free(storage.rawValue)
}
