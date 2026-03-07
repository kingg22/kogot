package io.github.kingg22.godot.runtime

import io.github.kingg22.godot.internal.binding.StringBinding
import io.github.kingg22.godot.internal.binding.VariantBinding
import io.github.kingg22.godot.internal.binding.allocConstTypePtrArray
import io.github.kingg22.godot.internal.ffi.GDExtensionVariantType
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import org.jetbrains.annotations.ApiStatus

// TODO explicit per class, possible remove the object and make all top-level declaration with full names to avoid clash
@ApiStatus.Internal
object BuiltinRuntime {
    fun initializeStringEmpty(destination: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_STRING, destination, 0)
    }

    fun initializeStringCopy(destination: COpaquePointer?, source: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_STRING, destination, 1, source)
    }

    fun initializeStringFromStringName(destination: COpaquePointer?, source: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_STRING, destination, 2, source)
    }

    fun initializeStringFromNodePath(destination: COpaquePointer?, source: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_STRING, destination, 3, source)
    }

    fun initializeStringFromUtf8(destination: COpaquePointer?, value: String) {
        memScoped {
            StringBinding.instance.newWithUtf8CharsRaw(destination, value.cstr.ptr)
        }
    }

    fun destroyString(self: COpaquePointer?) {
        destroyBuiltin(GDEXTENSION_VARIANT_TYPE_STRING, self)
    }

    fun stringToKotlin(self: COpaquePointer?): String = memScoped {
        val length = StringBinding.instance.toUtf8CharsRaw(self, null, 0)
        val buffer = allocArray<ByteVar>(length.toInt() + 1)
        StringBinding.instance.toUtf8CharsRaw(self, buffer, length)
        buffer[length.toInt()] = 0
        buffer.toKString()
    }

    fun initializeStringNameEmpty(destination: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_STRING_NAME, destination, 0)
    }

    fun initializeStringNameCopy(destination: COpaquePointer?, source: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_STRING_NAME, destination, 1, source)
    }

    fun initializeStringNameFromString(destination: COpaquePointer?, source: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_STRING_NAME, destination, 2, source)
    }

    fun initializeStringNameFromUtf8(destination: COpaquePointer?, value: String) {
        memScoped {
            StringBinding.instance.nameNewWithUtf8CharsRaw(destination, value.cstr.ptr)
        }
    }

    fun destroyStringName(self: COpaquePointer?) {
        destroyBuiltin(GDEXTENSION_VARIANT_TYPE_STRING_NAME, self)
    }

    fun initializeNodePathEmpty(destination: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_NODE_PATH, destination, 0)
    }

    fun initializeNodePathCopy(destination: COpaquePointer?, source: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_NODE_PATH, destination, 1, source)
    }

    fun initializeNodePathFromString(destination: COpaquePointer?, source: COpaquePointer?) {
        callBuiltinConstructor(GDEXTENSION_VARIANT_TYPE_NODE_PATH, destination, 2, source)
    }

    fun destroyNodePath(self: COpaquePointer?) {
        destroyBuiltin(GDEXTENSION_VARIANT_TYPE_NODE_PATH, self)
    }

    fun initializeVariantNil(destination: COpaquePointer?) {
        VariantBinding.instance.newNilRaw(destination)
    }

    fun initializeVariantCopy(destination: COpaquePointer?, source: COpaquePointer?) {
        VariantBinding.instance.newCopyRaw(destination, source)
    }

    fun destroyVariant(self: COpaquePointer?) {
        VariantBinding.instance.destroyRaw(self)
    }

    private fun callBuiltinConstructor(
        variantType: GDExtensionVariantType,
        destination: COpaquePointer?,
        constructorIndex: Int,
        vararg args: COpaquePointer?,
    ) {
        val constructor = VariantBinding.instance.getPtrConstructorRaw(variantType, constructorIndex)
            ?: error("Missing builtin constructor $variantType[$constructorIndex]")
        memScoped {
            constructor.invoke(destination, allocConstTypePtrArray(*args))
        }
    }

    private fun destroyBuiltin(variantType: GDExtensionVariantType, self: COpaquePointer?) {
        val destructor = VariantBinding.instance.getPtrDestructorRaw(variantType)
            ?: error("Missing builtin destructor for $variantType")
        destructor.invoke(self)
    }
}
