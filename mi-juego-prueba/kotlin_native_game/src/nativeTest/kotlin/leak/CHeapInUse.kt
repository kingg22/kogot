package io.github.kingg22.godot.test.leak

/**
 * Bytes in use in the C heap (`malloc`): where `nativeHeap` puts the storage of every builtin wrapper (a `Variant` is
 * 24 bytes there) and where the engine allocates too. `OS.get_static_memory_usage()` only sees the engine's own
 * allocations, so a leaked wrapper whose engine value owns nothing (a `Variant` holding a `Node`) shows only here.
 *
 * `null` where the platform has no cheap query for it (only glibc's `mallinfo` is used, in `linuxTest`): the leak
 * tests then measure the engine memory only.
 */
internal expect fun cHeapInUse(): Long?
