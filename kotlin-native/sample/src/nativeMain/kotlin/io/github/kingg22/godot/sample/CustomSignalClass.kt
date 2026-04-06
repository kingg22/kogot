package io.github.kingg22.godot.sample

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.asGodotString
import io.github.kingg22.godot.api.builtin.asStringName
import io.github.kingg22.godot.api.builtin.asVariantString
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.signal.param
import io.github.kingg22.godot.api.signal.signal
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.print
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.ClassDBBinding
import io.github.kingg22.godot.internal.ffi.FALSE
import io.github.kingg22.godot.internal.ffi.GDExtensionBool
import io.github.kingg22.godot.internal.ffi.GDExtensionClassCreationInfo5
import io.github.kingg22.godot.internal.ffi.GDExtensionPropertyInfo
import io.github.kingg22.godot.internal.ffi.GDExtensionVariantType
import io.github.kingg22.godot.internal.ffi.TRUE
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped

class CustomSignalClass(nativePtr: COpaquePointer) : Node(nativePtr) {
    companion object {
        val mySignal = signal("my_signal", param<Int>("value"))
    }

    fun registerSignal() {
        mySignal.register(this)
    }

    fun emitSignal(value: Int) {
        mySignal.emit(value)
    }

    fun testSignal() {
        GD.print("Test signal called!")
        emitSignal(42)
    }
}

fun registerCustomSignalClass() {
    val info = cValue<GDExtensionClassCreationInfo5> {
        is_virtual = GDExtensionBool.FALSE
        is_abstract = GDExtensionBool.FALSE
        is_exposed = GDExtensionBool.TRUE
        set_func = null
        get_func = null
        get_property_list_func = null
        free_property_list_func = null
        property_can_revert_func = null
        property_get_revert_func = null
        validate_property_func = null
        notification_func = notificationFunc
        to_string_func = null
        reference_func = null
        unreference_func = null
        recreate_instance_func = null
        get_virtual_func = getVirtualFunc
        get_virtual_call_data_func = null
        call_virtual_with_data_func = null
        class_userdata = StableRef.create(CustomSignalClass::class).asCPointer()
        create_instance_func = createInstanceFunc
        free_instance_func = freeInstanceFunc
    }

    // ONly requires memScoped to get the ptr of GDExtensionClassCreationInfo5
    memScoped {
        ClassDBBinding.instance.registerExtensionClass5Raw(
            BindingProcAddressHolder.library,
            "CustomSignalClass".asStringName().rawPtr,
            "Node".asStringName().rawPtr,
            info.ptr,
        )
    }

    println("✓ CustomSignalClass registered")
}

fun registerSignalsForCustomSignalClass(
    className: StringName = "CustomSignalClass".asStringName(),
    signalName: StringName = "my_signal".asStringName(),
    paramName: StringName = "value".asStringName(),
    variantType: GDExtensionVariantType = GDEXTENSION_VARIANT_TYPE_INT,
) {
    val argInfo = cValue<GDExtensionPropertyInfo> {
        type = variantType
        name = paramName.rawPtr
        class_name = className.rawPtr
        hint = 0u
        hint_string = "".asGodotString().rawPtr
        usage = 0u
    }

    try {
        memScoped {
            ClassDBBinding.instance.registerExtensionClassSignalRaw(
                BindingProcAddressHolder.library,
                className.rawPtr,
                signalName.rawPtr,
                argInfo.ptr,
                1L,
            )
        }

        GD.print("Signal registered successfully!".asVariantString())
    } catch (e: Throwable) {
        GD.print("Error registering signal: ${e.message}".asVariantString())
        e.printStackTrace()
    }
}
