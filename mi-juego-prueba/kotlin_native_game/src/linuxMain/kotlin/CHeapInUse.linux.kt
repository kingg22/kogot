@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.linux.mallinfo

// glibc: bytes handed out by malloc from the arenas plus the ones mapped directly (large blocks).
internal actual fun cHeapInUse(): Long? = mallinfo().useContents { uordblks.toLong() + hblkhd.toLong() }
