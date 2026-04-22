package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.internal.binding.StringBinding
import kotlinx.cinterop.*

public fun GodotString.Companion.fromUtf8(value: String): GodotString = GodotString(null).also {
    StringBinding.instance.newWithUtf8Chars(it.rawPtr, value)
}

public fun GodotString.Companion.fromUtf16(value: String): GodotString = GodotString(null).also {
    memScoped {
        StringBinding.instance.newWithUtf16CharsRaw(it.rawPtr, value.utf16.ptr)
    }
}

public fun GodotString.Companion.fromUtf32(value: String): GodotString = GodotString(null).also {
    memScoped {
        StringBinding.instance.newWithUtf32CharsRaw(it.rawPtr, value.utf32.ptr.reinterpret())
    }
}

public fun GodotString.toKStringFromUtf8(): String = memScoped {
    // First call with null rText to get the actual byte length needed (excluding null terminator)
    val utf8Length = StringBinding.instance.toUtf8CharsRaw(
        this@toKStringFromUtf8.rawPtr,
        null,
        0,
    )
    // Allocate buffer with extra space for null terminator
    val buffer = allocArray<ByteVar>(utf8Length + 1)
    // Actually write the UTF-8 chars
    val _ = StringBinding.instance.toUtf8CharsRaw(
        this@toKStringFromUtf8.rawPtr,
        buffer,
        utf8Length + 1,
    )
    // Convert to Kotlin String (toKString expects null-terminated string)
    buffer.toKStringFromUtf8()
}

public fun GodotString.toKStringFromUtf16(): String = memScoped {
    // First call with null rText to get the actual byte length needed (excluding null terminator)
    val utf8Length = StringBinding.instance.toUtf16CharsRaw(
        this@toKStringFromUtf16.rawPtr,
        null,
        0,
    )
    // Allocate buffer with extra space for null terminator
    val buffer = allocArray<UShortVar>(utf8Length + 1)
    // Actually write the UTF-16 chars
    val _ = StringBinding.instance.toUtf16CharsRaw(
        this@toKStringFromUtf16.rawPtr,
        buffer,
        utf8Length + 1,
    )
    // Convert to Kotlin String (toKString expects null-terminated string)
    buffer.toKStringFromUtf16()
}

public fun GodotString.toKStringFromUtf32(): String = memScoped {
    // First call with null rText to get the actual byte length needed (excluding null terminator)
    val utf8Length = StringBinding.instance.toUtf32CharsRaw(
        this@toKStringFromUtf32.rawPtr,
        null,
        0,
    )
    // Allocate buffer with extra space for null terminator
    val buffer = allocArray<UIntVar>(utf8Length + 1)
    // Actually write the UTF-32 chars
    val _ = StringBinding.instance.toUtf32CharsRaw(
        this@toKStringFromUtf32.rawPtr,
        buffer,
        utf8Length + 1,
    )
    // Convert to Kotlin String (toKString expects null-terminated string)
    buffer.reinterpret<IntVar>().toKStringFromUtf32()
}
