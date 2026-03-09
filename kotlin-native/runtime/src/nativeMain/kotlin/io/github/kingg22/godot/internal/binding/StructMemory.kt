/*
 * Low-level struct field access by byte offsets.
 *
 * The generated native-struct wrappers rely on this for primitive/pointer reads/writes when the struct isn't declared
 * in any cinterop header (so we cannot use `memberAt` / `CStructVar` fields).
 */

@file:Suppress("NOTHING_TO_INLINE")

package io.github.kingg22.godot.internal.binding

import kotlinx.cinterop.*
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
public inline fun CPointer<ByteVar>.at(offsetBytes: Int): CPointer<ByteVar> = requireNotNull(this + offsetBytes)

@ApiStatus.Internal
public inline fun getBoolean(base: CPointer<ByteVar>, offsetBytes: Int): Boolean =
    base.at(offsetBytes).reinterpret<UByteVar>()[0] != 0.toUByte()

@ApiStatus.Internal
public inline fun setBoolean(base: CPointer<ByteVar>, offsetBytes: Int, value: Boolean) {
    base.at(offsetBytes).reinterpret<UByteVar>()[0] = if (value) 1u else 0u
}

@ApiStatus.Internal
public inline fun getByte(base: CPointer<ByteVar>, offsetBytes: Int): Byte =
    base.at(offsetBytes).reinterpret<ByteVar>()[0]

@ApiStatus.Internal
public inline fun setByte(base: CPointer<ByteVar>, offsetBytes: Int, value: Byte) {
    base.at(offsetBytes).reinterpret<ByteVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getUByte(base: CPointer<ByteVar>, offsetBytes: Int): UByte =
    base.at(offsetBytes).reinterpret<UByteVar>()[0]

@ApiStatus.Internal
public inline fun setUByte(base: CPointer<ByteVar>, offsetBytes: Int, value: UByte) {
    base.at(offsetBytes).reinterpret<UByteVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getShort(base: CPointer<ByteVar>, offsetBytes: Int): Short =
    base.at(offsetBytes).reinterpret<ShortVar>()[0]

@ApiStatus.Internal
public inline fun setShort(base: CPointer<ByteVar>, offsetBytes: Int, value: Short) {
    base.at(offsetBytes).reinterpret<ShortVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getUShort(base: CPointer<ByteVar>, offsetBytes: Int): UShort =
    base.at(offsetBytes).reinterpret<UShortVar>()[0]

@ApiStatus.Internal
public inline fun setUShort(base: CPointer<ByteVar>, offsetBytes: Int, value: UShort) {
    base.at(offsetBytes).reinterpret<UShortVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getInt(base: CPointer<ByteVar>, offsetBytes: Int): Int = base.at(offsetBytes).reinterpret<IntVar>()[0]

@ApiStatus.Internal
public inline fun setInt(base: CPointer<ByteVar>, offsetBytes: Int, value: Int) {
    base.at(offsetBytes).reinterpret<IntVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getUInt(base: CPointer<ByteVar>, offsetBytes: Int): UInt =
    base.at(offsetBytes).reinterpret<UIntVar>()[0]

@ApiStatus.Internal
public inline fun setUInt(base: CPointer<ByteVar>, offsetBytes: Int, value: UInt) {
    base.at(offsetBytes).reinterpret<UIntVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getLong(base: CPointer<ByteVar>, offsetBytes: Int): Long =
    base.at(offsetBytes).reinterpret<LongVar>()[0]

@ApiStatus.Internal
public inline fun setLong(base: CPointer<ByteVar>, offsetBytes: Int, value: Long) {
    base.at(offsetBytes).reinterpret<LongVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getULong(base: CPointer<ByteVar>, offsetBytes: Int): ULong =
    base.at(offsetBytes).reinterpret<ULongVar>()[0]

@ApiStatus.Internal
public inline fun setULong(base: CPointer<ByteVar>, offsetBytes: Int, value: ULong) {
    base.at(offsetBytes).reinterpret<ULongVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getFloat(base: CPointer<ByteVar>, offsetBytes: Int): Float =
    base.at(offsetBytes).reinterpret<FloatVar>()[0]

@ApiStatus.Internal
public inline fun setFloat(base: CPointer<ByteVar>, offsetBytes: Int, value: Float) {
    base.at(offsetBytes).reinterpret<FloatVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getDouble(base: CPointer<ByteVar>, offsetBytes: Int): Double =
    base.at(offsetBytes).reinterpret<DoubleVar>()[0]

@ApiStatus.Internal
public inline fun setDouble(base: CPointer<ByteVar>, offsetBytes: Int, value: Double) {
    base.at(offsetBytes).reinterpret<DoubleVar>()[0] = value
}

@ApiStatus.Internal
public inline fun getPointer(base: CPointer<ByteVar>, offsetBytes: Int): COpaquePointer =
    requireNotNull(base.at(offsetBytes).reinterpret<COpaquePointerVar>()[0])

@ApiStatus.Internal
public inline fun setPointer(base: CPointer<ByteVar>, offsetBytes: Int, value: COpaquePointer) {
    base.at(offsetBytes).reinterpret<COpaquePointerVar>()[0] = value
}
