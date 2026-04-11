// TODO make all internals published api when shared internals are beta/stable
// https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0451-shared-internals.md

@file:Suppress("NOTHING_TO_INLINE")

package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.*
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
public data class BindingStatus(val valid: Boolean? = null, val outOfBounds: Boolean? = null) {
    val isOk: Boolean get() = valid == true && outOfBounds != true
}

@ApiStatus.Internal
public data class BindingBooleanResult(
    val value: Boolean,
    val valid: Boolean? = null,
    val outOfBounds: Boolean? = null,
) {
    val isOk: Boolean get() = valid == true && outOfBounds != true && value
}

@ApiStatus.Internal
public data class BindingCallErrorInfo(val error: GDExtensionCallErrorType, val argument: Int, val expected: Int)

@ApiStatus.Internal
public inline fun Boolean.toGdBool(): GDExtensionBool = if (this) 1u else 0u

@ApiStatus.Internal
public inline fun GDExtensionBool.toBoolean(): Boolean = this != 0u.toUByte()

@ApiStatus.Internal
public inline fun MemScope.allocGdBool(): CPointer<GDExtensionBoolVar> = alloc<GDExtensionBoolVar>().ptr

@ApiStatus.Internal
public inline fun MemScope.allocGdBool(value: Boolean): CPointer<GDExtensionBoolVar> =
    alloc<GDExtensionBoolVar> { this.value = value.toGdBool() }.ptr

@ApiStatus.Internal
public inline fun CPointer<GDExtensionBoolVar>.readGdBool(): Boolean = pointed.value.toBoolean()

@ApiStatus.Internal
public inline fun MemScope.allocCallError(): CPointer<GDExtensionCallError> = alloc<GDExtensionCallError>().ptr

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

/**
 * Throws [IllegalStateException] if the call did not succeed with [GDExtensionCallErrorType.GDEXTENSION_CALL_OK].
 *
 * Designed for call sites that are "guaranteed correct" at codegen time but should
 * still surface any mismatch as a clear, actionable error instead of a silent bad state.
 */
@ApiStatus.Internal
public inline fun checkCallError(context: String, info: BindingCallErrorInfo) {
    check(info.error == GDEXTENSION_CALL_OK) {
        buildString {
            append("[Kogot] GDExtension call failed — $context. ")
            append("error=${info.error}")
            if (info.argument >= 0) append(", argument=${info.argument}")
            if (info.expected >= 0) append(", expected=${info.expected}")
        }
    }
}
