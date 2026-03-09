// TODO make all internals published api when shared internals are beta/stable
// https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0451-shared-internals.md

@file:Suppress("NOTHING_TO_INLINE")

package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionBool
import io.github.kingg22.godot.internal.ffi.GDExtensionCallError
import io.github.kingg22.godot.internal.ffi.GDExtensionCallErrorType
import io.github.kingg22.godot.internal.ffi.GDExtensionConstTypePtr
import io.github.kingg22.godot.internal.ffi.GDExtensionConstTypePtrVar
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtrVar
import io.github.kingg22.godot.internal.ffi.GDExtensionTypePtr
import io.github.kingg22.godot.internal.ffi.GDExtensionTypePtrVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.set
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
public data class BindingStatus(val valid: Boolean? = null, val outOfBounds: Boolean? = null)

@ApiStatus.Internal
public data class BindingBooleanResult(val value: Boolean, val valid: Boolean? = null, val outOfBounds: Boolean? = null)

@ApiStatus.Internal
public data class BindingCallErrorInfo(val error: GDExtensionCallErrorType, val argument: Int, val expected: Int)

@ApiStatus.Internal
public inline fun Boolean.toGdBool(): GDExtensionBool = if (this) 1u else 0u

@ApiStatus.Internal
public inline fun GDExtensionBool.toBoolean(): Boolean = this != 0u.toUByte()

@ApiStatus.Internal
public inline fun MemScope.allocGdBool(initialValue: Boolean = false): CArrayPointer<UByteVar> =
    allocArray<UByteVar>(1).also { it[0] = initialValue.toGdBool() }

@ApiStatus.Internal
public inline fun CPointer<UByteVar>.readGdBool(): Boolean = this[0].toBoolean()

@ApiStatus.Internal
public inline fun MemScope.allocCallError(): CArrayPointer<GDExtensionCallError> = allocArray(1)

@ApiStatus.Internal
public inline fun CPointer<GDExtensionCallError>.readCallErrorInfo(): BindingCallErrorInfo =
    pointed.toBindingCallErrorInfo()

@ApiStatus.Internal
public inline fun GDExtensionCallError.toBindingCallErrorInfo(): BindingCallErrorInfo = BindingCallErrorInfo(
    error = error,
    argument = argument,
    expected = expected,
)

@ApiStatus.Internal
public fun MemScope.allocConstTypePtrArray(
    vararg values: GDExtensionConstTypePtr?,
): CArrayPointer<GDExtensionConstTypePtrVar>? = values.takeIf { it.isNotEmpty() }?.let { array ->
    allocArray<GDExtensionConstTypePtrVar>(array.size).also { pointers ->
        array.forEachIndexed { index, value ->
            pointers[index] = value
        }
    }
}

@ApiStatus.Internal
public fun MemScope.allocTypePtrArray(vararg values: GDExtensionTypePtr?): CPointer<GDExtensionTypePtrVar>? =
    values.takeIf { it.isNotEmpty() }?.let { array ->
        allocArray<GDExtensionTypePtrVar>(array.size).also { pointers ->
            array.forEachIndexed { index, value ->
                pointers[index] = value
            }
        }
    }

@ApiStatus.Internal
public fun MemScope.allocConstVariantPtrArray(
    vararg values: GDExtensionConstVariantPtr?,
): CArrayPointer<GDExtensionConstVariantPtrVar>? = values.takeIf { it.isNotEmpty() }?.let { array ->
    allocArray<GDExtensionConstVariantPtrVar>(array.size).also { pointers ->
        array.forEachIndexed { index, value ->
            pointers[index] = value
        }
    }
}
