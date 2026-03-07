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
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.set
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class BindingStatus(val valid: Boolean? = null, val outOfBounds: Boolean? = null)

@ApiStatus.Internal
data class BindingBooleanResult(val value: Boolean, val valid: Boolean? = null, val outOfBounds: Boolean? = null)

@ApiStatus.Internal
data class BindingCallErrorInfo(val error: GDExtensionCallErrorType, val argument: Int, val expected: Int)

@PublishedApi
internal fun Boolean.toGdBool(): GDExtensionBool = if (this) 1u else 0u

@PublishedApi
internal fun GDExtensionBool.toBoolean(): Boolean = this != 0u.toUByte()

@PublishedApi
internal fun MemScope.allocGdBool(initialValue: Boolean = false) = allocArray<UByteVar>(1).also {
    it[0] = initialValue.toGdBool()
}

@PublishedApi
internal fun CPointer<UByteVar>.readGdBool(): Boolean = this[0].toBoolean()

@PublishedApi
internal fun MemScope.allocCallError() = allocArray<GDExtensionCallError>(1)

@PublishedApi
internal fun CPointer<GDExtensionCallError>.readCallErrorInfo(): BindingCallErrorInfo = pointed.toBindingCallErrorInfo()

@PublishedApi
internal fun GDExtensionCallError.toBindingCallErrorInfo(): BindingCallErrorInfo = BindingCallErrorInfo(
    error = error,
    argument = argument,
    expected = expected,
)

@PublishedApi
internal fun MemScope.allocConstTypePtrArray(vararg values: GDExtensionConstTypePtr?) =
    values.takeIf { it.isNotEmpty() }?.let { array ->
        allocArray<GDExtensionConstTypePtrVar>(array.size).also { pointers ->
            array.forEachIndexed { index, value ->
                pointers[index] = value
            }
        }
    }

@PublishedApi
internal fun MemScope.allocTypePtrArray(vararg values: GDExtensionTypePtr?): CPointer<GDExtensionTypePtrVar>? =
    values.takeIf { it.isNotEmpty() }?.let { array ->
        allocArray<GDExtensionTypePtrVar>(array.size).also { pointers ->
            array.forEachIndexed { index, value ->
                pointers[index] = value
            }
        }
    }

@PublishedApi
internal fun MemScope.allocConstVariantPtrArray(vararg values: GDExtensionConstVariantPtr?) =
    values.takeIf { it.isNotEmpty() }?.let { array ->
        allocArray<GDExtensionConstVariantPtrVar>(array.size).also { pointers ->
            array.forEachIndexed { index, value ->
                pointers[index] = value
            }
        }
    }
