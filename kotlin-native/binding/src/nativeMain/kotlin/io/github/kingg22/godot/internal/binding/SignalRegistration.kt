package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.asStringName
import io.github.kingg22.godot.api.builtin.internal.toGDE
import io.github.kingg22.godot.api.toEnumMask
import io.github.kingg22.godot.internal.ffi.GDExtensionPropertyInfo
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped

@InternalBinding
public fun registerCustomSignal(className: String, signal: RegisterSignal) {
    className.asStringName().use { className ->
        signal.name.asStringName().use { signalName ->
            memScoped {
                val argumentsInfo = allocArray<GDExtensionPropertyInfo>(signal.params.size) { index ->
                    val param = signal.params[index]
                    this.type = param.type.toGDE()
                    this.name = param.name.asStringName().rawPtr
                    this.class_name = className.rawPtr
                    this.hint = param.hints.toEnumMask().value.toUInt()
                    this.hint_string = param.hintString.asStringName().rawPtr
                    this.usage = param.usages.toEnumMask().value.toUInt()
                }
                ClassDBBinding.instance.registerExtensionClassSignalRaw(
                    BindingProcAddressHolder.library,
                    className.rawPtr,
                    signalName.rawPtr,
                    argumentsInfo,
                    signal.params.size.toLong(),
                )
            }
        }
    }
}
