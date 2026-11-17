package io.github.kingg22.godot.test.leak

// No mallinfo here: the leak tests measure the engine memory only on this platform.
internal actual fun cHeapInUse(): Long? = null
