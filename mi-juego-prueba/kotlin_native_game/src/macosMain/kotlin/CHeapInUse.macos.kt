// No mallinfo here: NativeLeakCheck measures the engine memory only on this platform.
internal actual fun cHeapInUse(): Long? = null
